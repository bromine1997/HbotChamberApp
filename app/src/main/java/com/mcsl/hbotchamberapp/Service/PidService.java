package com.mcsl.hbotchamberapp.Service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

import androidx.lifecycle.Observer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mcsl.hbotchamberapp.Controller.Pid;
import com.mcsl.hbotchamberapp.model.SensorData;
import com.mcsl.hbotchamberapp.repository.PIDRepository;
import com.mcsl.hbotchamberapp.repository.SensorRepository;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.TimeUnit;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class PidService extends Service {
    private static final String TAG = "PIDService";
    private ScheduledExecutorService scheduler;
    private Pid pressPidController;
    private Pid ventPidController;
    private double setPoint;
    private double currentPressure;

    private PIDRepository pidRepository;
    private SensorRepository sensorRepository;

    private ValveService valveService;
    private boolean isValveServiceBound = false;

    private List<String[]> profileData;
    private long startTime = 0;
    private long elapsedTime = 0;
    private boolean isPaused = false;
    private long pauseStartTime = 0;
    private long totalPausedDuration = 0;

    private enum Phase {
        PRESSURE_INCREASE,
        PRESSURE_HOLD,
        PRESSURE_DECREASE
    }

    private Phase currentPhase = Phase.PRESSURE_INCREASE;

    private ServiceConnection valveServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ValveService.LocalBinder binder = (ValveService.LocalBinder) service;
            valveService = binder.getService();
            isValveServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isValveServiceBound = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        pidRepository = PIDRepository.getInstance(this);
        sensorRepository = SensorRepository.getInstance(this);
        sensorRepository.getSensorData().observeForever(sensorDataObserver);

        pressPidController = new Pid(15.0, 10.00, 0.1);
        ventPidController = new Pid(15.0,10.00,0.1);
        ventPidController.setDirection(true);



        // ValveService 바인딩
        Intent intent = new Intent(this, ValveService.class);
        bindService(intent, valveServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private Observer<SensorData> sensorDataObserver = new Observer<SensorData>() {
        @Override
        public void onChanged(SensorData data) {
            currentPressure = data.getPressure();
        }
    };

    private void startPIDControl(List<String[]> profileData) {

        String sessionId = UUID.randomUUID().toString();
        pidRepository.setSessionId(sessionId);


        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }

        sendPidControlStatusBroadcast("PID_CONTROL_STARTED");

        final long totalProfileTime = calculateTotalProfileTime(profileData);
        Log.d(TAG, "총 프로파일 시간: " + totalProfileTime + "ms");

        // 스케줄러를 초기화
        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(new Runnable() {
            private int currentSection = 0;
            private long sectionStartTime = System.currentTimeMillis();

            @Override
            public void run() {
                if (isPaused) {
                    return;
                }

                double output = 0;

                elapsedTime = System.currentTimeMillis() - startTime - totalPausedDuration;
                pidRepository.setElapsedTime(elapsedTime);

                if (elapsedTime > totalProfileTime) {
                    Log.d(TAG, "치료 끝. PID 제어를 종료합니다.");
                    stopPIDControl();
                    stopSelf();
                    return;
                }

                if (currentSection < profileData.size()) {
                    String[] section = profileData.get(currentSection);

                    double startPressure = Double.parseDouble(section[1]);
                    double endPressure = Double.parseDouble(section[2]);

                    long duration = (long) (Double.parseDouble(section[3]) * 60 * 1000);

                    long sectionElapsedTime = System.currentTimeMillis() - sectionStartTime;

                    if (sectionElapsedTime < duration) {
                        setPoint = startPressure + ((endPressure - startPressure) * (sectionElapsedTime / (double) duration));
                    } else {
                        setPoint = endPressure;
                        currentSection++;
                        sectionStartTime = System.currentTimeMillis();
                    }

                    pidRepository.setSetPoint(setPoint);

                    if (currentSection > 0) {
                        String[] previousSection = profileData.get(currentSection - 1);
                        double previousEndPressure = Double.parseDouble(previousSection[2]);

                        if (endPressure > previousEndPressure) {
                            currentPhase = Phase.PRESSURE_INCREASE;
                        } else if (endPressure < previousEndPressure) {
                            currentPhase = Phase.PRESSURE_DECREASE;
                        } else {
                            currentPhase = Phase.PRESSURE_HOLD;
                        }
                    } else {
                        currentPhase = Phase.PRESSURE_INCREASE;
                    }

                    pidRepository.setPidPhase(currentPhase.toString());

                    // PID 제어 로직
                    if (currentPhase == Phase.PRESSURE_INCREASE || currentPhase == Phase.PRESSURE_HOLD) {
                        output = pressPidController.getOutput(currentPressure, setPoint);
                        controlPressValve(output);
                        Log.d(TAG, "가압 중");
                    } else if (currentPhase == Phase.PRESSURE_DECREASE) {
                        output = ventPidController.getOutput(currentPressure, setPoint);
                        controlVentValve(output);
                        Log.d(TAG, "감압 중");
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void controlPressValve(double output) {
        if (isValveServiceBound) {
            valveService.PidControlPressProportionValve(output);
        }
    }

    private void controlVentValve(double output) {
        if (isValveServiceBound) {
            valveService.PidControlVentProportionValve(output);
        }
    }

    private void stopPIDControl() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;         // 스케줄러를 null로 설정합니다.
        }

        startTime = 0;
        totalPausedDuration = 0;
        isPaused = false;

        pidRepository.setSessionId(null);

        sendPidControlStatusBroadcast("PID_CONTROL_STOPPED");

        if (isValveServiceBound) {
            valveService.stopAllValves();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "com.mcsl.hbotchamberapp.action.START_PID":
                        profileData = loadProfileData();
                        if (profileData != null && !profileData.isEmpty()) {
                            startPIDControl(profileData);
                        }
                        break;
                    case "com.mcsl.hbotchamberapp.action.PAUSE_PID":
                        isPaused = true;
                        pauseStartTime = System.currentTimeMillis();
                        break;
                    case "com.mcsl.hbotchamberapp.action.RESUME_PID":
                        if (isPaused) {
                            totalPausedDuration += System.currentTimeMillis() - pauseStartTime;
                            isPaused = false;
                        }
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
            long duration = (long) (Double.parseDouble(section[3]) * 60 * 1000);
            totalTime += duration;
        }
        return totalTime;
    }

    // 브로드캐스트를 전송하는 메서드 추가
    private void sendPidControlStatusBroadcast(String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPIDControl();
        sensorRepository.getSensorData().removeObserver(sensorDataObserver);
        if (isValveServiceBound) {
            unbindService(valveServiceConnection);
            isValveServiceBound = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
