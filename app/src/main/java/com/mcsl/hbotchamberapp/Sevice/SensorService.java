package com.mcsl.hbotchamberapp.Sevice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mcsl.hbotchamberapp.Controller.Co2Sensor;
import com.mcsl.hbotchamberapp.Controller.Max1032;

public class SensorService extends Service {
    private static final String TAG = "SensorService";
    private Handler handler;
    private Runnable adcRunnable;
    private Runnable co2Runnable;


    private static final String ACTION_REQUEST_ADC_VALUES = "com.mcsl.hbotchamberapp.action.REQUEST_ADC_VALUES";
    private static final String ACTION_CO2_UPDATE = "com.mcsl.hbotchamberapp.action.CO2_UPDATE";


    private Max1032 max1032;
    private Co2Sensor co2sensor;



    @Override
    public void onCreate() {
        super.onCreate();

        max1032 = new Max1032(1, 18); // SPI 1 bus => SPI5, LatchPin 설정
        co2sensor = new Co2Sensor();                //baud rate 9600,

        HandlerThread handlerThread = new HandlerThread("MyServiceBackgroundThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());


        co2sensor.init();

        adcRunnable = new Runnable() {
            @Override
            public void run() {
                readAndBroadcastAdcValues();
                Log.d(TAG, "run: adc 1");
                handler.postDelayed(this, 100); // 1초마다 실행
            }
        };

        co2Runnable = new Runnable() {
            @Override
            public void run() {
                readAndBroadcastCo2Values();
                Log.d(TAG, "run: adc 2");
                handler.postDelayed(this, 100); // 2초마다 실행
            }
        };

        // 처음 실행 (1초 후에 첫 실행)
        handler.postDelayed(adcRunnable, 100);
        handler.postDelayed(co2Runnable, 100);
    }

    private void readAndBroadcastAdcValues() {
        int[] adcValues = max1032.readAllChannels();
        Intent intent = new Intent("com.example.test.ADC_VALUES");
        intent.putExtra("adcValues", adcValues);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }




    private void controlValves() {
        Log.d(TAG, "Valves PID Start ");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Valves PID processing");
            }
        }, 1000);
    }

    private void readAndBroadcastCo2Values() {

        co2sensor.loopbackCommand("Q\r\n"); // 문자열 데이터를 정수로 바꾸고 ppm으로 표시하는 함수 추가해야함..ing (미완성)

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Sensor 서비스가 시작되었습니다.");

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_REQUEST_ADC_VALUES:
                        readAndBroadcastAdcValues();
                        break;
                    case ACTION_CO2_UPDATE:
                        readAndBroadcastCo2Values();
                        break;
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Sensor 서비스가 종료되었습니다.");
        handler.removeCallbacks(adcRunnable);
        handler.removeCallbacks(co2Runnable);
        handler.getLooper().quit();  // HandlerThread 종료
    }

    private void sendBroadcastUpdate(String status) {
        Intent intent = new Intent("com.example.test.IO_STATUS_UPDATE");
        intent.putExtra("status", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}