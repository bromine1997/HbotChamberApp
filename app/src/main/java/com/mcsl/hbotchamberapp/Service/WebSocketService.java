package com.mcsl.hbotchamberapp.Service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mcsl.hbotchamberapp.model.SensorData;
import com.mcsl.hbotchamberapp.model.SensorDataPacket;
import com.mcsl.hbotchamberapp.model.ServerCommand;
import com.mcsl.hbotchamberapp.model.PIDState;
import com.mcsl.hbotchamberapp.repository.PIDRepository;
import com.mcsl.hbotchamberapp.repository.SensorRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class WebSocketService extends Service {
    private static final String TAG = "WebSocketService";

    private ExecutorService executorService;

    private Socket mSocket;
    private SensorRepository sensorRepository;
    private PIDRepository pidRepository;
    private Gson gson;

    private boolean isConnected = false;

    // SensorData, elapsedTime, setPoint 값을 저장하기 위한 변수
    private boolean isObservingSensorData = false;
    private long elapsedTimeValue = 0L;
    private double setPointValue = 0.0;

    private String sessionId = null;
    private String userId = null; // userId 필드 추가

    @Override
    public void onCreate() {
        super.onCreate();

        sensorRepository = SensorRepository.getInstance(this);
        pidRepository = PIDRepository.getInstance(this);
        gson = new Gson();

        // PIDRepository에서 sessionId를 관찰
        pidRepository.getSessionId().observeForever(sessionIdObserver);

        userId = getUserId();

        Log.d(TAG, "웹소켓 연결 시도한다~: ");

        // Socket.IO 클라이언트 초기화
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.reconnectionDelay = 1000;
            options.reconnectionDelayMax = 5000;

            mSocket = IO.socket("http://192.168.0.125:8080", options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Socket.IO 이벤트 리스너 설정
        if (mSocket != null) {
            mSocket.on(Socket.EVENT_CONNECT, onConnect);
            mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);

            mSocket.on("server_command", onServerCommand); // 서버에서 보낸 명령 처리
            mSocket.connect();
        }

        // BroadcastReceiver 등록
        IntentFilter filter = new IntentFilter();
        filter.addAction("PID_CONTROL_STARTED");
        filter.addAction("PID_CONTROL_STOPPED");
        filter.addAction("PID_CONTROL_PAUSED");
        filter.addAction("PID_CONTROL_RESUMED");
        LocalBroadcastManager.getInstance(this).registerReceiver(pidControlReceiver, filter);

        executorService = Executors.newSingleThreadExecutor();
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            isConnected = true;
            Log.d(TAG, "Socket.IO Connected");
            // 연결 상태 Broadcast 전송 (필요하다면)
            sendConnectionStatusBroadcast(true);
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            isConnected = false;
            Log.d(TAG, "Socket.IO Disconnected");
            // 연결 상태 Broadcast 전송 (필요하다면)
            sendConnectionStatusBroadcast(false);
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            isConnected = false;
            Log.e(TAG, "Socket.IO Connection Error");
            // 연결 상태 Broadcast 전송 (필요하다면)
            sendConnectionStatusBroadcast(false);
        }
    };

    private Emitter.Listener onServerCommand = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (args.length > 0) {
                String message = args[0].toString();
                handleServerMessage(message);
            }
        }
    };

    private void sendConnectionStatusBroadcast(boolean isConnected) {
        Intent intent = new Intent("WEBSOCKET_CONNECTION_STATUS");
        intent.putExtra("isConnected", isConnected);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private final BroadcastReceiver pidControlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received broadcast: " + action);
            if ("PID_CONTROL_STARTED".equals(action)) {
                startSendingSensorData();
                pidRepository.setPidState(PIDState.STARTED);
            } else if ("PID_CONTROL_PAUSED".equals(action)) {
                stopSendingSensorData();
                pidRepository.setPidState(PIDState.PAUSED);
            } else if ("PID_CONTROL_RESUMED".equals(action)) {
                startSendingSensorData();
                pidRepository.setPidState(PIDState.RUNNING);
            } else if ("PID_CONTROL_STOPPED".equals(action)) {
                stopSendingSensorData();
                pidRepository.setPidState(PIDState.STOPPED);
            }
        }
    };

    private void startSendingSensorData() {
        if (!isObservingSensorData) {
            isObservingSensorData = true;
            sensorRepository.getSensorData().observeForever(sensorDataObserver);
            pidRepository.getElapsedTime().observeForever(elapsedTimeObserver);
            pidRepository.getSetPoint().observeForever(setPointObserver);
        }
    }

    private void stopSendingSensorData() {
        if (isObservingSensorData) {
            isObservingSensorData = false;
            sensorRepository.getSensorData().removeObserver(sensorDataObserver);
            pidRepository.getElapsedTime().removeObserver(elapsedTimeObserver);
            pidRepository.getSetPoint().removeObserver(setPointObserver);
        }
    }

    // sensorDataObserver 수정
    private final Observer<SensorData> sensorDataObserver = new Observer<SensorData>() {
        @Override
        public void onChanged(SensorData data) {
            if (data != null && sessionId != null && (pidRepository.getPidState().getValue() == PIDState.STARTED || pidRepository.getPidState().getValue() == PIDState.RUNNING)) { // STARTED 또는 RUNNING 상태일 때만 전송
                executorService.execute(() -> {
                    String deviceId = "chamber 1"; // deviceId는 하드코딩 또는 설정값 사용
                    SensorDataPacket packet = new SensorDataPacket(deviceId, sessionId, userId, data, elapsedTimeValue, setPointValue);
                    if (isConnected) {
                        try {
                            JSONObject jsonData = new JSONObject(gson.toJson(packet));
                            mSocket.emit("sensor_data", jsonData);
                            Log.d(TAG, "onChanged: 데이터 전송");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        saveSensorDataLocally(packet);
                    }
                });
            }
        }
    };

    private final Observer<Long> elapsedTimeObserver = new Observer<Long>() {
        @Override
        public void onChanged(Long value) {
            elapsedTimeValue = (value != null) ? value : 0L;
        }
    };

    private final Observer<Double> setPointObserver = new Observer<Double>() {
        @Override
        public void onChanged(Double value) {
            setPointValue = (value != null) ? value : 0.0;
        }
    };

    private void handleServerMessage(String message) {
        try {
            ServerCommand command = gson.fromJson(message, ServerCommand.class);
            if (command != null) {
                Log.d(TAG, "Received server command: " + command.getAction());
                switch (command.getAction()) {
                    case "START":
                        pidRepository.startPidControl(); // START
                        break;
                    case "PAUSE":
                        pidRepository.pausePidControl(); // PAUSE
                        break;
                    case "RESUME":
                        pidRepository.resumePidControl(); // RESUME
                        break;
                    case "STOP":
                        pidRepository.stopPidControl(); // STOP
                        break;
                    default:
                        Log.d(TAG, "Unknown command received: " + command.getAction());
                        break;
                }
            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    private void saveSensorDataLocally(SensorDataPacket packet) {
        // 로컬에 SensorDataPacket을 저장하는 로직 구현
        // 파일로 저장하거나 로컬 DB에 저장
        try {
            File file = new File(getFilesDir(), "sensor_data_packet.json");
            FileWriter writer = new FileWriter(file, true);
            String jsonData = gson.toJson(packet);
            writer.append(jsonData).append("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Observer<String> sessionIdObserver = new Observer<String>() {
        @Override
        public void onChanged(String value) {
            sessionId = value;
        }
    };

    private String getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("user_id", null);  // 저장된 userId 반환
    }

    private String getUsername() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("username", null);  // 저장된 사용자 이름 반환
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
        stopSendingSensorData();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pidControlReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 바인드 서비스를 사용하지 않으므로 null 반환
        return null;
    }
}
