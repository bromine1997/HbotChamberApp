package com.mcsl.hbotchamberapp.Service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mcsl.hbotchamberapp.Controller.Ad5420;
import com.mcsl.hbotchamberapp.Controller.PinController;

public class ValveService extends Service {
    private static final String TAG = "ValveService";


    private MutableLiveData<Double> pressValveCurrentLiveData = new MutableLiveData<>();
    private MutableLiveData<Double> ventValveCurrentLiveData = new MutableLiveData<>();


    private PinController pinController;
    private Ad5420 ad5420;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public ValveService getService() {
            return ValveService.this;
        }
    }

    public LiveData<Double> getPressValveCurrentLiveData() {
        return pressValveCurrentLiveData;
    }

    public LiveData<Double> getVentValveCurrentLiveData() {
        return ventValveCurrentLiveData;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        pinController = new PinController();
        ad5420 = new Ad5420(0);

        // Daisy_reset을 실행하고 1밀리초 지연 후 Daisy_Setup을 실행
        ad5420.Daisy_reset();
        try {
            Thread.sleep(1); // 1밀리초 지연
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ad5420.Daisy_Setup();
    }


    private BroadcastReceiver pressPidOutputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.mcsl.hbotchamberapp.PRESS_VALVE_CONTROL".equals(intent.getAction())) {
                double pidOutput = intent.getDoubleExtra("pidOutput", 0.0);
                Log.d(TAG, "Received PID output: " + pidOutput);
                // PID 출력 값을 이용해 비례제어 밸브를 제어하는 로직을 추가
                PidControlPressProportionValve(pidOutput);
            }
        }
    };

    private BroadcastReceiver ventPidOutputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.mcsl.hbotchamberapp.VENT_VALVE_CONTROL".equals(intent.getAction())) {
                double pidOutput = intent.getDoubleExtra("pidOutput", 0.0);
                Log.d(TAG, "Received PID output: " + pidOutput);
                // PID 출력 값을 이용해 비례제어 밸브를 제어하는 로직을 추가
                PidControlVentProportionValve(pidOutput);
            }
        }
    };

    private BroadcastReceiver stopAllValvesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.mcsl.hbotchamberapp.STOP_ALL_VALVES".equals(intent.getAction())) {
                // 모든 벨브를 끄는 로직
                stopAllValves();
            }
        }
    };

    public void solPressOn() {
        pinController.Sol_OUPUT(0, 1);
    }

    public void solPressOff() {
        pinController.Sol_OUPUT(0, 0);
    }

    public void pressValveUp() {
        ad5420.PressValveCurrentUp();
        double currentInMA = ad5420.getPressCurrentInMA();
        pressValveCurrentLiveData.postValue(currentInMA);
    }

    public void pressValveDown() {
        ad5420.PressValveCurrentDown();
        double currentInMA = ad5420.getPressCurrentInMA();
        pressValveCurrentLiveData.postValue(currentInMA);
    }


    public void solVentOn() {
        pinController.Sol_OUPUT(1, 1);
    }

    public void solVentOff() {
        pinController.Sol_OUPUT(1, 0);
    }

    public void ventValveUp() {
        ad5420.VentValveCurrentUp();
        double currentInMA = ad5420.getVentCurrentInMA();
        ventValveCurrentLiveData.postValue(currentInMA);

    }

    public void ventValveDown() {
        ad5420.VentValveCurrentDown();
        double currentInMA = ad5420.getVentCurrentInMA();
        ventValveCurrentLiveData.postValue(currentInMA);

    }






    private void PidControlPressProportionValve(double pidOutput) {

        pinController.Sol_OUPUT(0, 1);          // 가압 솔 벨브 on
        pinController.Sol_OUPUT(1, 0);          // 배기 솔 벨브 off

        double minCurrent = 4.0;  // 4mA
        double maxCurrent = 20.0; // 20mA

        // PID 출력 값을 이용하여 출력하고자 하는 전류를 계산 (0~100%를 이용하여)
        double desiredCurrent = minCurrent + ((maxCurrent - minCurrent) * (pidOutput / 100.0));

        // 전류 값을 DAC의 16비트 값으로 변환 (0x0000 ~ 0xFFFF)
        short dacValue = (short) ((desiredCurrent - minCurrent) / (maxCurrent - minCurrent) * 0xFFFF);

        // AD5420에 전달 (0x0000은 채널 번호, dacValue는 전류 설정)
        ad5420.DaisyCurrentWrite((char) 0, dacValue);

    }

    private void PidControlVentProportionValve(double pidOutput) {

        pinController.Sol_OUPUT(0, 0);          // 가압 솔 벨브 off
        pinController.Sol_OUPUT(1, 1);          // 배기 솔 벨브 on

        double minCurrent = 4.0;  // 4mA
        double maxCurrent = 20.0; // 20mA

        // PID 출력 값을 이용하여 출력하고자 하는 전류를 계산 (0~100%를 이용하여)
        double desiredCurrent = minCurrent + ((maxCurrent - minCurrent) * (pidOutput / 100.0));

        // 전류 값을 DAC의 16비트 값으로 변환 (0x0000 ~ 0xFFFF)
        short dacValue = (short) ((desiredCurrent - minCurrent) / (maxCurrent - minCurrent) * 0xFFFF);

        // AD5420에 전달 (0x0000은 채널 번호, dacValue는 전류 설정)
        ad5420.DaisyCurrentWrite((char) 1, dacValue);

    }


    private void stopAllValves() {
        // 모든 벨브를 끄는 로직 구현
        pinController.Sol_OUPUT(0, 0);  // 가압 솔 벨브 OFF
        pinController.Sol_OUPUT(1, 0);  // 배기 솔 벨브 OFF
        pinController.Proportion_Press_OFF();  // 비례제어 가압 벨브 OFF
        pinController.Proportion_VENT_OFF();   // 비례제어 배기 벨브 OFF
        ad5420.DaisyCurrentWrite((char) 0, (short) 0); // 압력 벨브 전류 0으로 설정
        ad5420.DaisyCurrentWrite((char) 1, (short) 0); // 배기 벨브 전류 0으로 설정
        Log.d(TAG, "모든 벨브가 종료되었습니다.");
    }





    @Override
    public void onDestroy() {
        super.onDestroy();
        // 리소스 정리 필요 시 추가
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
