package com.mcsl.hbotchamberapp.Activity;


import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;


import android.os.Bundle;

import android.widget.TextView;

import com.mcsl.hbotchamberapp.model.SensorData;

import com.mcsl.hbotchamberapp.ViewModel.IoPortViewModel;
import com.mcsl.hbotchamberapp.databinding.ActivityIoportBinding;

public class IoPortActivity extends AppCompatActivity {

    private ActivityIoportBinding binding;
    private IoPortViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityIoportBinding.inflate(getLayoutInflater());

        viewModel = new ViewModelProvider(this).get(IoPortViewModel.class);

        setContentView(binding.getRoot());

        // 센서 데이터 관찰
        viewModel.getSensorData().observe(this, sensorData -> {
            updateSensorData(sensorData);
        });

        // 입력 상태 관찰
        viewModel.getInputStatus().observe(this, status -> {
            updateInputStatus(status);
        });

        // **전류 값 관찰**
        viewModel.getPressValveCurrent().observe(this, currentInMA -> {
            binding.proportionPressValue.setText(String.format("%.2f mA", currentInMA));
        });

        viewModel.getVentValveCurrent().observe(this, currentInMA -> {
            binding.proportionVentValue.setText(String.format("%.2f mA", currentInMA));
        });


        initializeButtons();
        initializeValveButtons();
    }





    private void initializeButtons() {
        binding.controlled1.setOnClickListener(v -> viewModel.toggleLed(1));
        binding.controlled2.setOnClickListener(v -> viewModel.toggleLed(2));
        binding.controlled3.setOnClickListener(v -> viewModel.toggleLed(3));
    }

    private void initializeValveButtons() {
        binding.controlSolPRESSON.setOnClickListener(v -> viewModel.solPressOn());
        binding.controlSolPRESSOFF.setOnClickListener(v -> viewModel.solPressOff());

        binding.controlSolVENTON.setOnClickListener(v -> viewModel.solVentOn());
        binding.controlSolVENTOFF.setOnClickListener(v -> viewModel.solVentOff());

        binding.controlProportionPressDown.setOnClickListener(v -> viewModel.pressProportionalValveDown());
        binding.controlProportionPressUP.setOnClickListener(v -> viewModel.pressProportionalValveUp());

        binding.controlProportionVentDown.setOnClickListener(v -> viewModel.ventProportionalValveDown());
        binding.controlProportionVentUp.setOnClickListener(v -> viewModel.ventProportionalValveUp());

    }

    private void updateSensorData(SensorData data) {
        if (data != null) {
            binding.tempValue.setText(data.getTemperature() + " °C");
            binding.humidityValue.setText(data.getHumidity() + " %");
            binding.flowValue.setText(data.getFlowRate() + " lpm");
            binding.pressureValue.setText(data.getPressure() + " ATA");
            binding.o2Value.setText(data.getOxygen() + " %");
            binding.co2Value.setText(data.getCo2() + " PPM");
        }
    }

    private void updateInputStatus(byte inputStatus) {
        updateTextViewStatus(inputStatus, binding.switch1Status, 0);
        updateTextViewStatus(inputStatus, binding.switch2Status, 1);
        updateTextViewStatus(inputStatus, binding.switch3Status, 2);
        updateTextViewStatus(inputStatus, binding.switch4Status, 3);
        updateTextViewStatus(inputStatus, binding.switch5Status, 4);
        updateTextViewStatus(inputStatus, binding.switch6Status, 5);
        updateTextViewStatus(inputStatus, binding.switch7Status, 6);
        updateTextViewStatus(inputStatus, binding.switch8Status, 7);
    }

    private void updateTextViewStatus(byte inputStatus, TextView textView, int bit) {
        boolean isOn = ((inputStatus >> bit) & 1) == 1;
        textView.setText("Switch " + (bit + 1) + ": " + (isOn ? "ON" : "OFF"));
    }


}
