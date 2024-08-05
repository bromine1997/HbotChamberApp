package com.mcsl.hbotchamberapp.Sevice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mcsl.hbotchamberapp.Controller.Pid;

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



    // broadcastReceiver를 통해 특정 인텐드를 수신하여 압력값을 추출해서 PIDservice에 사용하는 Pressure 변수에 저장
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
        pidController = new Pid(1.0, 0.1, 0.01);
        scheduler = Executors.newScheduledThreadPool(1);

        LocalBroadcastManager.getInstance(this).registerReceiver(pressureReceiver,
                new IntentFilter("com.mcsl.hbotchamberapp.PRESSURE_UPDATE"));
    }

    private void startPIDControl() {
        scheduledFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // PID 계산을 수행한다.
                double output = pidController.getOutput(currentPressure, setPoint);
                // PID 출력 값을 전송한다.
                sendPidOutput(output);
            }
        }, 0, 1, TimeUnit.SECONDS); // 1초마다 이 작업을 반복한다.
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
            startPIDControl();
        }
    }

    // PID 제어를 완전히 중지하는 메소드
    private void stopPIDControl() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "com.mcsl.hbotchamberapp.action.START_PID":
                        startPIDControl();
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