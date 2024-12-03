package com.mcsl.hbotchamberapp.Activity;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.util.Log;

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
            PIDState currentState = viewModel.getPidState().getValue();
            if (currentState == PIDState.STARTED || currentState == PIDState.RUNNING) {
                long elapsedTime = viewModel.getElapsedTime().getValue() != null
                        ? viewModel.getElapsedTime().getValue()
                        : 0;
                chartManager.updatePressureChart(sensorData.getPressure(), elapsedTime);
                Log.d(TAG, "Pressure data updated.");
            }
            updateSensorReadings(sensorData);
        });

        // Observe formatted elapsed time
        viewModel.getFormattedElapsedTime().observe(this, formattedTime -> {
            binding.elapsedTime.setText(formattedTime);
        });

        // Observe setPoint
        viewModel.getSetPoint().observe(this, setPoint -> {
            String setPointText = String.format("%.2f", setPoint);
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
        String co2Text = String.format("%.2f %%", sensorData.getCo2());
        binding.carbonDioxideValue.setText(co2Text);

        // Update Flow Rate
        String flowRateText = String.format("%.2f L/min", sensorData.getFlowRate());
        binding.flowRate.setText(flowRateText);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;

    }
}
