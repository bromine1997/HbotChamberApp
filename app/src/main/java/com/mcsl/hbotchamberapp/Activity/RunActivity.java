package com.mcsl.hbotchamberapp.Activity;

import static android.text.format.DateUtils.formatElapsedTime;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mcsl.hbotchamberapp.Sevice.PidService;
import com.mcsl.hbotchamberapp.databinding.ActivityRunBinding;
import com.mcsl.hbotchamberapp.model.ProfileSection;
import com.mcsl.hbotchamberapp.Controller.SensorData;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RunActivity extends AppCompatActivity {

    private LineChart chart;
    private LineData lineData;
    private LineDataSet profileDataSet;
    private LineDataSet pressureDataSet;
    private ActivityRunBinding binding;
    private RunViewModel viewModel;
    private boolean isPaused = false;
    private boolean isRunning = false;
    private long elapsedTimeWhenPaused = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRunBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RunViewModel.class);
        chart = binding.lineChart;

        // Chart 설정
        setupChart();

        // ViewModel에서 프로파일 데이터와 센서 데이터 구독
        setupObservers();

        // 초기 프로파일 데이터를 차트에 그리기
        if (viewModel.getProfileData().getValue() != null) {
            updateProfileChart(viewModel.getProfileData().getValue());
        } else {
            loadAndSetProfileData();  // 파일에서 로드된 데이터를 ViewModel에 설정
        }

        // Run 버튼 클릭 이벤트 처리
        binding.btnRun.setOnClickListener(v -> startPidControl());
        binding.btnEnd.setOnClickListener(v -> stopPidControl());

        // Pause 버튼 클릭 이벤트 처리
        binding.btnPause.setOnClickListener(v -> togglePauseResume());

        // 로컬 브로드캐스트 리시버 등록
        registerLocalReceivers();
    }

    private void togglePauseResume() {
        if (isPaused) {
            // Resume PID control
            isPaused = false;
            binding.btnPause.setText("Pause");

            // Resume PID 서비스
            Intent resumeIntent = new Intent(this, PidService.class);
            resumeIntent.setAction("com.mcsl.hbotchamberapp.action.RESUME_PID");
            startService(resumeIntent);

            // 일시정지 시간을 빼고 재개
            long currentTime = System.currentTimeMillis();
            elapsedTimeWhenPaused = currentTime - elapsedTimeWhenPaused;  // 일시정지된 시간 빼기
            viewModel.setElapsedTime(elapsedTimeWhenPaused);  // ViewModel에 경과 시간 업데이트

            // ElapsedTime 브로드캐스트 수신 다시 등록
            LocalBroadcastManager.getInstance(this).registerReceiver(elapsedTimeReceiver, new IntentFilter("com.mcsl.hbotchamberapp.ELAPSED_TIME_UPDATE"));
        } else {
            // Pause PID control
            isPaused = true;
            binding.btnPause.setText("Resume");

            // Pause PID 서비스
            Intent pauseIntent = new Intent(this, PidService.class);
            pauseIntent.setAction("com.mcsl.hbotchamberapp.action.PAUSE_PID");
            startService(pauseIntent);

            // 현재 시간을 기록하여 나중에 resume 시 계산에 사용
            elapsedTimeWhenPaused = System.currentTimeMillis();

            // ElapsedTime 브로드캐스트 수신 중단
            LocalBroadcastManager.getInstance(this).unregisterReceiver(elapsedTimeReceiver);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        // ViewModel에 저장된 프로파일 데이터가 있는 경우 차트를 업데이트
        if (viewModel.getProfileData().getValue() != null) {
            updateProfileChart(viewModel.getProfileData().getValue());
        }

        // ViewModel에 저장된 압력 데이터가 있는 경우 차트를 업데이트
        if (viewModel.getSensorData().getValue() != null) {
            SensorData sensorData = viewModel.getSensorData().getValue();
            updatePressureChart(sensorData.getPressure());
            updateChamberPressure(sensorData.getPressure());
        }

        // PID가 실행 중이었고, 일시 정지가 아니면 PID 제어를 다시 시작
        if (isRunning && !isPaused) {
            startPidControl();  // PID가 실행 중이었다면 다시 시작
        }

        // 센서 데이터 및 경과 시간 수신 브로드캐스트 리시버 등록
        registerLocalReceivers();
    }

    private void setupChart() {
        profileDataSet = new LineDataSet(null, "Profile Data");
        profileDataSet.setColor(ColorTemplate.getHoloBlue());
        profileDataSet.setLineWidth(3f);
        profileDataSet.setDrawCircles(true);
        profileDataSet.setDrawValues(false);
        profileDataSet.setDrawFilled(true);
        profileDataSet.setFillColor(Color.BLUE);

        pressureDataSet = new LineDataSet(null, "Pressure Data");
        pressureDataSet.setColor(Color.RED);
        pressureDataSet.setLineWidth(4f);
        pressureDataSet.setDrawCircles(false);
        pressureDataSet.setDrawValues(false);
        pressureDataSet.setDrawFilled(true);
        pressureDataSet.setFillColor(Color.RED);

        lineData = new LineData();
        lineData.addDataSet(profileDataSet);
        lineData.addDataSet(pressureDataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yAxis = chart.getAxisLeft();
       // yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(4f);
        chart.getAxisRight().setEnabled(false);  // 오른쪽 Y축 비활성화
    }

    private void setupObservers() {
        // 프로파일 데이터 변경 시 차트 업데이트
        viewModel.getProfileData().observe(this, this::updateProfileChart);

        // 센서 데이터 변경 시 차트 및 UI 업데이트
        viewModel.getSensorData().observe(this, sensorData -> {
            updatePressureChart(sensorData.getPressure());
            updateChamberPressure(sensorData.getPressure());
        });

        // 경과 시간 업데이트
        viewModel.getElapsedTime().observe(this, elapsedTime -> binding.elapsedTime.setText(formatElapsedTime(elapsedTime)));
    }

    private void startPidControl() {
        isRunning = true;
        viewModel.setPidControlRunning(true);  // ViewModel에서 상태를 관리
        isPaused = false;

        pressureDataSet.clear();  // 새로운 런을 위해 압력 데이터셋 초기화
        chart.invalidate();

        // PID 제어 시작
        Intent pidIntent = new Intent(this, PidService.class);
        pidIntent.setAction("com.mcsl.hbotchamberapp.action.START_PID");
        startService(pidIntent);
    }

    private void stopPidControl() {
        isRunning = false;
        viewModel.setPidControlRunning(false);  // ViewModel에서 상태를 관리
        isPaused = false;

        // PID 제어 중지
        Intent stopIntent = new Intent(this, PidService.class);
        stopIntent.setAction("com.mcsl.hbotchamberapp.action.STOP_PID");
        startService(stopIntent);
    }

    private void registerLocalReceivers() {
        // 센서 값 및 PID 관련 수신을 위한 로컬 브로드캐스트 리시버 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(sensorValuesReceiver, new IntentFilter("com.mcsl.hbotchamberapp.SENSOR_UPDATE"));
        LocalBroadcastManager.getInstance(this).registerReceiver(elapsedTimeReceiver, new IntentFilter("com.mcsl.hbotchamberapp.ELAPSED_TIME_UPDATE"));
        // 리시버 등록
        LocalBroadcastManager.getInstance(this).registerReceiver(
                setPointReceiver, new IntentFilter("com.mcsl.hbotchamberapp.SETPOINT_UPDATE")
        );
    }

    // ElapsedTime 브로드캐스트 수신
    private final BroadcastReceiver elapsedTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long elapsedTime = intent.getLongExtra("elapsedTime", 0);
            viewModel.setElapsedTime(elapsedTime);  // ViewModel에 경과 시간 업데이트
        }
    };

    // SetPoint 값을 수신하는 리시버 추가
    private final BroadcastReceiver setPointReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double setPoint = intent.getDoubleExtra("setPoint", 0.0);
            viewModel.setSetPoint(setPoint); // ViewModel에 SetPoint 업데이트
        }
    };

    // Sensor 데이터 수신 리시버 설정
    private final BroadcastReceiver sensorValuesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double pressure = intent.getDoubleExtra("pressure", -1);
            double temperature = intent.getDoubleExtra("temperature", -1);
            double humidity = intent.getDoubleExtra("humidity", -1);
            double flowRate = intent.getDoubleExtra("flowRate", -1);
            double oxygen = intent.getDoubleExtra("oxygen", -1);
            int co2Ppm = intent.getIntExtra("co2Ppm", -1);

            // 추출한 데이터를 ViewModel에 전달하여 UI 업데이트
            SensorData sensorData = new SensorData(pressure, temperature, humidity, flowRate, oxygen, co2Ppm);
            viewModel.updateSensorData(sensorData);
        }
    };

    private void updateProfileChart(List<ProfileSection> data) {
        profileDataSet.clear();  // 기존 프로파일 데이터 지우기
        float currentTime = 0;

        for (ProfileSection section : data) {
            profileDataSet.addEntry(new Entry(currentTime, section.getStartPressure()));
            currentTime += section.getDuration() * 60; // 분을 초로 변환
            profileDataSet.addEntry(new Entry(currentTime, section.getEndPressure()));
        }

        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void updatePressureChart(double pressure) {
        if (isRunning && !isPaused) {
            long elapsedTime = viewModel.getElapsedTime().getValue() != null
                    ? viewModel.getElapsedTime().getValue()
                    : 0;
            float currentTime = elapsedTime / 1000f;  // X축을 초 단위로 설정
            pressureDataSet.addEntry(new Entry(currentTime, (float) pressure));
            lineData.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }


    private void updateChamberPressure(double pressure) {
        String pressureText = String.format("%.2f ATA", pressure);
        binding.chamberPressure.setText(pressureText);
    }

    private void loadAndSetProfileData() {
        // 파일에서 프로파일 데이터를 로드하고 ViewModel에 설정
        List<String[]> stringData = loadProfileDataFromFile();
        if (stringData != null) {
            List<ProfileSection> profileData = convertToProfileSection(stringData);
            viewModel.setProfileData(profileData);  // ViewModel에 설정
        }
    }

    private List<String[]> loadProfileDataFromFile() {
        List<String[]> profileData = new ArrayList<>();
        try (FileInputStream fis = openFileInput("profile_data.json");
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            Gson gson = new Gson();
            Type type = new TypeToken<List<String[]>>() {}.getType();
            profileData = gson.fromJson(sb.toString(), type);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
        }

        return profileData;
    }

    private List<ProfileSection> convertToProfileSection(List<String[]> stringData) {
        List<ProfileSection> profileSections = new ArrayList<>();
        for (String[] section : stringData) {
            if (section.length >= 3) {
                float startPressure = Float.parseFloat(section[1]);
                float endPressure = Float.parseFloat(section[2]);
                float duration = Float.parseFloat(section[3]);

                profileSections.add(new ProfileSection("SectionName", startPressure, endPressure, duration));
            }
        }
        return profileSections;
    }

    private String formatElapsedTime(long elapsedTime) {

        int seconds = (int) (elapsedTime / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorValuesReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(elapsedTimeReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(setPointReceiver);
    }
}
