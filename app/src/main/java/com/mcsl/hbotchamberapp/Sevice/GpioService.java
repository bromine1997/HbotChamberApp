package com.mcsl.hbotchamberapp.Sevice;



import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mcsl.hbotchamberapp.Controller.PinController;


// GPIO 출력 및 입력을 담당하는 서비스
public class GpioService extends Service {
    private static final String TAG = "GpioService";
    private Handler handler;
    private Runnable i2cRunnable;

    private static final String ACTION_TOGGLE_LED1 = "com.mcsl.hbotchamberapp.action.TOGGLE_LED1";
    private static final String ACTION_TOGGLE_LED2 = "com.mcsl.hbotchamberapp.action.TOGGLE_LED2";
    private static final String ACTION_TOGGLE_LED3 = "com.mcsl.hbotchamberapp.action.TOGGLE_LED3";

    private PinController pinController;

    @Override
    public void onCreate() {
        super.onCreate();
        pinController = new PinController();

        HandlerThread handlerThread = new HandlerThread("GpioServiceBackgroundThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        i2cRunnable = new Runnable() {
            @Override
            public void run() {
                readAndBroadcastI2cValues();
               // Log.d(TAG, "GPIO I2C values read and broadcasted");
                handler.postDelayed(this, 100); // 0.5초마다 실행
            }
        };

        // 처음 실행
        handler.postDelayed(i2cRunnable, 100);
       // Log.d(TAG, "GpioService started");
    }

    private void readAndBroadcastI2cValues() {
        byte inputStatus = pinController.readInputs(); // 외부 입력 스위치 주기적으로 확인
        Intent intent = new Intent("com.mcsl.hbotchamberapp.IO_STATUS_UPDATE");
        intent.putExtra("inputStatus", inputStatus);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "GpioService가 시작되었습니다.");

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_TOGGLE_LED1:
                        pinController.toggleLed(1);
                        sendBroadcastUpdate("LED1");
                        break;
                    case ACTION_TOGGLE_LED2:
                        pinController.toggleLed(2);
                        sendBroadcastUpdate("LED2");
                        break;
                    case ACTION_TOGGLE_LED3:
                        pinController.toggleLed(3);
                        sendBroadcastUpdate("LED3");
                        break;
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "GpioService가 종료되었습니다.");
        handler.removeCallbacks(i2cRunnable);
        handler.getLooper().quit(); // HandlerThread 종료
    }

    private void sendBroadcastUpdate(String status) {
        Intent intent = new Intent("com.mcsl.hbotchamberapp.IO_STATUS_UPDATE");
        intent.putExtra("status", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
