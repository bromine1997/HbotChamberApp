package com.mcsl.hbotchamberapp.network;


import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketManager {
    private static final String TAG = "WebSocketManager";
    private WebSocket webSocket;
    private OkHttpClient client;
    private String serverUrl;
    private WebSocketListener listener;

    public WebSocketManager(String serverUrl, WebSocketListener listener) {
        this.serverUrl = serverUrl;
        this.listener = listener;
        client = new OkHttpClient();
    }

    public void connect() {
        Request request = new Request.Builder().url(serverUrl).build();
        webSocket = client.newWebSocket(request, listener);
    }

    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        } else {
            Log.e(TAG, "WebSocket is not connected");
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing WebSocket");
        }
    }
}

