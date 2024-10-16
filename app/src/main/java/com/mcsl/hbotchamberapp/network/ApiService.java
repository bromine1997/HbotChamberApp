package com.mcsl.hbotchamberapp.network;

import com.mcsl.hbotchamberapp.model.LoginResponse;
import com.mcsl.hbotchamberapp.model.LoginRequest;
import com.mcsl.hbotchamberapp.model.ProfileRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    // 로그인 API
    @POST("auth/login")
    Call<LoginResponse> loginUser(@Body LoginRequest loginRequest);

    //profile SAVE
    @POST("profile/save")
    Call<Void> saveProfile(@Body ProfileRequest profileRequest);

}