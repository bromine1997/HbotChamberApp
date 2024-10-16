package com.mcsl.hbotchamberapp.network;

import android.util.Log;

import com.google.gson.Gson;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class SensorWebSocketListener extends WebSocketListener {

    private static final int NORMAL_CLOSURE_STATUS = 1000;

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
        // 웹소켓이 열리면 첫 연결 시 실행
        Log.d("WebSocket", "WebSocket Connection Opened");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        // 서버로부터 제어 명령 수신 시 실행
        Log.d("WebSocket", "Receiving : " + text);
        // 받은 메시지에 따라 챔버 제어 또는 다른 동작 수행
        handleControlCommand(text);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
        Log.e("WebSocket", "Error : " + t.getMessage());
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        Log.d("WebSocket", "Closing : " + code + " / " + reason);
    }

    private void handleControlCommand(String command) {
        // 서버로부터 받은 명령을 처리하는 로직 (예: start, stop 명령 등)
        if (command.equals("start")) {
            // PID 제어 시작 명령 처리
          //  startPidControl();
        } else if (command.equals("stop")) {
            // PID 제어 중지 명령 처리
            //stopPidControl();
        }
    }

}
