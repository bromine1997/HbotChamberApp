package com.mcsl.hbotchamberapp.network;

import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class SensorWebSocketListener extends WebSocketListener {
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.d("WebSocket", "Connected to server");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.d("WebSocket", "Message received: " + text);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(1000, null);
        Log.d("WebSocket", "Connection closed: " + reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.e("WebSocket", "Connection error", t);
    }
}
