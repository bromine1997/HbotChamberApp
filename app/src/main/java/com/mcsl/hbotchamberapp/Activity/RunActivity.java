package com.mcsl.hbotchamberapp.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.mcsl.hbotchamberapp.Sevice.PidService;
import com.mcsl.hbotchamberapp.databinding.ActivityRunBinding;
import com.mcsl.hbotchamberapp.Controller.SensorData;
import com.mcsl.hbotchamberapp.util.ChartManager;

public class RunActivity extends AppCompatActivity {

    private ActivityRunBinding binding;
    private RunViewModel viewModel;
    private ChartManager chartManager;
    private boolean isPaused = false;
    private boolean isRunning = false;
    private long elapsedTimeWhenPaused = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRunBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RunViewModel.class);
        chartManager = new ChartManager(binding.lineChart);

        // Observe LiveData from ViewModel
        setupObservers();

        // Load profile data
        viewModel.loadProfileData(this);

        // Run button click event
        binding.btnRun.setOnClickListener(v -> startPidControl());
        binding.btnEnd.setOnClickListener(v -> stopPidControl());

        // Pause button click event
        binding.btnPause.setOnClickListener(v -> togglePauseResume());

        // Register local broadcast receivers
        registerLocalReceivers();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If PID was running and not paused, restart PID control
        if (isRunning && !isPaused) {
            startPidControl();
        }

        // Register local broadcast receivers
        registerLocalReceivers();
    }

    private void setupObservers() {
        // Observe profile data
        viewModel.getProfileData().observe(this, data -> {
            chartManager.updateProfileChart(data);
        });

        // Observe sensor data
        viewModel.getSensorData().observe(this, sensorData -> {
            if (isRunning && !isPaused) {
                long elapsedTime = viewModel.getElapsedTime().getValue() != null
                        ? viewModel.getElapsedTime().getValue()
                        : 0;
                chartManager.updatePressureChart(sensorData.getPressure(), elapsedTime);
            }
            updateChamberPressure(sensorData.getPressure());
        });

        // Observe formatted elapsed time
        viewModel.getFormattedElapsedTime().observe(this, formattedTime -> {
            binding.elapsedTime.setText(formattedTime);
        });
    }

    private void startPidControl() {
        isRunning = true;
        viewModel.setPidControlRunning(true);
        isPaused = false;

        chartManager.clearPressureData();

        // Start PID service
        Intent pidIntent = new Intent(this, PidService.class);
        pidIntent.setAction("com.mcsl.hbotchamberapp.action.START_PID");
        startService(pidIntent);
    }

    private void stopPidControl() {
        isRunning = false;
        viewModel.setPidControlRunning(false);
        isPaused = false;

        // Stop PID service
        Intent stopIntent = new Intent(this, PidService.class);
        stopIntent.setAction("com.mcsl.hbotchamberapp.action.STOP_PID");
        startService(stopIntent);
    }

    private void togglePauseResume() {
        if (isPaused) {
            // Resume PID control
            isPaused = false;
            binding.btnPause.setText("Pause");

            // Resume PID service
            Intent resumeIntent = new Intent(this, PidService.class);
            resumeIntent.setAction("com.mcsl.hbotchamberapp.action.RESUME_PID");
            startService(resumeIntent);

            // Adjust elapsed time
            long currentTime = System.currentTimeMillis();
            elapsedTimeWhenPaused = currentTime - elapsedTimeWhenPaused;
            viewModel.setElapsedTime(elapsedTimeWhenPaused);

            // Re-register elapsed time receiver
            LocalBroadcastManager.getInstance(this).registerReceiver(elapsedTimeReceiver, new IntentFilter("com.mcsl.hbotchamberapp.ELAPSED_TIME_UPDATE"));
        } else {
            // Pause PID control
            isPaused = true;
            binding.btnPause.setText("Resume");

            // Pause PID service
            Intent pauseIntent = new Intent(this, PidService.class);
            pauseIntent.setAction("com.mcsl.hbotchamberapp.action.PAUSE_PID");
            startService(pauseIntent);

            // Record the time when paused
            elapsedTimeWhenPaused = System.currentTimeMillis();

            // Unregister elapsed time receiver
            LocalBroadcastManager.getInstance(this).unregisterReceiver(elapsedTimeReceiver);
        }
    }

    private void registerLocalReceivers() {
        // Register receivers
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorValuesReceiver, new IntentFilter("com.mcsl.hbotchamberapp.SENSOR_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(elapsedTimeReceiver, new IntentFilter("com.mcsl.hbotchamberapp.ELAPSED_TIME_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(setPointReceiver, new IntentFilter("com.mcsl.hbotchamberapp.SETPOINT_UPDATE"));
    }

    private void updateChamberPressure(double pressure) {
        String pressureText = String.format("%.2f ATA", pressure);
        binding.chamberPressure.setText(pressureText);
    }



    // BroadcastReceivers
    private final BroadcastReceiver elapsedTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long elapsedTime = intent.getLongExtra("elapsedTime", 0);
            viewModel.setElapsedTime(elapsedTime);
        }
    };

    private final BroadcastReceiver setPointReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double setPoint = intent.getDoubleExtra("setPoint", 0.0);
            viewModel.setSetPoint(setPoint);
        }
    };

    private final BroadcastReceiver sensorValuesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double pressure = intent.getDoubleExtra("pressure", -1);
            double temperature = intent.getDoubleExtra("temperature", -1);
            double humidity = intent.getDoubleExtra("humidity", -1);
            double flowRate = intent.getDoubleExtra("flowRate", -1);
            double oxygen = intent.getDoubleExtra("oxygen", -1);
            int co2Ppm = intent.getIntExtra("co2Ppm", -1);

            SensorData sensorData = new SensorData(pressure, temperature, humidity, flowRate, oxygen, co2Ppm);
            viewModel.updateSensorData(sensorData);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorValuesReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(elapsedTimeReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(setPointReceiver);
    }
}
