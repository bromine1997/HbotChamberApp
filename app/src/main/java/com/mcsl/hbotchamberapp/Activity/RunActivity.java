package com.mcsl.hbotchamberapp.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import com.mcsl.hbotchamberapp.ViewModel.RunViewModel;
import com.mcsl.hbotchamberapp.databinding.ActivityRunBinding;
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
        binding.btnRun.setOnClickListener(v -> {
            viewModel.startPidControl();
            isRunning = true;
        });

        binding.btnEnd.setOnClickListener(v -> {
            viewModel.stopPidControl();
            isRunning = false;
        });

        // Pause button click event
        binding.btnPause.setOnClickListener(v -> {
            if (isPaused) {
                viewModel.resumePidControl();
                isPaused = false;
                binding.btnPause.setText("Pause");
            } else {
                viewModel.pausePidControl();
                isPaused = true;
                binding.btnPause.setText("Resume");
            }
        });
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

        // Observe setPoint
        viewModel.getSetPoint().observe(this, setPoint -> {
            String setPointText = String.format("%.2f", setPoint);
            binding.setPointPressure.setText(setPointText);
        });

        // Observe pidPhase

    }



    private void updateChamberPressure(double pressure) {
        String pressureText = String.format("%.2f ATA", pressure);
        binding.chamberPressure.setText(pressureText);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;

    }
}
