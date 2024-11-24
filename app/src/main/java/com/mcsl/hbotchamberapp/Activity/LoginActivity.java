package com.mcsl.hbotchamberapp.Activity;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mcsl.hbotchamberapp.BuildConfig;
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
import org.json.JSONObject;

import com.auth0.android.jwt.JWT;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private OkHttpClient client;

    private static final String LOGIN_URL = "http://" + BuildConfig.SERVER_ADDRESS+"/auth/login";
    //private static final String LOGIN_URL = "http://192.168.0.125:8080/auth/login"; // 서버 주소 설정
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

        // 로그인 버튼 클릭 리스너 설정
        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username = binding.editTextUsername.getText().toString();
                String password = binding.editTextPassword.getText().toString();

                // 입력 값 검증 (공백 체크)
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

    // 서버에 로그인 요청을 보내는 메서드
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
                    try {
                        // JSON 응답 파싱
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String accessToken = jsonResponse.getString("access_token");

                        // JWT 토큰 저장 및 사용자 정보 저장
                        saveJwtToken(accessToken);
                        saveUserInfoFromToken(accessToken);

                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                            // 로그인 성공 시 다음 액티비티로 이동
                            navigateToMenu();
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "응답 처리 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // 서버 오류 또는 응답 실패
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "서버 오류: " + response.message(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // JWT 토큰을 SharedPreferences에 저장하는 메서드
    private void saveJwtToken(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("jwt_token", token);
        editor.apply();
        Log.d("LoginActivity", "JWT 토큰 저장됨: " + token);
    }

    // JWT 토큰에서 사용자 ID와 이름을 추출하여 저장하는 메서드
    private void saveUserInfoFromToken(String jwtToken) {
        JWT jwt = new JWT(jwtToken);
        String userId = jwt.getClaim("sub").asString();  // 'sub' 클레임에서 userId 추출
        String username = jwt.getClaim("username").asString();  // 'username' 클레임에서 사용자명 추출

        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_id", userId);  // userId 저장
        editor.putString("username", username);  // 사용자명 저장
        editor.apply();
    }

    // JWT 토큰을 불러오는 메서드 (필요할 때 사용)
    private String getJwtToken() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("jwt_token", null);
    }

    private void navigateToMenu() {
        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
        finish();
    }
}
