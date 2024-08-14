package com.mcsl.hbotchamberapp.Sevice;

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
    private Pid pidController;
    private double setPoint;
    private double currentPressure;

    private List<String[]> profileData;  // 프로파일 데이터를 저장할 변수

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
        pidController = new Pid(15.0, 10.00, 0.1);
        scheduler = Executors.newScheduledThreadPool(1);

        LocalBroadcastManager.getInstance(this).registerReceiver(pressureReceiver,
                new IntentFilter("com.mcsl.hbotchamberapp.PRESSURE_UPDATE"));
    }

    private void startPIDControl(List<String[]> profileData) {
        final long totalProfileTime = calculateTotalProfileTime(profileData); // 전체 프로파일 시간 계산
        final long startTime = System.currentTimeMillis(); // PID 시작 시간

        scheduler = Executors.newScheduledThreadPool(1);
        scheduledFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            private int currentSection = 0;
            private long sectionStartTime = System.currentTimeMillis();

            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime;

                // 전체 프로파일 시간이 경과하면 PID 제어 종료
                if (elapsedTime >= totalProfileTime) {
                    stopPIDControl();
                    stopGraphUpdate(); // 그래프 업데이트 중지
                    return;
                }

                if (currentSection < profileData.size()) {
                    String[] section = profileData.get(currentSection);
                    double startPressure = Double.parseDouble(section[1]);
                    double endPressure = Double.parseDouble(section[2]);
                    long duration = (long) (Double.parseDouble(section[3]) * 60 * 1000); // 분을 밀리초로 변환

                    long sectionElapsedTime = System.currentTimeMillis() - sectionStartTime;
                    if (sectionElapsedTime < duration) {
                        setPoint = startPressure + ((endPressure - startPressure) * (sectionElapsedTime / (double) duration));
                    } else {
                        setPoint = endPressure;
                        currentSection++;
                        sectionStartTime = System.currentTimeMillis();  // 다음 섹션 시작 시간 초기화
                    }

                    // PID 제어 수행
                    double output = pidController.getOutput(currentPressure, setPoint);
                    if (currentPhase == Phase.PRESSURE_INCREASE || currentPhase == Phase.PRESSURE_HOLD) {
                        controlPressValve(output);
                    } else if (currentPhase == Phase.PRESSURE_DECREASE) {
                        controlVentValve(output);
                    }

                    sendPidOutput(output);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }



    private void controlPressValve(double output) {
        // Press Valve 제어 로직
        Intent intent = new Intent("com.mcsl.hbotchamberapp.PRESS_VALVE_CONTROL");
        intent.putExtra("valveOutput", output);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void controlVentValve(double output) {
        // Vent Valve 제어 로직
        Intent intent = new Intent("com.mcsl.hbotchamberapp.VENT_VALVE_CONTROL");
        intent.putExtra("valveOutput", output);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendPidOutput(double output) {
        Intent intent = new Intent("com.mcsl.hbotchamberapp.PID_OUTPUT_UPDATE");
        intent.putExtra("pidOutput", output);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // PID 제어를 일시 정지하는 메소드
    private void pausePIDControl() {
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
        }
    }

    // PID 제어를 다시 시작하는 메소드
    private void resumePIDControl() {
        if (scheduledFuture == null || scheduledFuture.isCancelled()) {
            if (profileData != null && !profileData.isEmpty()) {
                startPIDControl(profileData);
            }
        }
    }


    // PID 제어를 완전히 중지하는 메소드
    private void stopPIDControl() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void stopGraphUpdate() {
        Intent intent = new Intent("com.mcsl.hbotchamberapp.STOP_GRAPH_UPDATE");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
