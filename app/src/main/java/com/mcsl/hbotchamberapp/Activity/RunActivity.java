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
    private int currentSection = 0;
    private LineChart chart;
    private LineData lineData;
    private LineDataSet profileDataSet;
    private LineDataSet updateDataSet;
    private float minY = Float.MAX_VALUE;
    private float maxY = Float.MIN_VALUE;
    private long startTime;
    private long elapsedTimeWhenPaused = 0;
    private ActivityRunBinding binding;
    private RunViewModel viewModel;

    private boolean isPaused = false;
    private boolean isRunning = false;

    private Button btnPauseResume;
    private boolean isGasAnalyzerDialogOpen = false; // 플래그 변수 추가

    private BroadcastReceiver sensorValuesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isGasAnalyzerDialogOpen) { // 팝업 창이 열려있지 않을 때만 처리
                if ("com.mcsl.hbotchamberapp.ADC_VALUES".equals(intent.getAction())) {
                    int[] adcValues = intent.getIntArrayExtra("adcValues");
                    if (adcValues != null) {
                        showGasAnalyzerDialog(adcValues, -1); // CO2 값이 아직 없는 경우
                    }
                } else if ("com.mcsl.hbotchamberapp.CO2_UPDATE".equals(intent.getAction())) {
                    int co2Ppm = intent.getIntExtra("co2Ppm", -1);
                    showGasAnalyzerDialog(null, co2Ppm); // ADC 값이 없는 경우
                }
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
        profileDataSet.setLineWidth(2.5f);
        profileDataSet.setDrawCircles(true);
        profileDataSet.setDrawValues(false);
        profileDataSet.setDrawFilled(true);
        profileDataSet.setFillColor(Color.BLUE);

        updateDataSet = new LineDataSet(new ArrayList<>(), "Update Data");
        updateDataSet.setColor(Color.RED);
        updateDataSet.setLineWidth(4f);
        updateDataSet.setDrawCircles(true);
        updateDataSet.setDrawValues(false);
        updateDataSet.setDrawFilled(true);
        updateDataSet.setFillColor(Color.RED);

        lineData = new LineData();
        lineData.addDataSet(profileDataSet);
        lineData.addDataSet(updateDataSet);
        chart.setData(lineData);

        // X축을 아래로 설정
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        viewModel.getProfileData().observe(this, new Observer<List<String[]>>() {
            @Override
            public void onChanged(List<String[]> data) {
                updateProfileChart(data);
                updateUI(data);
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

        binding.btnGasAnalyzer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalBroadcastManager.getInstance(RunActivity.this).registerReceiver(sensorValuesReceiver,
                        new IntentFilter("com.mcsl.hbotchamberapp.ADC_VALUES"));
                LocalBroadcastManager.getInstance(RunActivity.this).registerReceiver(sensorValuesReceiver,
                        new IntentFilter("com.mcsl.hbotchamberapp.CO2_UPDATE"));
            }
        });

        binding.btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSection = 0;  // 섹션을 처음부터 다시 시작
                startTime = System.currentTimeMillis();  // 시작 시간 초기화
                isPaused = false;  // Pause 상태 해제
                btnPauseResume.setText("Pause");  // 버튼 텍스트 초기화
                updateDataSet.clear();  // 업데이트 데이터셋 초기화
                chart.invalidate();  // 그래프 초기화
                List<String[]> latestProfileData = viewModel.getProfileData().getValue();  // 최신 프로파일 데이터 가져오기
                if (latestProfileData != null) {
                    runGraphUpdate(latestProfileData);
                }
                startElapsedTimeUpdate();  // 경과 시간 업데이트 시작
            }
        });

        btnPauseResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPaused) {
                    isPaused = false;
                    btnPauseResume.setText("Pause");
                    startTime = System.currentTimeMillis() - elapsedTimeWhenPaused;  // 재개 시 시간 초기화
                    handler.post(runnable);  // 그래프 업데이트 재개
                    startElapsedTimeUpdate();  // 경과 시간 업데이트 재개
                } else {
                    isPaused = true;
                    btnPauseResume.setText("Resume");
                    elapsedTimeWhenPaused = System.currentTimeMillis() - startTime;  // 일시 정지 시 경과 시간 저장
                    handler.removeCallbacks(runnable);  // 그래프 업데이트 중지
                    handler.removeCallbacks(elapsedTimeRunnable);  // 경과 시간 업데이트 중지
                }
            }
        });
    }

    private void showGasAnalyzerDialog(int[] adcValues, int co2Ppm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Gas Analyzer");

        // adcValues와 co2Ppm을 문자열로 변환하여 표시
        StringBuilder message = new StringBuilder();
        if (adcValues != null) {
            for (int value : adcValues) {
                message.append("ADC Value: ").append(value).append("\n");
            }
        }
        if (co2Ppm != -1) {
            message.append("CO2 Value: ").append(co2Ppm).append(" ppm\n");
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocalBroadcastManager.getInstance(RunActivity.this).unregisterReceiver(sensorValuesReceiver);
                isGasAnalyzerDialogOpen = false; // 팝업 창이 닫히면 플래그 업데이트
            }
        });
        builder.show();
        isGasAnalyzerDialogOpen = true; // 팝업 창이 열리면 플래그 업데이트
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

    private void updateUI(List<String[]> data) {
        if (!data.isEmpty()) {
            binding.chamberPressure.setText(data.get(0)[2] + " ATA"); // 첫 섹션의 종료 압력
        }
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (currentSection < viewModel.getProfileData().getValue().size()) {
                runGraphUpdate(viewModel.getProfileData().getValue());
            }
        }
    };

    private void runGraphUpdate(List<String[]> profileData) {
        if (currentSection < profileData.size()) {
            String[] section = profileData.get(currentSection);
            final float startPressure = Float.parseFloat(section[1]);
            final float endPressure = Float.parseFloat(section[2]);
            final float duration = Float.parseFloat(section[3]);

            final float sectionDurationInSeconds = duration * 60;
            final float interval = 1.0f;
            final float totalSteps = sectionDurationInSeconds / interval;
            final float pressureStep = (endPressure - startPressure) / totalSteps;

            final float initialCurrentTime = updateDataSet.getEntryCount() == 0 ? 0 : updateDataSet.getEntryForIndex(updateDataSet.getEntryCount() - 1).getX();
            final float initialCurrentPressure = updateDataSet.getEntryCount() == 0 ? startPressure : updateDataSet.getEntryForIndex(updateDataSet.getEntryCount() - 1).getY();
            final int initialStep = 0;

            final List<String[]> finalProfileData = profileData; // effectively final 로 사용하기 위해

            handler.postDelayed(new Runnable() {
                float currentTime = initialCurrentTime;
                float currentPressure = initialCurrentPressure;
                int step = initialStep;

                @Override
                public void run() {
                    if (!isPaused) {
                        if (step <= totalSteps) {
                            updateDataSet.addEntry(new Entry(currentTime, currentPressure));
                            lineData.notifyDataChanged();
                            chart.notifyDataSetChanged();
                            chart.invalidate();

                            currentTime += interval;
                            currentPressure += pressureStep;
                            step++;

                            handler.postDelayed(this, (long) (interval * 1000));
                        } else {
                            currentSection++;
                            runGraphUpdate(finalProfileData);
                        }
                    }
                }
            }, 0);
        } else {
            // 모든 섹션이 완료되었을 때 알림 창 띄우기
            showCompletionDialog();
        }
    }

    private void showCompletionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Complete");
        builder.setMessage("The run time has reached the total profile time.");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private final Runnable elapsedTimeRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsedMillis = System.currentTimeMillis() - startTime;
            viewModel.setElapsedTime(elapsedMillis);

            // 총 런타임과 저장된 프로파일의 총 시간이 같으면 알림 창 띄우기
            long totalProfileTime = getTotalProfileTime();
            if (elapsedMillis >= totalProfileTime * 60 * 1000) {
                showCompletionDialog();
                return;
            }

            // 다음 업데이트 예약
            handler.postDelayed(this, 1000);
        }
    };

    private long getTotalProfileTime() {
        List<String[]> profileData = viewModel.getProfileData().getValue();
        long totalTime = 0;
        if (profileData != null) {
            for (String[] section : profileData) {
                totalTime += Long.parseLong(section[3]);
            }
        }
        return totalTime;
    }

    private void startElapsedTimeUpdate() {
        handler.post(elapsedTimeRunnable);  // 경과 시간 업데이트 시작
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
        binding = null;  // 뷰 바인딩 해제
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(sensorValuesReceiver);
        } catch (IllegalArgumentException e) {
            // 리시버가 등록되지 않은 상태에서 해제하려고 할 때의 예외 처리
        }
    }
}
