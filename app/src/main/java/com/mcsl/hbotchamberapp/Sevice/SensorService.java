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

import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensorService extends Service {
    private static final String TAG = "SensorService";
    private Handler handler;
    private Runnable adcRunnable;
    private Runnable co2Runnable;

    private static final String ACTION_REQUEST_ADC_VALUES = "com.mcsl.hbotchamberapp.action.REQUEST_ADC_VALUES";
    private static final String ACTION_CO2_UPDATE = "com.mcsl.hbotchamberapp.action.CO2_UPDATE";

    private Max1032 multiSensor;
    private Co2Sensor co2sensor;

    private Intent adcIntent;
    private Intent pressureIntent;
    private Intent co2Intent;

    @Override
    public void onCreate() {
        super.onCreate();

        multiSensor = new Max1032(1, 18); // SPI 1 bus => SPI5, LatchPin 설정
        //multiSensor.ConfigAllChannels();

        co2sensor = new Co2Sensor(); // baud rate 9600
        co2sensor.init();

        adcIntent = new Intent("com.mcsl.hbotchamberapp.ADC_VALUES");
        pressureIntent = new Intent("com.mcsl.hbotchamberapp.PRESSURE_UPDATE");
        co2Intent = new Intent("com.mcsl.hbotchamberapp.CO2_UPDATE");

        HandlerThread handlerThread = new HandlerThread("MyServiceBackgroundThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        adcRunnable = new Runnable() {
            @Override
            public void run() {
                readAndBroadcastAdcValues();
                Log.d(TAG, "run: adc");
                handler.postDelayed(this, 100); // 1초마다 실행
            }
        };

        co2Runnable = new Runnable() {
            @Override
            public void run() {
                readAndBroadcastCo2Values();
                Log.d(TAG, "run: co2");
                handler.postDelayed(this, 100); // 2초마다 실행
            }
        };
        Log.d(TAG, "TTTTTTTTTTTTTTTTTTTes");
        handler.postDelayed(adcRunnable, 1000);
        handler.postDelayed(co2Runnable, 1000);
    }

    private void readAndBroadcastAdcValues() {
        int[] adcValues = multiSensor.ReadAllChannels(); // 0번 습도, 1번 온도 , 2번 : 유량 3번:압력 , 4번: 산소, 5번 : 연결안됨
        adcIntent.putExtra("adcValues", adcValues);

        // 압력 값만 별도로 브로드캐스트
        pressureIntent.putExtra("pressure", adcValues[3]);

        LocalBroadcastManager.getInstance(this).sendBroadcast(adcIntent);
        LocalBroadcastManager.getInstance(this).sendBroadcast(pressureIntent);
    }

    private void readAndBroadcastCo2Values() {
        try {
            Future<String> futureCo2Data = co2sensor.loopbackCommand("Q\r\n");
            String co2Data = futureCo2Data.get(); // 결과를 기다림
            int co2Ppm = parseCo2Value(co2Data);

            co2Intent.putExtra("co2Ppm", co2Ppm);
            LocalBroadcastManager.getInstance(this).sendBroadcast(co2Intent);
        } catch (Exception e) {
            Log.e(TAG, "CO2 데이터 읽기 오류", e);
        }
    }

    private int parseCo2Value(String co2Str) {
        // 숫자 부분만 추출하여 정수로 변환하는 로직 구현
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(co2Str);
        if (matcher.find()) {
            try {
                int rawValue = Integer.parseInt(matcher.group());
                int scalingFactor = 100; // 예제 스케일링 팩터 값
                return rawValue * scalingFactor;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return -1; // 변환 실패 시 기본값 반환
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
