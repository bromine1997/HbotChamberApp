package com.mcsl.hbotchamberapp.network;


import okhttp3.OkHttpClient;

public class ApiClient {
    private static OkHttpClient client;

    public static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    // 필요에 따라 타임아웃 등 설정 추가
                    .build();
        }
        return client;
    }
}