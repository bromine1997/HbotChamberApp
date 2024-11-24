package com.mcsl.hbotchamberapp.Activity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.mcsl.hbotchamberapp.Service.GpioService;
import com.mcsl.hbotchamberapp.Service.SensorService;
import com.mcsl.hbotchamberapp.Service.ValveService;
import com.mcsl.hbotchamberapp.Service.WebSocketService;
import com.mcsl.hbotchamberapp.databinding.ActivityMenuBinding;

public class MenuActivity extends AppCompatActivity {

    private ActivityMenuBinding binding;
    private Intent GpioServiceIntent;

    private Intent SensorServiceIntent;

    private Intent ValveServiceIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMenuBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // GPIOService 시작
        GpioServiceIntent = new Intent(this, GpioService.class);
        startService(GpioServiceIntent);
        // SensorService 시작
        SensorServiceIntent = new Intent(this, SensorService.class);
        startService(SensorServiceIntent);
        // ValveService 시작
        ValveServiceIntent = new Intent(this, ValveService.class);
        startService(ValveServiceIntent);

        // WebSocketService 시작
        Intent intent = new Intent(this, WebSocketService.class);
        startService(intent);


        // 버튼 클릭 리스너 설정
        binding.runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // RunActivity로 이동
                Intent intent = new Intent(MenuActivity.this, RunActivity.class);
                startActivity(intent);
            }
        });

        binding.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // EditActivity로 이동
                Intent intent = new Intent(MenuActivity.this, EditActivity.class);
                startActivity(intent);
            }
        });

        binding.ioportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // IoPortActivity로 이동
                Intent intent = new Intent(MenuActivity.this, IoPortActivity.class);
                startActivity(intent);
            }
        });

        binding.exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 앱 종료
                stopService(GpioServiceIntent);
                stopService(SensorServiceIntent);
                stopService(ValveServiceIntent);
                finish();
            }
        });


    }

    protected void onDestroy() {
        super.onDestroy();
        // GPIOService 종료
        stopService(GpioServiceIntent);
        stopService(SensorServiceIntent);
        stopService(ValveServiceIntent);
    };
}
