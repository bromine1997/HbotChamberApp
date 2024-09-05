package com.mcsl.hbotchamberapp.Activity;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.mcsl.hbotchamberapp.R;
import com.mcsl.hbotchamberapp.Sevice.GpioService;
import com.mcsl.hbotchamberapp.Sevice.ValveService;
import com.mcsl.hbotchamberapp.databinding.ActivityIoportBinding;

public class IoPortActivity extends AppCompatActivity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private ActivityIoportBinding binding;

    // 각 센서 데이터를 받기 위한 브로드캐스트 리시버들
    private BroadcastReceiver tempReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double temperature = intent.getDoubleExtra("temperature", -1);
            if (temperature != -1) {
                binding.tempValue.setText(temperature + " °C");
            }
        }
    };

    private BroadcastReceiver humidityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double humidity = intent.getDoubleExtra("humidity", -1);
            if (humidity != -1) {
                binding.humidityValue.setText(humidity + " %");
            }
        }
    };

    private BroadcastReceiver flowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double flowRate = intent.getDoubleExtra("flowRate", -1);
            if (flowRate != -1) {
                binding.flowValue.setText(flowRate + " lpm");
            }
        }
    };

    private BroadcastReceiver pressureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double pressure = intent.getDoubleExtra("pressure", -1);
            if (pressure != -1) {
                binding.pressureValue.setText(pressure + "ATA");
            }
        }
    };

    private BroadcastReceiver o2Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double oxygen = intent.getDoubleExtra("oxygen", -1);
            if (oxygen != -1) {
                binding.o2Value.setText(oxygen + " %");
            }
        }
    };

    private BroadcastReceiver co2ValuesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int co2Ppm = intent.getIntExtra("co2Ppm", -1);
            if (co2Ppm != -1) {
                binding.co2Value.setText(co2Ppm + " PPM");
            }
        }
    };

    private BroadcastReceiver ioStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte inputStatus = intent.getByteExtra("inputStatus", (byte) 0);
            updateTextViewStatus(inputStatus, binding.switch1Status, 0);
            updateTextViewStatus(inputStatus, binding.switch2Status, 1);
            updateTextViewStatus(inputStatus, binding.switch3Status, 2);
            updateTextViewStatus(inputStatus, binding.switch4Status, 3);
            updateTextViewStatus(inputStatus, binding.switch5Status, 4);
            updateTextViewStatus(inputStatus, binding.switch6Status, 5);
            updateTextViewStatus(inputStatus, binding.switch7Status, 6);
            updateTextViewStatus(inputStatus, binding.switch8Status, 7);
        }
    };

    private void initializeButtons() {
        binding.controlled1.setOnClickListener(v -> sendGpioServiceAction("com.mcsl.hbotchamberapp.action.TOGGLE_LED1"));
        binding.controlled2.setOnClickListener(v -> sendGpioServiceAction("com.mcsl.hbotchamberapp.action.TOGGLE_LED2"));
        binding.controlled3.setOnClickListener(v -> sendGpioServiceAction("com.mcsl.hbotchamberapp.action.TOGGLE_LED3"));
    }

    private void initializeValveButtons() {
        binding.controlSolPRESSON.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.SOL_PRESS_ON");                  //솔레노이드 온
        });
        binding.controlSolPRESSOFF.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.SOL_PRESS_OFF");                  //솔레노이드 오프
        });



        binding.ProportionalPRESSON.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.Proportional_PRESS_ON");                     //비례제어 온오프
        });
        binding.ProportionalPRESSOFF.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.Proportional_PRESS_OFF");                     //비례제어 온오프
        });



        binding.controlProportionPressDown.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.PRESS_VALVE_DOWN");
        });

        binding.controlProportionPressUP.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.PRESS_VALVE_UP");
        });



        binding.controlSolVENTON.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.SOL_VENT_ON");
        });

        binding.controlSolVENTOFF.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.SOL_VENT_OFF");
        });


        binding.ProportionalVENTON.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.Proportional_VENT_ON");
        });
        binding.ProportionalVENTOFF.setOnClickListener(v -> {
            sendValveServiceAction("com.mcsl.hbotchamberapp.action.Proportional_VENT_OFF");
        });

        binding.controlProportionVentDown.setOnClickListener(v -> {

            sendValveServiceAction("com.mcsl.hbotchamberapp.action.VENT_VALVE_DOWN");
        });

        binding.controlProportionVentUp.setOnClickListener(v -> {

            sendValveServiceAction("com.mcsl.hbotchamberapp.action.VENT_VALVE_UP");
        });
    }


    private void sendGpioServiceAction(String action) {
        Intent intent = new Intent(this, GpioService.class);
        intent.setAction(action);
        startService(intent);
    }

    private void sendValveServiceAction(String action) {
        Intent intent = new Intent(this, ValveService.class);
        intent.setAction(action);
        startService(intent);
    }

    private void updateTextViewStatus(byte inputStatus, TextView textView, int bit) {
        boolean isOn = ((inputStatus >> bit) & 1) == 1;
        textView.setText("Switch " + (bit + 1) + ": " + (isOn ? "ON" : "OFF"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(tempReceiver, new IntentFilter("com.mcsl.hbotchamberapp.Temp_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(humidityReceiver, new IntentFilter("com.mcsl.hbotchamberapp.Humidity_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(flowReceiver, new IntentFilter("com.mcsl.hbotchamberapp.Flow_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(pressureReceiver, new IntentFilter("com.mcsl.hbotchamberapp.PRESSURE_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(o2Receiver, new IntentFilter("com.mcsl.hbotchamberapp.O2_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(co2ValuesReceiver, new IntentFilter("com.mcsl.hbotchamberapp.CO2_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(ioStatusReceiver, new IntentFilter("com.mcsl.hbotchamberapp.IO_STATUS_UPDATE"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIoportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initializeButtons();
        initializeValveButtons();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(tempReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(humidityReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(flowReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pressureReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(o2Receiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(co2ValuesReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ioStatusReceiver);
    }
}
