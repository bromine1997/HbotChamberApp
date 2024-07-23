package com.mcsl.hbotchamberapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.mcsl.hbotchamberapp.databinding.ActivityMenuBinding;

public class MenuActivity extends AppCompatActivity {

    private ActivityMenuBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMenuBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

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
                finish();
            }
        });
    }
}
