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

    private BroadcastReceiver adcValuesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int[] adcValues = intent.getIntArrayExtra("adcValues");
            if (adcValues != null) {
                binding.o2Value.setText(adcValues[0] + " %");
                binding.humidityValue.setText(adcValues[2] + " %");
                binding.tempValue.setText(adcValues[3] + " °C");
                binding.pressureValue.setText(adcValues[4] + " ATA");
                binding.flowValue.setText(adcValues[5] + " lpm");
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
        LocalBroadcastManager.getInstance(this).registerReceiver(adcValuesReceiver, new IntentFilter("com.mcsl.hbotchamberapp.ADC_VALUES"));
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(adcValuesReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(co2ValuesReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ioStatusReceiver);
    }
}
