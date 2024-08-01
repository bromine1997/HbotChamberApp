package com.mcsl.hbotchamberapp.Sevice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mcsl.hbotchamberapp.Controller.Ad5420;
import com.mcsl.hbotchamberapp.Controller.PinController;

public class ValveService extends Service {
    private static final String TAG = "GPIOService";
    private Handler handler;
    private Runnable valveRunnable;



    private static final String ACTION_Sol_PRESS_ON = "com.mcsl.hbotchamberapp.action.SOL_PRESS_ON";
    private static final String ACTION_Sol_PRESS_OFF = "com.mcsl.hbotchamberapp.action.SOL_PRESS_OFF";

    private static final String ACTION_Sol_VENT_ON = "com.mcsl.hbotchamberapp.action.SOL_VENT_ON";
    private static final String ACTION_Sol_VENT_OFF = "com.mcsl.hbotchamberapp.action.SOL_VENT_OFF";

    private static final String ACTION_Proportional_PRESS_ON = "com.mcsl.hbotchamberapp.action.Proportional_PRESS_ON";
    private static final String ACTION_Proportional_PRESS_OFF = "com.mcsl.hbotchamberapp.action.Proportional_PRESS_OFF";

    private static final String ACTION_Proportional_VENT_ON = "com.mcsl.hbotchamberapp.action.Proportional_VENT_ON";
    private static final String ACTION_Proportional_VENT_OFF = "com.mcsl.hbotchamberapp.action.Proportional_VENT_OFF";



    private static final String ACTION_PRESS_VALVE_DOWN = "com.mcsl.hbotchamberapp.action.PRESS_VALVE_DOWN";
    private static final String ACTION_PRESS_VALVE_UP = "com.mcsl.hbotchamberapp.action.PRESS_VALVE_UP";

    private static final String ACTION_VENT_VALVE_DOWN = "com.mcsl.hbotchamberapp.action.VENT_VALVE_DOWN";
    private static final String ACTION_VENT_VALVE_UP = "com.mcsl.hbotchamberapp.action.VENT_VALVE_UP";

    private PinController pinController;
    private Ad5420 ad5420;


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



        HandlerThread handlerThread = new HandlerThread("GPIOServiceBackgroundThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        valveRunnable = new Runnable() {
            @Override
            public void run() {
                // PID 제어를 포함한 벨브 제어 로직을 추가
                controlValves();  // PID제어 함수로 변경
                handler.postDelayed(this, 1000); // 1초마다 실행
            }
        };


        // 처음 실행
        handler.postDelayed(valveRunnable, 1000);

    }

    private void readAndBroadcastI2cValues() {
        byte inputStatus = pinController.readInputs();              //외부 입력 스위치 주기적으로 확인
        Intent intent = new Intent("com.example.test.IO_STATUS_UPDATE");
        intent.putExtra("inputStatus", inputStatus);
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



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Valve 서비스가 시작되었습니다.");

        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_Sol_PRESS_ON:
                        pinController.Sol_OUPUT(0,1);
                        sendBroadcastUpdate("SOL_PRESS_ON");
                        break;

                    case ACTION_Sol_PRESS_OFF:
                        pinController.Sol_OUPUT(0 , 0);
                        sendBroadcastUpdate("SOL_PRESS_OFF");
                        break;

                    case ACTION_Sol_VENT_ON:
                        pinController.Sol_OUPUT(1,1);
                        sendBroadcastUpdate("SOL_VENT_ON");
                        break;

                    case ACTION_Sol_VENT_OFF:
                        pinController.Sol_OUPUT(1,0);
                        sendBroadcastUpdate("SOL_VENT_OFF");
                        break;



                    case ACTION_Proportional_PRESS_ON:
                        pinController.Proportion_Press_ON();
                        sendBroadcastUpdate("PRESS");
                        break;
                    case ACTION_Proportional_PRESS_OFF:
                        pinController.Proportion_Press_OFF();
                        sendBroadcastUpdate("PRESS");
                        break;

                    case ACTION_Proportional_VENT_ON:
                        pinController.Proportion_VENT_ON();
                        sendBroadcastUpdate("PRESS");
                        break;
                    case ACTION_Proportional_VENT_OFF:
                        pinController.Proportion_VENT_OFF();
                        sendBroadcastUpdate("PRESS");
                        break;


                    case ACTION_PRESS_VALVE_DOWN:
                        ad5420.PressValveCurrentDown();
                        sendBroadcastUpdate("PRESS_VALVE_DOWN");
                        break;
                    case ACTION_PRESS_VALVE_UP:
                        ad5420.PressValveCurrentUp();
                        sendBroadcastUpdate("PRESS_VALVE_UP");
                        break;

                    case ACTION_VENT_VALVE_DOWN:
                        ad5420.VentValveCurrentDown();
                        sendBroadcastUpdate("VENT_VALVE_DOWN");
                        break;
                    case ACTION_VENT_VALVE_UP:
                        ad5420.VentValveCurrentUp();
                        sendBroadcastUpdate("VENT_VALVE_UP");
                        break;
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Valve 서비스가 종료되었습니다.");
        handler.removeCallbacks(valveRunnable);
        handler.getLooper().quit();  // HandlerThread 종료
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