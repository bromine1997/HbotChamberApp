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
    private Runnable broadcastRunnable;

    private static final String ACTION_REQUEST_ADC_VALUES = "com.mcsl.hbotchamberapp.action.REQUEST_ADC_VALUES";
    private static final String ACTION_CO2_UPDATE = "com.mcsl.hbotchamberapp.action.CO2_UPDATE";

    private Max1032 multiSensor;
    private Co2Sensor co2sensor;

    private Intent adcIntent;
    private Intent Co2Intent;
    private Intent PressureIntent;
    private Intent TempIntent;
    private Intent HumidityIntent;
    private Intent O2Intent;
    private Intent FlowIntent;

    private double temperature, humidity, flowRate, pressure,oxygen;


    @Override
    public void onCreate() {
        super.onCreate();

        multiSensor = new Max1032(1, 18);
        multiSensor.ConfigAllChannels();

        co2sensor = new Co2Sensor();
        co2sensor.init();

        TempIntent = new Intent("com.mcsl.hbotchamberapp.Temp_UPDATE");
        HumidityIntent = new Intent("com.mcsl.hbotchamberapp.Humidity_UPDATE");
        O2Intent = new Intent("com.mcsl.hbotchamberapp.O2_UPDATE");
        PressureIntent = new Intent("com.mcsl.hbotchamberapp.PRESSURE_UPDATE");
        FlowIntent = new Intent("com.mcsl.hbotchamberapp.Flow_UPDATE");
        Co2Intent = new Intent("com.mcsl.hbotchamberapp.CO2_UPDATE");

        HandlerThread handlerThread = new HandlerThread("MyServiceBackgroundThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        adcRunnable = new Runnable() {
            @Override
            public void run() {
                // 자주 센서 데이터를 읽어온다
                readAdcValues();
                handler.postDelayed(this, 100); // 0.1초마다 실행
            }
        };

        broadcastRunnable = new Runnable() {
            @Override
            public void run() {
                // 주기적으로 브로드캐스트를 수행한다
                broadcastSensorValues();
                handler.postDelayed(this, 1000); // 1초마다 실행
            }
        };

        co2Runnable = new Runnable() {
            @Override
            public void run() {
                readAndBroadcastCo2Values();
                handler.postDelayed(this, 1000); // 1초마다 실행
            }
        };

        handler.post(adcRunnable);
        handler.post(broadcastRunnable);
        handler.post(co2Runnable);
    }

    private void readAdcValues() {
        int[] adcValues = multiSensor.ReadAllChannels();
        temperature = calibrateTempeValue(adcValues[1]);
        humidity = calibrateHumidityValue(adcValues[0]);
        flowRate = calibrateFlowValue(adcValues[2]);
        pressure = calibratePressureValue(adcValues[3]);
        oxygen = calibrateOxygenValue(adcValues[4]);
    }

    private void broadcastSensorValues() {
        TempIntent.putExtra("temperature", temperature);
        LocalBroadcastManager.getInstance(this).sendBroadcast(TempIntent);

        HumidityIntent.putExtra("humidity", humidity);
        LocalBroadcastManager.getInstance(this).sendBroadcast(HumidityIntent);

        FlowIntent.putExtra("flowRate", flowRate);
        LocalBroadcastManager.getInstance(this).sendBroadcast(FlowIntent);

        PressureIntent.putExtra("pressure", pressure);
        LocalBroadcastManager.getInstance(this).sendBroadcast(PressureIntent);

        O2Intent.putExtra("oxygen", oxygen);
        LocalBroadcastManager.getInstance(this).sendBroadcast(O2Intent);


    }

    private void readAndBroadcastCo2Values() {
        try {
            Future<String> futureCo2Data = co2sensor.loopbackCommand("Q\r\n");
            String co2Data = futureCo2Data.get();
            int co2Ppm = parseCo2Value(co2Data);

            Co2Intent.putExtra("co2Ppm", co2Ppm);
            LocalBroadcastManager.getInstance(this).sendBroadcast(Co2Intent);
        } catch (Exception e) {
            Log.e(TAG, "CO2 데이터 읽기 오류", e);
        }
    }



    private double calibrateTempeValue(int rawTempValue) {
        int ADC_min = 2675;
        int ADC_max = 13305;
        double currentInMilliAmps = 4.0 + ((double)(rawTempValue - ADC_min) / (ADC_max - ADC_min)) * (20.0 - 4.0);
        double temperature = ((currentInMilliAmps - 4.0) / 0.1524) - 30.0;
        return Math.round(temperature * 100.0) / 100.0;
    }

    private double calibrateHumidityValue(int rawHumidityValue) {
        int ADC_min = 2675;
        int ADC_max = 13305;
        double currentInMilliAmps = 4.0 + ((double)(rawHumidityValue - ADC_min) / (ADC_max - ADC_min)) * (20.0 - 4.0);
        double humidity = (currentInMilliAmps - 4.0) / 0.16;
        return Math.round(humidity * 100.0) / 100.0;
    }

    private double calibrateFlowValue(int rawFlowValue) {
        int ADC_min = 2675;
        int ADC_max = 13305;
        double currentInMilliAmps = 4.0 + ((double)(rawFlowValue - ADC_min) / (ADC_max - ADC_min)) * (20.0 - 4.0);


        double flowRate = (currentInMilliAmps - 4.0) * (100.0 / 16.0);
        return Math.round(flowRate * 100.0) / 100.0;
    }

    private double calibratePressureValue(int rawPressureValue) {
        int ADC_min = 2675;
        int ADC_max = 13305;
        double pressure = 1.0 + ((double)(rawPressureValue - ADC_min) / (ADC_max - ADC_min)) * (6 - 1);
        return Math.round(pressure * 100.0) / 100.0;
    }

    private double calibrateOxygenValue(int rawOxygenValue) {
        int a0 = 46;
        int a1 = 10602;

        double O2Value = ((double)(rawOxygenValue - a0)) * 2090.0 / (double) (a1 - a0);
        return Math.round(O2Value) / 100.0;


    }


    private int parseCo2Value(String co2Str) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(co2Str);
        if (matcher.find()) {
            try {
                int rawValue = Integer.parseInt(matcher.group());
                int scalingFactor = 100;
                return rawValue * scalingFactor;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Sensor 서비스가 시작되었습니다.");

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_REQUEST_ADC_VALUES:
                        broadcastSensorValues();
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
        handler.removeCallbacks(broadcastRunnable);
        handler.removeCallbacks(co2Runnable);
        handler.getLooper().quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
