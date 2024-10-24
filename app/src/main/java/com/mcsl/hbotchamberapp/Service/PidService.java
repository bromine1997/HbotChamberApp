package com.mcsl.hbotchamberapp.Service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mcsl.hbotchamberapp.Controller.Pid;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
public class PidService extends Service {
    private static final String TAG = "PIDService";
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;
    private Pid pressPidController;

    private Pid ventPidController;
    private double setPoint;
    private double currentPressure;

    private Intent pressValvePidIntent;
    private Intent ventValvePidIntent;


    private Intent setPointIntent; // Intent 객체 재사용을 위한 멤버 변수

    private Intent elapsedTimeIntent; //경과 시간 업데이트 변수

    private List<String[]> profileData;  // 프로파일 데이터를 저장할 변수
    private long startTime = 0;

    private long elapsedTime = 0;

    private boolean isPaused = false;  // To track if the PID control is paused
    private long pauseStartTime = 0;   // To store when the pause started
    private long totalPausedDuration = 0; // To accumulate the total duration of pauses

    private enum Phase {
        PRESSURE_INCREASE, // 가압 구간
        PRESSURE_HOLD,     // 유지 구간
        PRESSURE_DECREASE  // 감압 구간
    }

    private Phase currentPhase = Phase.PRESSURE_INCREASE;

    private BroadcastReceiver pressureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.mcsl.hbotchamberapp.PRESSURE_UPDATE".equals(intent.getAction())) {
                currentPressure = intent.getDoubleExtra("pressure", 0.0);
                Log.d(TAG, "Received pressure: " + currentPressure);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();


        pressPidController = new Pid(15.0, 10.00, 0.1);                                          //

        ventPidController = new Pid(15.0,10.00,0.1);
        ventPidController.setDirection(true);

        scheduler = Executors.newScheduledThreadPool(1);                                    //pid 제어를 위한 스레드풀 생성

        LocalBroadcastManager.getInstance(this).registerReceiver(pressureReceiver,
                new IntentFilter("com.mcsl.hbotchamberapp.PRESSURE_UPDATE"));                   //Pid 제어할때 사용할 압력 값 수신 Reciver

        // Intent 객체 초기화
        setPointIntent = new Intent("com.mcsl.hbotchamberapp.SETPOINT_UPDATE");                 // Run Activity에서 사용할 Setpoint 보내기 위한 Intent 객체 생성 --> Run Activity에서 수신

        elapsedTimeIntent = new Intent("com.mcsl.hbotchamberapp.ELAPSED_TIME_UPDATE");          //Run Activity에서 사용할 경과 시간 측정후 보내기 위한 Intent 객체 생성 --> Run Activity에서 수신

        pressValvePidIntent = new Intent("com.mcsl.hbotchamberapp.PRESS_VALVE_CONTROL");        //PRESS valve PID boradcast 보내기위한 Intent 객체 생성 --> Valve service에서 수신

        ventValvePidIntent = new Intent("com.mcsl.hbotchamberapp.VENT_VALVE_CONTROL");          //Vent valve PID boradcast 보내기위한 Intent 객체 생성 --> Valve service에서 수신
    }

    private void startPIDControl(List<String[]> profileData) {

        if (startTime == 0) {
            startTime = System.currentTimeMillis(); // Initialize only once
        }

        final long totalProfileTime = calculateTotalProfileTime(profileData); // 전체 프로파일 시간 계산


        scheduler = Executors.newScheduledThreadPool(1);
        scheduledFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            private int currentSection = 0;
            private long sectionStartTime = System.currentTimeMillis();

            @Override
            public void run() {

                if (isPaused) {
                    return; // 일시정지 상태에서는 실행하지 않음
                }

                double output = 0;  // PID OUTPUT

                // 일시정지 시간을 뺀 경과 시간 계산
                elapsedTime = System.currentTimeMillis() - startTime - totalPausedDuration;

                elapsedTimeIntent.putExtra("elapsedTime", elapsedTime);
                LocalBroadcastManager.getInstance(PidService.this).sendBroadcast(elapsedTimeIntent);

                // 전체 프로파일 시간이 정확히 경과했을 때 PID 제어 종료
                if (elapsedTime > totalProfileTime) {
                    stopPIDControl();

                    // RunActivity에게 그래프 업데이트를 중지하라는 신호를 보냄
                    Intent stopGraphIntent = new Intent("com.mcsl.hbotchamberapp.STOP_GRAPH_UPDATE");
                    LocalBroadcastManager.getInstance(PidService.this).sendBroadcast(stopGraphIntent);

                    stopSelf(); // 서비스 종료
                    return;
                }

                if (currentSection < profileData.size()) {
                    String[] section = profileData.get(currentSection);

                    double startPressure = Double.parseDouble(section[1]);
                    double endPressure = Double.parseDouble(section[2]);

                    long duration = (long) (Double.parseDouble(section[3]) * 60 * 1000); // 각 섹션의 time(분)을 밀리초로 변환

                    long sectionElapsedTime = System.currentTimeMillis() - sectionStartTime;

                    if (sectionElapsedTime < duration) {
                        setPoint = startPressure + ((endPressure - startPressure) * (sectionElapsedTime / (double) duration));
                    } else {
                        setPoint = endPressure;
                        currentSection++;
                        sectionStartTime = System.currentTimeMillis();  // 다음 섹션 시작 시간 초기화
                    }

                    // SetPoint 값을 전달하는 브로드캐스트 - Intent 객체 재사용
                    setPointIntent.putExtra("setPoint", setPoint);
                    LocalBroadcastManager.getInstance(PidService.this).sendBroadcast(setPointIntent);

                    // 구간 판단 로직 추가
                    if (currentSection > 0) {
                        String[] previousSection = profileData.get(currentSection - 1);
                        double previousEndPressure = Double.parseDouble(previousSection[2]);

                        if (endPressure > previousEndPressure) {
                            currentPhase = Phase.PRESSURE_INCREASE; // 가압 구간
                        } else if (endPressure < previousEndPressure) {
                            currentPhase = Phase.PRESSURE_DECREASE; // 감압 구간
                        } else {
                            currentPhase = Phase.PRESSURE_HOLD; // 유지 구간
                        }
                    } else {
                        // 첫 섹션은 기본적으로 가압 구간으로 처리
                        currentPhase = Phase.PRESSURE_INCREASE;
                    }

                    // PID 제어 수행
                    if (currentPhase == Phase.PRESSURE_INCREASE || currentPhase == Phase.PRESSURE_HOLD) {
                        output = pressPidController.getOutput(currentPressure, setPoint);
                        controlPressValve(output);
                        Log.d(TAG, "가압중입니다~~~~~~~~~~~~~~~~~~~~~~~ " );
                    } else if (currentPhase == Phase.PRESSURE_DECREASE) {
                        output = ventPidController.getOutput(currentPressure, setPoint);
                        controlVentValve(output);
                        Log.d(TAG, "감압중입니다@@@@@@@@@@@@@@@@@@@@@@@ " );
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }




    private void controlPressValve(double output) {
        //Press Valve PID Broadcast
        pressValvePidIntent.putExtra("valveOutput", output);
        LocalBroadcastManager.getInstance(this).sendBroadcast(pressValvePidIntent);
    }

    private void controlVentValve(double output) {
        //Vent Valve PID Broadcast
        ventValvePidIntent.putExtra("valveOutput", output);
        LocalBroadcastManager.getInstance(this).sendBroadcast(ventValvePidIntent);
    }


    // PID 제어를 일시 정지하는 메소드
    private void pausePIDControl() {
        isPaused = true;
        pauseStartTime = System.currentTimeMillis();  // 일시 정지 시작 시간 기록
    }

    // PID 제어를 다시 시작하는 메소드
    private void resumePIDControl() {
        if (isPaused) {
            totalPausedDuration += System.currentTimeMillis() - pauseStartTime;
            isPaused = false;
        }
    }




    // PID 제어를 완전히 중지하는 메소드
    private void stopPIDControl() {
        if (scheduler != null) {
            scheduler.shutdown();
        }

        startTime = 0;
        totalPausedDuration = 0;
        isPaused = false;

        // RunActivity에게 그래프 업데이트를 중지하라는 신호를 보냄
        Intent stopGraphIntent = new Intent("com.mcsl.hbotchamberapp.STOP_GRAPH_UPDATE");
        LocalBroadcastManager.getInstance(this).sendBroadcast(stopGraphIntent);

        // 모든 벨브를 멈추는 브로드캐스트 전송
        Intent stopAllValvesIntent = new Intent("com.mcsl.hbotchamberapp.STOP_ALL_VALVES");
        LocalBroadcastManager.getInstance(this).sendBroadcast(stopAllValvesIntent);
    }

    private void stopAllValves() {
        // 벨브를 모두 끄는 로직
        controlPressValve(0);  // 압력 벨브를 닫습니다.
        controlVentValve(0);   // 배기 벨브를 닫습니다.
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "com.mcsl.hbotchamberapp.action.START_PID":
                        profileData = loadProfileData();  // 데이터를 멤버 변수에 저장
                        if (profileData != null && !profileData.isEmpty()) {
                            startPIDControl(profileData);
                        }
                        break;
                    case "com.mcsl.hbotchamberapp.action.PAUSE_PID":
                        pausePIDControl();
                        break;
                    case "com.mcsl.hbotchamberapp.action.RESUME_PID":
                        resumePIDControl();
                        break;
                    case "com.mcsl.hbotchamberapp.action.STOP_PID":
                        stopPIDControl();
                        stopSelf();
                        break;
                }
            }
        }
        return START_STICKY;
    }



    private List<String[]> loadProfileData() {
        List<String[]> profileData = new ArrayList<>();
        try (FileInputStream fis = openFileInput("profile_data.json");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            Gson gson = new Gson();
            Type type = new TypeToken<List<String[]>>() {}.getType();
            profileData = gson.fromJson(sb.toString(), type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return profileData;
    }

    private long calculateTotalProfileTime(List<String[]> profileData) {
        long totalTime = 0;
        for (String[] section : profileData) {
            long duration = (long) (Double.parseDouble(section[3]) * 60 * 1000); // 분을 밀리초로 변환
            totalTime += duration;
        }
        return totalTime;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPIDControl();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pressureReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}