package com.mcsl.hbotchamberapp.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
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
                binding.co2Value.setText(adcValues[1] + " %");
                binding.humidityValue.setText(adcValues[2] + " %");
                binding.tempValue.setText(adcValues[3] + " °C");
                binding.pressureValue.setText(adcValues[4] + " ATA");
                binding.flowValue.setText(adcValues[5] + " lpm");
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
        binding.controlled1.setOnClickListener(v -> {
            Intent intent = new Intent(this, GpioService.class);
            intent.setAction("com.example.test.action.TOGGLE_LED1");
            startService(intent);
        });

        binding.controlled2.setOnClickListener(v -> {
            Intent intent = new Intent(this, GpioService.class);
            intent.setAction("com.example.test.action.TOGGLE_LED2");
            startService(intent);
        });

        binding.controlled3.setOnClickListener(v -> {
            Intent intent = new Intent(this, GpioService.class);
            intent.setAction("com.example.test.action.TOGGLE_LED3");
            startService(intent);
        });
    }

    private void initializeValveButtons() {
        binding.controlProportionPressButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ValveService.class);
            intent.setAction("com.example.test.action.TOGGLE_PRESS");
            startService(intent);
        });

        binding.controlSolenoidPressButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ValveService.class);
            intent.setAction("com.example.test.action.TOGGLE_PRESS");
            startService(intent);
        });

        binding.controlProportionPressDown.setOnClickListener(v -> {
            Intent intent = new Intent(this, ValveService.class);
            intent.setAction("com.example.test.action.PRESS_VALVE_DOWN");
            startService(intent);
        });

        binding.controlProportionPressUP.setOnClickListener(v -> {
            Intent intent = new Intent(this, ValveService.class);
            intent.setAction("com.example.test.action.PRESS_VALVE_UP");
            startService(intent);
        });

        binding.controlSolenoidVentButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ValveService.class);
            intent.setAction("com.example.test.action.TOGGLE_VENT");
            startService(intent);
        });

        binding.controlProportionVentButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ValveService.class);
            intent.setAction("com.example.test.action.TOGGLE_VENT");
            startService(intent);
        });

        binding.controlProportionVentDown.setOnClickListener(v -> {
            Intent intent = new Intent(this, ValveService.class);
            intent.setAction("com.example.test.action.VENT_VALVE_DOWN");
            startService(intent);
        });

        binding.controlProportionVentUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, ValveService.class);
            intent.setAction("com.example.test.action.VENT_VALVE_UP");
            startService(intent);
        });
    }

    private void updateTextViewStatus(byte inputStatus, TextView textView, int bit) {
        boolean isOn = ((inputStatus >> bit) & 1) == 1;
        textView.setText("Switch " + (bit + 1) + ": " + (isOn ? "ON" : "OFF"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(adcValuesReceiver, new IntentFilter("com.example.test.ADC_VALUES"));
        LocalBroadcastManager.getInstance(this).registerReceiver(ioStatusReceiver, new IntentFilter("com.example.test.IO_STATUS_UPDATE"));
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(ioStatusReceiver);
    }
}
