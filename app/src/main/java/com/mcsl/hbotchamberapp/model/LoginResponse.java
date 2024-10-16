package com.mcsl.hbotchamberapp.model;

public class LoginResponse {
    private String access_token;  // JWT 토큰
    private String id;            // 사용자 ID
    private String username;      // 사용자 이름

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
