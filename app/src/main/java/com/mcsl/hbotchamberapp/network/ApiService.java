package com.mcsl.hbotchamberapp.network;

import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

public class ApiService {
    private static final String BASE_URL = "http://your-server-url/v1/sample-test/";

    // GET 요청: 샘플 데이터 조회
    public static void getSampleList(Callback callback) {
        OkHttpClient client = ApiClient.getClient();
        Request request = new Request.Builder()
                .url(BASE_URL + "sample")
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }

    // POST 요청: 샘플 데이터 추가
    public static void addSampleData(String _id, String name, int age, Callback callback) {
        OkHttpClient client = ApiClient.getClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("_id", _id);
            jsonObject.put("name", name);
            jsonObject.put("age", age);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "sample")
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    // PUT 요청: 샘플 데이터 수정
    public static void updateSampleData(String _id, String name, int age, Callback callback) {
        OkHttpClient client = ApiClient.getClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("_id", _id);
            jsonObject.put("name", name);
            jsonObject.put("age", age);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "sample")
                .put(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    // DELETE 요청: 샘플 데이터 삭제
    public static void deleteSampleData(String _id, Callback callback) {
        OkHttpClient client = ApiClient.getClient();
        HttpUrl url = HttpUrl.parse(BASE_URL + "sample").newBuilder()
                .addQueryParameter("_id", _id)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        client.newCall(request).enqueue(callback);
    }
}
