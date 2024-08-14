package com.mcsl.hbotchamberapp.Activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RunActivity extends AppCompatActivity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private LineChart chart;
    private LineData lineData;
    private LineDataSet profileDataSet;
    private LineDataSet pressureDataSet;
    private float minY = Float.MAX_VALUE;
    private float maxY = Float.MIN_VALUE;
    private long startTime;
    private long elapsedTimeWhenPaused = 0;
    private ActivityRunBinding binding;
    private RunViewModel viewModel;

    private boolean isPaused = false;
    private boolean isRunning = false;

    private Button btnPauseResume;
    private boolean isGasAnalyzerDialogOpen = false;

    private BroadcastReceiver sensorValuesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isGasAnalyzerDialogOpen) {
                if ("com.mcsl.hbotchamberapp.PRESSURE_UPDATE".equals(intent.getAction())) {
                    double pressure = intent.getDoubleExtra("pressure", -1);
                    if (pressure != -1 && isRunning) {
                        updatePressureChart(pressure);  // 압력 값 업데이트 메서드 호출
                        updateChamberPressure(pressure);
                    }
                }
            }
        }
    };

    private BroadcastReceiver stopGraphReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.mcsl.hbotchamberapp.STOP_GRAPH_UPDATE".equals(intent.getAction())) {
                handler.removeCallbacks(elapsedTimeRunnable);  // 그래프 업데이트를 중지
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRunBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RunViewModel.class);
        chart = binding.lineChart;

        btnPauseResume = binding.btnPause;  // 버튼 초기화

        profileDataSet = new LineDataSet(new ArrayList<>(), "Profile Data");
        profileDataSet.setColor(ColorTemplate.getHoloBlue());
        profileDataSet.setLineWidth(3f);
        profileDataSet.setDrawCircles(true);
        profileDataSet.setDrawValues(false);
        profileDataSet.setDrawFilled(true);
        profileDataSet.setFillColor(Color.BLUE);

        pressureDataSet = new LineDataSet(new ArrayList<>(), "Pressure Data");
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

        // X축을 아래로 설정
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        viewModel.getProfileData().observe(this, new Observer<List<String[]>>() {
            @Override
            public void onChanged(List<String[]> data) {
                updateProfileChart(data);
            }
        });

        viewModel.getElapsedTime().observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long elapsedTime) {
                binding.elapsedTime.setText(formatElapsedTime(elapsedTime));
            }
        });

        List<String[]> profileData = loadProfileData();
        viewModel.setProfileData(profileData);

        binding.btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRunning = true;
                startTime = System.currentTimeMillis();
                isPaused = false;
                btnPauseResume.setText("Pause");

                pressureDataSet.clear();  // 새로운 런을 위해 압력 데이터셋 초기화
                chart.invalidate();

                // 압력 센서 데이터 리시버 등록
                LocalBroadcastManager.getInstance(RunActivity.this).registerReceiver(sensorValuesReceiver,
                        new IntentFilter("com.mcsl.hbotchamberapp.PRESSURE_UPDATE"));

                startElapsedTimeUpdate();


                // PID 제어 시작을 위해 PidService를 호출
                Intent pidIntent = new Intent(RunActivity.this, PidService.class);
                pidIntent.setAction("com.mcsl.hbotchamberapp.action.START_PID");
                startService(pidIntent);  // PidService 시작
            }
        });

        btnPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPaused) {
                    isPaused = false;
                    btnPauseResume.setText("Pause");
                    startTime = System.currentTimeMillis() - elapsedTimeWhenPaused;  // 재개 시 시간 초기화
                    startElapsedTimeUpdate();  // 경과 시간 업데이트 재개
                } else {
                    isPaused = true;
                    btnPauseResume.setText("Resume");
                    elapsedTimeWhenPaused = System.currentTimeMillis() - startTime;  // 일시 정지 시 경과 시간 저장
                    handler.removeCallbacks(elapsedTimeRunnable);  // 경과 시간 업데이트 중지
                }
            }
        });
    }

    private void updateProfileChart(List<String[]> data) {
        profileDataSet.clear();  // 기존 프로파일 데이터 지우기
        float currentTime = 0;
        for (String[] section : data) {
            float startPressure = Float.parseFloat(section[1]);
            float endPressure = Float.parseFloat(section[2]);
            float duration = Float.parseFloat(section[3]) * 60; // 분을 초로 변환
            profileDataSet.addEntry(new Entry(currentTime, startPressure));
            currentTime += duration;
            profileDataSet.addEntry(new Entry(currentTime, endPressure));

            // Y축의 최소값과 최대값 업데이트
            if (startPressure < minY) minY = startPressure;
            if (endPressure < minY) minY = endPressure;
            if (startPressure > maxY) maxY = startPressure;
            if (endPressure > maxY) maxY = endPressure;
        }

        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();

        // Y축의 범위를 고정
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMinimum(minY);
        yAxis.setAxisMaximum(maxY);
        chart.getAxisRight().setEnabled(false);  // 오른쪽 Y축 비활성화
    }

    private void updatePressureChart(double pressure) {
        if (isRunning && !isPaused) {
            float currentTime = (System.currentTimeMillis() - startTime) / 1000f;
            pressureDataSet.addEntry(new Entry(currentTime, (float) pressure));
            lineData.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();

            // Y축의 최소값과 최대값 업데이트
            if (pressure < minY) minY = (float) pressure;
            if (pressure > maxY) maxY = (float) pressure;
            YAxis yAxis = chart.getAxisLeft();
            yAxis.setAxisMinimum(minY);
            yAxis.setAxisMaximum(maxY);
        }
    }

    private void startElapsedTimeUpdate() {
        handler.post(elapsedTimeRunnable);  // 경과 시간 업데이트 시작
    }

    private void updateChamberPressure(double pressure) {
        String press = String.format(" %.2f ATA", pressure);
        binding.chamberPressure.setText(press);
    }

    private final Runnable elapsedTimeRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsedMillis = System.currentTimeMillis() - startTime;
            viewModel.setElapsedTime(elapsedMillis);

            // 다음 업데이트 예약
            handler.postDelayed(this, 1000);
        }
    };

    private String formatElapsedTime(long elapsedTime) {
        int seconds = (int) (elapsedTime / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private List<String[]> loadProfileData() {
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
            Type type = new TypeToken<List<String[]>>() {
            }.getType();
            profileData = gson.fromJson(sb.toString(), type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return profileData;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorValuesReceiver);
        } catch (IllegalArgumentException e) {
            // 리시버가 등록되지 않은 상태에서 해제하려고 할 때의 예외 처리
        }
    }
}
