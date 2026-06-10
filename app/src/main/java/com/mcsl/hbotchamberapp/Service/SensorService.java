package com.mcsl.hbotchamberapp.Service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mcsl.hbotchamberapp.Controller.Co2Sensor;
import com.mcsl.hbotchamberapp.Controller.Max1032;
import com.mcsl.hbotchamberapp.model.SensorData;
import com.mcsl.hbotchamberapp.repository.SensorRepository;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensorService extends Service {
    private static final String TAG = "SensorService";
    private Handler handler;
    private Runnable sensorReadRunnable;

    private Max1032 multiSensor;
    private Co2Sensor co2sensor;

    private Intent sensorDataIntent;

    private double temperature, humidity, flowRate, pressure, oxygen;
    private int co2Ppm;

    private int consecutiveSensorErrors = 0;
    private static final int MAX_CONSECUTIVE_ERRORS = 3;

    private final IBinder binder = new LocalBinder();
    private final MutableLiveData<SensorData> sensorDataLiveData = new MutableLiveData<>();
    private SensorRepository sensorRepository;

    public class LocalBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }


    public LiveData<SensorData> getSensorData() {
        return sensorDataLiveData;
    }

    public void setRepository(SensorRepository repository) {
        this.sensorRepository = repository;
    }



    @Override
    public void onCreate() {
        super.onCreate();

        try {
            multiSensor = new Max1032(1, 18);
            multiSensor.ConfigAllChannels();
        } catch (Exception e) {
            Log.e(TAG, "SPI 센서 초기화 실패", e);
            multiSensor = null;
        }

        try {
            co2sensor = new Co2Sensor();
            co2sensor.init();
        } catch (Exception e) {
            Log.e(TAG, "CO2 센서 초기화 실패", e);
            co2sensor = null;
        }

        // 브로드캐스트를 위한 Intent
        sensorDataIntent = new Intent("com.mcsl.hbotchamberapp.SENSOR_UPDATE");

        // 백그라운드 작업을 위한 HandlerThread 생성
        HandlerThread handlerThread = new HandlerThread("SensorServiceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        // 센서 값을 읽고 브로드캐스트하는 Runnable 정의
        sensorReadRunnable = new Runnable() {
            @Override
            public void run() {
                readAllSensorValues();
                handler.postDelayed(this, 1000); // 1초마다 실행
            }
        };

        // 첫 실행
        handler.post(sensorReadRunnable);
    }



    // 모든 센서 값을 읽고, Intent에 담아 브로드캐스트
    private void readAllSensorValues() {
        try {
            if (multiSensor == null) {
                throw new Exception("SPI 센서 미연결");
            }
            readAdcValues();

            if (co2sensor != null) {
                Future<String> futureCo2Data = co2sensor.loopbackCommand("Q\r\n");
                String co2Data = futureCo2Data.get(3, TimeUnit.SECONDS);
                co2Ppm = parseCo2Value(co2Data);
            }

            sensorDataIntent.putExtra("temperature", temperature);
            sensorDataIntent.putExtra("humidity", humidity);
            sensorDataIntent.putExtra("flowRate", flowRate);
            sensorDataIntent.putExtra("pressure", pressure);
            sensorDataIntent.putExtra("oxygen", oxygen);
            sensorDataIntent.putExtra("co2Ppm", co2Ppm);

            SensorData data = new SensorData(pressure, temperature, humidity, oxygen, co2Ppm, flowRate);
            if (sensorRepository != null) {
                sensorRepository.setSensorData(data);
            } else {
                sensorDataLiveData.postValue(data);
            }

            LocalBroadcastManager.getInstance(this).sendBroadcast(sensorDataIntent);

            consecutiveSensorErrors = 0;

        } catch (Exception e) {
            Log.e(TAG, "센서 데이터 읽기 오류", e);
            consecutiveSensorErrors++;
            // 이상값(-999)은 PID에 전달하지 않음 — PidService의 stale 감지가 처리
            if (consecutiveSensorErrors >= MAX_CONSECUTIVE_ERRORS) {
                sendSensorErrorBroadcast();
            }
        }
    }

    private void sendSensorErrorBroadcast() {
        Intent intent = new Intent("SENSOR_CONNECTION_ERROR");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // SPI 센서 값 읽기
    private void readAdcValues() {
        int[] adcValues = multiSensor.ReadAllChannels();
        temperature = calibrateTempeValue(adcValues[1]);
        humidity = calibrateHumidityValue(adcValues[0]);
        flowRate = calibrateFlowValue(adcValues[2]);
        pressure = calibratePressureValue(adcValues[3]);
        oxygen = calibrateOxygenValue(adcValues[4]);
    }

    // CO2 값 파싱
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

    // 보정 함수들 (온도, 습도, 유량, 압력, 산소)
    private double calibrateTempeValue(int rawTempValue) {
        int ADC_min = 2675;
        int ADC_max = 13305;
        int clamped = Math.max(ADC_min, Math.min(ADC_max, rawTempValue));
        double currentInMilliAmps = 4.0 + ((double) (clamped - ADC_min) / (ADC_max - ADC_min)) * (20.0 - 4.0);
        double temperature = ((currentInMilliAmps - 4.0) / 0.1524) - 30.0;
        return Math.round(temperature * 100.0) / 100.0;
    }

    private double calibrateHumidityValue(int rawHumidityValue) {
        int ADC_min = 2675;
        int ADC_max = 13305;
        int clamped = Math.max(ADC_min, Math.min(ADC_max, rawHumidityValue));
        double currentInMilliAmps = 4.0 + ((double) (clamped - ADC_min) / (ADC_max - ADC_min)) * (20.0 - 4.0);
        double humidity = (currentInMilliAmps - 4.0) / 0.16;
        return Math.round(humidity * 100.0) / 100.0;
    }

    private double calibrateFlowValue(int rawFlowValue) {
        int ADC_min = 2675;
        int ADC_max = 13305;
        int clamped = Math.max(ADC_min, Math.min(ADC_max, rawFlowValue));
        double currentInMilliAmps = 4.0 + ((double) (clamped - ADC_min) / (ADC_max - ADC_min)) * (20.0 - 4.0);
        double flowRate = (currentInMilliAmps - 4.0) * (100.0 / 16.0);
        return Math.round(flowRate * 100.0) / 100.0;
    }

    private double calibratePressureValue(int rawPressureValue) {
        int ADC_min = 2675;
        int ADC_max = 13305;
        int clamped = Math.max(ADC_min, Math.min(ADC_max, rawPressureValue));
        double pressure = 1.0 + ((double) (clamped - ADC_min) / (ADC_max - ADC_min)) * (3.5 - 1);
        return Math.round(pressure * 100.0) / 100.0;
    }

    private double calibrateOxygenValue(int rawOxygenValue) {
        int a0 = 46;
        int a1 = 8045;
        int clamped = Math.max(a0, Math.min(a1, rawOxygenValue));
        double O2Value = ((double) (clamped - a0)) * 2090.0 / (double) (a1 - a0);
        return Math.round(O2Value) / 100.0;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Sensor 서비스가 종료되었습니다.");
        handler.removeCallbacks(sensorReadRunnable);
        handler.getLooper().quit();
        if (co2sensor != null) {
            co2sensor.cleanup();
        }
    }


}
