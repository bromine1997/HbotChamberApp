package com.mcsl.hbotchamberapp.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.mcsl.hbotchamberapp.BuildConfig;

public class ApiClient {

    private static Retrofit retrofit = null;

    // 서버의 URL을 여기에 설정 (예: 로컬 서버, 클라우드 서버 등)
    private static final String BASE_URL = "http://" + BuildConfig.SERVER_ADDRESS;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
