package com.mcsl.hbotchamberapp.Activity;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.mcsl.hbotchamberapp.ViewModel.RunViewModel;
import com.mcsl.hbotchamberapp.databinding.ActivityRunBinding;
import com.mcsl.hbotchamberapp.model.PIDState;
import com.mcsl.hbotchamberapp.model.SensorData;
import com.mcsl.hbotchamberapp.util.ChartManager;

public class RunActivity extends AppCompatActivity {

    private ActivityRunBinding binding;
    private RunViewModel viewModel;
    private ChartManager chartManager;
    private boolean isPaused = false;
    private boolean isRunning = false;

    private final BroadcastReceiver safetyErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            showSafetyAlert(message != null ? message : "알 수 없는 오류가 발생했습니다");
        }
    };

    private final BroadcastReceiver wsConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isConnected = intent.getBooleanExtra("isConnected", true);
            binding.wsStatusBanner.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        }
    };

    private final BroadcastReceiver sensorErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showSafetyAlert("센서 연결 오류: 센서 데이터를 읽을 수 없습니다.\nPID 제어가 자동 정지됩니다.");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRunBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RunViewModel.class);
        chartManager = new ChartManager(binding.lineChart);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                safetyErrorReceiver, new IntentFilter("PID_SAFETY_ERROR"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                wsConnectionReceiver, new IntentFilter("WEBSOCKET_CONNECTION_STATUS"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                sensorErrorReceiver, new IntentFilter("SENSOR_CONNECTION_ERROR"));

        // Observe LiveData from ViewModel
        setupObservers();

        // Load profile data
        viewModel.loadProfileData(this);

        // Run button click event
        binding.btnRun.setOnClickListener(v -> viewModel.startPidControl());

        // End button click event
        binding.btnEnd.setOnClickListener(v -> viewModel.stopPidControl());




        // Pause/Resume button click event
        binding.btnPause.setOnClickListener(v -> {
            PIDState currentState = viewModel.getPidState().getValue();
            if (currentState == PIDState.RUNNING || currentState == PIDState.STARTED) {
                viewModel.pausePidControl();
            } else if (currentState == PIDState.PAUSED) {
                viewModel.resumePidControl();
            }
        });

    }



    private void setupObservers() {
        // Observe profile data
        viewModel.getProfileData().observe(this, data -> {
            if (data != null && !data.isEmpty()) {
                chartManager.updateProfileChart(data);
                Log.d(TAG, "Profile data updated.");
            } else {
                Log.d(TAG, "Profile data is empty.");
            }
        });


        // Observe sensor data
        viewModel.getSensorData().observe(this, sensorData -> {
            if (sensorData == null) return;
            PIDState currentState = viewModel.getPidState().getValue();
            if (currentState == PIDState.STARTED || currentState == PIDState.RUNNING) {
                long elapsedTime = viewModel.getElapsedTime().getValue() != null
                        ? viewModel.getElapsedTime().getValue()
                        : 0;
                chartManager.updatePressureChart(sensorData.getPressure(), elapsedTime);
                //Log.d(TAG, "Pressure data updated.");
            }
            updateSensorReadings(sensorData);
        });

        // Observe formatted elapsed time
        viewModel.getFormattedElapsedTime().observe(this, formattedTime -> {
            binding.elapsedTime.setText(formattedTime);
        });

        // Observe setPoint
        viewModel.getSetPoint().observe(this, setPoint -> {
            String setPointText = String.format("%.2f ATA", setPoint);
            binding.setPointPressure.setText(setPointText);
        });



        // Observe PID 상태

        // Observe PID 상태
        viewModel.getPidState().observe(this, pidState -> {
            Log.d(TAG, "RunActivity: Observed PID state: " + pidState);
            switch (pidState) {
                case STARTED:
                case RUNNING:
                    binding.btnRun.setEnabled(false);
                    binding.btnPause.setEnabled(true);
                    binding.btnEnd.setEnabled(true);
                    binding.btnPause.setText("Pause");
                    break;
                case PAUSED:
                    binding.btnPause.setText("Resume");
                    break;
                case STOPPED:
                    binding.btnRun.setEnabled(true);
                    binding.btnPause.setEnabled(false);
                    binding.btnEnd.setEnabled(false);
                    binding.btnPause.setText("Pause");
                    break;
            }
            // 추가적인 UI 업데이트 로직이 필요하면 여기에 작성
        });

    }

    private void updateChamberPressure(double pressure) {
        String pressureText = String.format("%.2f ATA", pressure);
        binding.chamberPressure.setText(pressureText);
    }

    private void updateSensorReadings(SensorData sensorData) {
        // Update Chamber Pressure
        String pressureText = String.format("%.2f ATA", sensorData.getPressure());
        binding.chamberPressure.setText(pressureText);

        // Update Temperature
        String temperatureText = String.format("%.2f °C", sensorData.getTemperature());
        binding.temperatureValue.setText(temperatureText);

        // Update Humidity
        String humidityText = String.format("%.2f %%", sensorData.getHumidity());
        binding.humidityValue.setText(humidityText);

        // Update Oxygen
        String oxygenText = String.format("%.2f %%", sensorData.getOxygen());
        binding.oxygenValue.setText(oxygenText);

        // Update Carbon Dioxide
        String co2Text = String.format("%.2f PPM", sensorData.getCo2());
        binding.carbonDioxideValue.setText(co2Text);


    }


    private void showSafetyAlert(String message) {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
                .setTitle("안전 경고")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(safetyErrorReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wsConnectionReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorErrorReceiver);
        binding = null;
    }
}
