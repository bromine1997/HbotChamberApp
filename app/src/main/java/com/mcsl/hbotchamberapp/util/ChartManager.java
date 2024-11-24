package com.mcsl.hbotchamberapp.util;

import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.mcsl.hbotchamberapp.model.ProfileSection;

import java.util.List;

public class ChartManager {

    private LineChart chart;
    private LineData lineData;
    private LineDataSet profileDataSet;
    private LineDataSet pressureDataSet;

    public ChartManager(LineChart chart) {
        this.chart = chart;
        setupChart();
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
        yAxis.setAxisMaximum(4f);
        chart.getAxisRight().setEnabled(false);  // Disable right Y-axis
    }

    public void updateProfileChart(List<ProfileSection> data) {
        profileDataSet.clear();  // Clear existing profile data
        float currentTime = 0;

        for (ProfileSection section : data) {
            profileDataSet.addEntry(new Entry(currentTime, section.getStartPressure()));
            currentTime += section.getDuration();
            profileDataSet.addEntry(new Entry(currentTime, section.getEndPressure()));
        }

        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    public void updatePressureChart(double pressure, long elapsedTime) {
        float currentTime = elapsedTime / (1000f * 60f);  // 밀리초를 분으로 변환
        pressureDataSet.addEntry(new Entry(currentTime, (float) pressure));
        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    public void clearPressureData() {
        pressureDataSet.clear();
        chart.invalidate();
    }
}
