package com.mcsl.hbotchamberapp.Activity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import com.mcsl.hbotchamberapp.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        //Focus 요청
        binding.editTextUsername.requestFocus();

        // 버튼 클릭 리스너 설정
        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // EditText에서 사용자 이름과 비밀번호 가져오기
                String username = binding.editTextUsername.getText().toString();
                String password = binding.editTextPassword.getText().toString();
                // 로그인 검증
                validateLogin(username, password);
            }
        });
    }

    // 로그인 검증 메서드
    private void validateLogin(String username, String password) {
        if (username.equals("admin") && password.equals("1234")) {
            // 로그인 성공
            Intent intent = new Intent(this, MenuActivity.class);
            startActivity(intent);
            finish(); // 로그인 액티비티를 스택에서 제거
        } else {
            // 로그인 실패
            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
        }
    }
}
