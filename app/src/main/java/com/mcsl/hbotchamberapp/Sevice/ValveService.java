package com.mcsl.hbotchamberapp.Sevice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    private BroadcastReceiver pidOutputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.mcsl.hbotchamberapp.PID_OUTPUT_UPDATE".equals(intent.getAction())) {
                double pidOutput = intent.getDoubleExtra("pidOutput", 0.0);
                Log.d(TAG, "Received PID output: " + pidOutput);
                // PID 출력 값을 이용해 비례제어 밸브를 제어하는 로직을 추가
                controlProportionalValve(pidOutput);
            }
        }
    };

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
                //controlValves();
                handler.postDelayed(this, 1000); // 1초마다 실행
            }
        };

        handler.postDelayed(valveRunnable, 1000);

        LocalBroadcastManager.getInstance(this).registerReceiver(pidOutputReceiver,
                new IntentFilter("com.mcsl.hbotchamberapp.PID_OUTPUT_UPDATE"));
    }

    private void controlProportionalValve(double pidOutput) {
        double minCurrent = 4.0;  // 4mA
        double maxCurrent = 20.0; // 20mA

// PID 출력 값을 이용하여 출력하고자 하는 전류를 계산 (0~100%를 이용하여)
        double desiredCurrent = minCurrent + ((maxCurrent - minCurrent) * (pidOutput / 100.0));

// 전류 값을 DAC의 16비트 값으로 변환 (0x0000 ~ 0xFFFF)
        short dacValue = (short) ((desiredCurrent - minCurrent) / (maxCurrent - minCurrent) * 0xFFFF);

// AD5420에 전달 (0x0000은 채널 번호, dacValue는 전류 설정)
        ad5420.DaisyCurrentWrite((char) 0, dacValue);
        Log.d(TAG, "Proportional valve set to: " + dacValue + "mA");
    }

    private void controlValves() {
        Log.d(TAG, "Valves PID Start");
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
                        pinController.Sol_OUPUT(0, 1);
                        sendBroadcastUpdate("SOL_PRESS_ON");
                        break;

                    case ACTION_Sol_PRESS_OFF:
                        pinController.Sol_OUPUT(0, 0);
                        sendBroadcastUpdate("SOL_PRESS_OFF");
                        break;

                    case ACTION_Sol_VENT_ON:
                        pinController.Sol_OUPUT(1, 1);
                        sendBroadcastUpdate("SOL_VENT_ON");
                        break;

                    case ACTION_Sol_VENT_OFF:
                        pinController.Sol_OUPUT(1, 0);
                        sendBroadcastUpdate("SOL_VENT_OFF");
                        break;

                    case ACTION_Proportional_PRESS_ON:
                        pinController.Proportion_Press_ON();
                        sendBroadcastUpdate("PRESS_ON");
                        break;
                    case ACTION_Proportional_PRESS_OFF:
                        pinController.Proportion_Press_OFF();
                        sendBroadcastUpdate("PRESS_OFF");
                        break;

                    case ACTION_Proportional_VENT_ON:
                        pinController.Proportion_VENT_ON();
                        sendBroadcastUpdate("VENT_ON");
                        break;
                    case ACTION_Proportional_VENT_OFF:
                        pinController.Proportion_VENT_OFF();
                        sendBroadcastUpdate("VENT_OFF");
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
        handler.getLooper().quit(); // HandlerThread 종료
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pidOutputReceiver);
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
