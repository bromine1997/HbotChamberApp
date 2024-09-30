package com.mcsl.hbotchamberapp.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.mcsl.hbotchamberapp.databinding.ActivityLoginBinding;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private OkHttpClient client;
    private static final String LOGIN_URL = "http://your-server-url/login"; // 서버의 로그인 엔드포인트로 변경하세요
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        client = new OkHttpClient();

        // Focus 요청
        binding.editTextUsername.requestFocus();

        // 버튼 클릭 리스너 설정
        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username = binding.editTextUsername.getText().toString();
                String password = binding.editTextPassword.getText().toString();

                // 입력 값 검증 (예: 공백 체크)
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "아이디와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 로그인 검증
                validateLogin(username, password);
            }
        });
    }

    // 로그인 검증 메서드
    private void validateLogin(String username, String password) {
        if (username.equals("admin") && password.equals("1234")) {
            // 하드코딩된 관리자 계정 검증
            navigateToMenu();
        } else {
            // 서버로 로그인 요청 보내기
            try {
                sendLoginRequest(username, password);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "요청 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendLoginRequest(String username, String password) throws Exception {
        // JSON 형식의 요청 바디 생성
        String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(body)
                .build();

        // 비동기 요청
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                // 네트워크 오류 등으로 요청 실패
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "네트워크 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                // 서버로부터 응답을 받았을 때 처리
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    // 응답 처리 (예: JSON 파싱)
                    // 서버에서 {"success": true, "message": "Login successful"} 형태로 응답한다고 가정
                    boolean success = false;
                    String message = "로그인 실패";

                    // 간단한 방법으로 응답 처리 (JSON 파서 사용 권장)
                    if (responseBody.contains("\"success\":true")) {
                        success = true;
                        message = "로그인 성공";
                    } else if (responseBody.contains("\"message\"")) {
                        int startIndex = responseBody.indexOf("\"message\":\"") + 10;
                        int endIndex = responseBody.indexOf("\"", startIndex);
                        message = responseBody.substring(startIndex, endIndex);
                    }

                    String finalMessage = message;
                    boolean finalSuccess = success;

                    runOnUiThread(() -> {
                        if (finalSuccess) {
                            // 로그인 성공 시 다음 액티비티로 이동
                            navigateToMenu();
                        } else {
                            // 로그인 실패
                            Toast.makeText(LoginActivity.this, finalMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // 서버 오류 또는 응답 실패
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "서버 오류: " + response.message(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void navigateToMenu() {
        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
        finish();
    }
}
