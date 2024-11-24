package com.mcsl.hbotchamberapp.repository;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mcsl.hbotchamberapp.Service.PidService;
import com.mcsl.hbotchamberapp.model.PIDState;

public class PIDRepository {
    private static PIDRepository instance;
    private Context context;

    private MutableLiveData<Long> elapsedTime = new MutableLiveData<>();
    private MutableLiveData<Double> setPoint = new MutableLiveData<>();
    private MutableLiveData<String> pidPhase = new MutableLiveData<>();

    private MutableLiveData<String> sessionId = new MutableLiveData<>();


    private MutableLiveData<PIDState> pidState = new MutableLiveData<>(PIDState.STOPPED);   //PID 상태 관리 변수 Enum으로 관리


    private PIDRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized PIDRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PIDRepository(context);
        }
        return instance;
    }

    public LiveData<PIDState> getPidState() {
        return pidState;
    }

    public void setPidState(PIDState state) {
        pidState.postValue(state);
    }

    public LiveData<Long> getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long time) {
        elapsedTime.postValue(time);
    }

    public LiveData<Double> getSetPoint() {
        return setPoint;
    }

    public void setSetPoint(double setPointValue) {
        setPoint.postValue(setPointValue);
    }

    public LiveData<String> getPidPhase() {
        return pidPhase;
    }

    public void setPidPhase(String phase) {
        pidPhase.postValue(phase);
    }


    public LiveData<String> getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionIdValue) {
        sessionId.postValue(sessionIdValue);
    }


    private void sendPidControlBroadcast(String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void startPidControl() {
        Intent pidIntent = new Intent(context, PidService.class);
        pidIntent.setAction("com.mcsl.hbotchamberapp.action.START_PID");
        context.startService(pidIntent);
        setPidState(PIDState.STARTED); // PID 상태 업데이트

    }

    public void pausePidControl() {
        Intent pauseIntent = new Intent(context, PidService.class);
        pauseIntent.setAction("com.mcsl.hbotchamberapp.action.PAUSE_PID");
        context.startService(pauseIntent);
        setPidState(PIDState.PAUSED); // PID 상태 업데이트
        sendPidControlBroadcast("PID_CONTROL_PAUSED");
    }

    public void resumePidControl() {
        Intent resumeIntent = new Intent(context, PidService.class);
        resumeIntent.setAction("com.mcsl.hbotchamberapp.action.RESUME_PID");
        context.startService(resumeIntent);
        setPidState(PIDState.RUNNING); // PID 상태 업데이트
        // PID_CONTROL_RESUMED Broadcast 전송
        sendPidControlBroadcast("PID_CONTROL_RESUMED");
    }

    public void stopPidControl() {
        Intent stopIntent = new Intent(context, PidService.class);
        stopIntent.setAction("com.mcsl.hbotchamberapp.action.STOP_PID");
        context.startService(stopIntent);
        setPidState(PIDState.STOPPED); // PID 상태 업데이트


    }
}
