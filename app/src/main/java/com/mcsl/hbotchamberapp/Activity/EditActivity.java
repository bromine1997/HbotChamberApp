package com.mcsl.hbotchamberapp.Activity;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.widget.Toast;

import com.mcsl.hbotchamberapp.R;
import com.mcsl.hbotchamberapp.databinding.ActivityEditBinding;

public class EditActivity extends AppCompatActivity {
    private Runnable runnableCode;
    private LineChart chart;
    private Handler handler = new Handler(Looper.getMainLooper());

    private List<String[]> currentProfile = new ArrayList<>();
    private int currentSectionIndex = 0;

    private ActivityEditBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        initializeUIComponents();
        initializeChart();
        initializetableChart();

        runnableCode = () -> {
            // 여기에 주기적으로 실행할 코드 .edit 화면에서는 주기적으로 실행할 코드가 없으므로 필요 없음.
        };
    }

    private void initializetableChart() {
        TableLayout tableChart = binding.Table;

        // 헤더 행 추가
        TableRow headerRow = new TableRow(this);
        headerRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        String[] headers = {"#", "Start P", "End P", "Time(min)"};
        for (String header : headers) {
            TextView textView = new TextView(this);
            textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
            textView.setPadding(5, 5, 5, 5);
            textView.setText(header);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
            textView.setTypeface(null, Typeface.BOLD);

            headerRow.addView(textView);
        }

        tableChart.addView(headerRow);

        // 초기 프로파일 데이터 추가
        updateTable(currentProfile);
    }

    private void initializeUIComponents() {
        binding.buttonIncreaseSections.setOnClickListener(v -> {
            int numSections = Integer.parseInt(binding.valueNumberOfSections.getText().toString());
            numSections++;
            binding.valueNumberOfSections.setText(String.valueOf(numSections));
            addSection();
        });

        binding.buttonDecreaseSections.setOnClickListener(v -> {
            int numSections = Integer.parseInt(binding.valueNumberOfSections.getText().toString());
            if (numSections > 1) {
                numSections--;
                binding.valueNumberOfSections.setText(String.valueOf(numSections));
                removeSection();
            }
        });

        binding.buttonDecreaseControlSection.setOnClickListener(v -> {
            try {
                int section = Integer.parseInt(binding.valueControlSection.getText().toString());
                if (section > 1) {
                    section -= 1;
                    binding.valueControlSection.setText(String.valueOf(section));
                    currentSectionIndex = section - 1;
                    updateUIWithCurrentSection();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        });

        binding.buttonIncreaseControlSection.setOnClickListener(v -> {
            try {
                int section = Integer.parseInt(binding.valueControlSection.getText().toString());
                if (section < currentProfile.size()) {
                    section += 1;
                    binding.valueControlSection.setText(String.valueOf(section));
                    currentSectionIndex = section - 1;
                    updateUIWithCurrentSection();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        });

        binding.buttonDecreaseEndPressure.setOnClickListener(v -> {
            float currentValue = Float.parseFloat(binding.valueEndPressure.getText().toString());
            if (currentValue > 1) {
                currentValue -= 0.1;
                binding.valueEndPressure.setText(String.format(Locale.US, "%.1f", currentValue));
                updateCurrentSectionData();
            } else {
                currentValue = 1;
                binding.valueEndPressure.setText(String.format(Locale.US, "%.1f", currentValue));
            }
        });

        binding.buttonIncreaseEndPressure.setOnClickListener(v -> {
            float currentValue = Float.parseFloat(binding.valueEndPressure.getText().toString());
            currentValue += 0.1;
            binding.valueEndPressure.setText(String.format(Locale.US, "%.1f", currentValue));
            updateCurrentSectionData();
        });

        binding.buttonDecreaseTime.setOnClickListener(v -> {
            float currentValue = Float.parseFloat(binding.valueTime.getText().toString());
            currentValue -= 1;
            binding.valueTime.setText(String.format(Locale.US, "%.1f", currentValue));
            updateCurrentSectionData();
        });

        binding.buttonIncreaseTime.setOnClickListener(v -> {
            float currentValue = Float.parseFloat(binding.valueTime.getText().toString());
            currentValue += 1;
            binding.valueTime.setText(String.format(Locale.US, "%.1f", currentValue));
            updateCurrentSectionData();
        });

        binding.buttonDecreaseFlow.setOnClickListener(v -> {
            float currentValue = Float.parseFloat(binding.valueFlow.getText().toString());
            currentValue -= 1;
            binding.valueFlow.setText(String.format(Locale.US, "%.1f", currentValue));
        });

        binding.buttonIncreaseFlow.setOnClickListener(v -> {
            float currentValue = Float.parseFloat(binding.valueFlow.getText().toString());
            currentValue += 1;
            binding.valueFlow.setText(String.format(Locale.US, "%.1f", currentValue));
        });

        binding.btnNew.setOnClickListener(v -> {
            List<String[]> newProfile = new ArrayList<>();
            newProfile.add(new String[]{"1", "1.0", "1.0", "5"});
            updateTable(newProfile);
            currentProfile = newProfile;
            currentSectionIndex = 0;
            updateUIWithCurrentSection();
            binding.valueNumberOfSections.setText("1");
        });

        binding.btnCurve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyCurve();
            }
        });

        binding.btnOpen.setOnClickListener(v -> {
            loadProfileDataFromFile();
            updateChart();
            updateTable(currentProfile);
            updateNumberOfSections();

            if (!currentProfile.isEmpty()) {
                currentSectionIndex = 0;
                binding.valueControlSection.setText(String.valueOf(currentSectionIndex + 1));
                updateUIWithCurrentSection();
            } else {
                binding.valueControlSection.setText("0");
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            saveProfileData();
            Toast.makeText(EditActivity.this, "저장되었습니다", Toast.LENGTH_SHORT).show();
        });

        binding.btnExit.setOnClickListener(v -> finish());
    }

    private void updateTable(List<String[]> profileData) {
        TableLayout tableChart = binding.Table;

        if (tableChart.getChildCount() > 1) {
            tableChart.removeViews(1, tableChart.getChildCount() - 1);
        }

        int totalDuration = 0;

        for (int i = 0; i < profileData.size(); i++) {
            String[] dataRow = profileData.get(i);
            TableRow tableRow = new TableRow(this);
            tableRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            for (String cellData : dataRow) {
                TextView textView = new TextView(this);
                textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                textView.setPadding(5, 5, 5, 5);
                textView.setText(cellData);
                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                textView.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
                textView.setTextColor(Color.BLACK);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);

                tableRow.addView(textView);
            }

            tableChart.addView(tableRow);

            try {
                totalDuration += Integer.parseInt(dataRow[3]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        TableRow totalRow = new TableRow(this);
        totalRow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView totalLabelTextView = new TextView(this);
        totalLabelTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        totalLabelTextView.setPadding(5, 5, 5, 5);
        totalLabelTextView.setText("Total");
        totalLabelTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        totalLabelTextView.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
        totalLabelTextView.setTextColor(Color.BLACK);
        totalLabelTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        totalLabelTextView.setTypeface(null, Typeface.BOLD);
        totalRow.addView(totalLabelTextView);

        for (int i = 0; i < 2; i++) {
            TextView emptyTextView = new TextView(this);
            emptyTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
            emptyTextView.setPadding(5, 5, 5, 5);
            emptyTextView.setText("");
            emptyTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyTextView.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
            emptyTextView.setTextColor(Color.BLACK);
            emptyTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
            totalRow.addView(emptyTextView);
        }

        TextView totalValueTextView = new TextView(this);
        totalValueTextView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        totalValueTextView.setPadding(5, 5, 5, 5);
        totalValueTextView.setText(String.valueOf(totalDuration));
        totalValueTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        totalValueTextView.setBackground(ContextCompat.getDrawable(this, R.drawable.border));
        totalValueTextView.setTextColor(Color.BLACK);
        totalValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        totalRow.addView(totalValueTextView);

        tableChart.addView(totalRow);
    }

    private void addSection() {
        currentProfile.add(new String[]{String.valueOf(currentProfile.size() + 1), "1.0", "1.0", "5"});
        updateTable(currentProfile);
    }

    private void removeSection() {
        if (!currentProfile.isEmpty()) {
            currentProfile.remove(currentProfile.size() - 1);
        }
        updateTable(currentProfile);
    }

    private void applyCurve() {
        double factor = 1.10;
        for (String[] section : currentProfile) {
            double startPressure = Double.parseDouble(section[1]);
            double endPressure = Double.parseDouble(section[2]);

            startPressure *= factor;
            endPressure *= factor;

            section[1] = String.format(Locale.US, "%.2f", startPressure);
            section[2] = String.format(Locale.US, "%.2f", endPressure);
        }

        updateTable(currentProfile);
        updateChart();
    }

    private void updateUIWithCurrentSection() {
        if (currentProfile != null && !currentProfile.isEmpty()) {
            String[] currentSection = currentProfile.get(currentSectionIndex);

            binding.valueEndPressure.setText(currentSection[2]);
            binding.valueTime.setText(currentSection[3]);
            binding.valueFlow.setText("Flow value here");
            binding.valueControlSection.setText(String.valueOf(currentSectionIndex + 1));
        }
    }

    private void updateCurrentSectionData() {
        String[] currentSection = currentProfile.get(currentSectionIndex);
        currentSection[2] = binding.valueEndPressure.getText().toString();
        currentSection[3] = binding.valueTime.getText().toString();

        if (currentSectionIndex > 0) {
            String[] previousSection = currentProfile.get(currentSectionIndex - 1);
            currentSection[1] = previousSection[2];
        }

        if (currentSectionIndex < currentProfile.size() - 1) {
            String[] nextSection = currentProfile.get(currentSectionIndex + 1);
            nextSection[1] = currentSection[2];
        }

        updateTable(currentProfile);
        updateChart();
    }

    private void initializeChart() {
        chart = binding.chart;

        List<Entry> entries = new ArrayList<>();
        PressureTimeChart pressureTimeChart = new PressureTimeChart(chart, currentProfile);
        pressureTimeChart.drawChart();
    }

    private void updateChart() {
        List<Entry> entries = new ArrayList<>();
        float currentTime = 0;

        for (String[] dataPoint : currentProfile) {
            float duration = Float.parseFloat(dataPoint[3]);
            float startPressure = Float.parseFloat(dataPoint[1]);
            float endPressure = Float.parseFloat(dataPoint[2]);

            entries.add(new Entry(currentTime, startPressure));
            currentTime += duration;
            entries.add(new Entry(currentTime, endPressure));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Pressure Over Time");
        dataSet.setColor(Color.BLACK);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(3f);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }

    public class PressureTimeChart {
        private LineChart chart;
        private List<String[]> data;

        public PressureTimeChart(LineChart chart, List<String[]> data) {
            this.chart = chart;
            this.data = data;
        }

        public void drawChart() {
            List<Entry> entries = new ArrayList<>();
            float currentTime = 0;

            for (String[] dataPoint : data) {
                float duration = Float.parseFloat(dataPoint[3]);
                float startPressure = Float.parseFloat(dataPoint[1]);
                float endPressure = Float.parseFloat(dataPoint[2]);

                entries.add(new Entry(currentTime, startPressure));
                currentTime += duration;
                entries.add(new Entry(currentTime, endPressure));
            }

            LineDataSet dataSet = new LineDataSet(entries, "Pressure Over Time");
            dataSet.setColor(Color.BLACK);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setLineWidth(3f);

            configureChartAppearance();

            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
            chart.invalidate();
        }
    }

    private void configureChartAppearance() {
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        YAxis leftAxis = chart.getAxisLeft();
        YAxis rightAxis = chart.getAxisRight();
        leftAxis.setSpaceTop(100f);
        leftAxis.setGranularity(0.25f);
        xAxis.setSpaceMin(2f);
        xAxis.setSpaceMax(10f);

        rightAxis.setEnabled(false);
    }

    private void saveProfileData() {
        Gson gson = new Gson();
        String json = gson.toJson(currentProfile);
        try (FileOutputStream fos = openFileOutput("profile_data.json", Context.MODE_PRIVATE)) {
            fos.write(json.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadProfileDataFromFile() {
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
            currentProfile = gson.fromJson(sb.toString(), type);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNumberOfSections() {
        if (currentProfile != null) {
            int numSections = currentProfile.size();
            binding.valueNumberOfSections.setText(String.valueOf(numSections));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.postDelayed(runnableCode, 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(runnableCode);
    }
}