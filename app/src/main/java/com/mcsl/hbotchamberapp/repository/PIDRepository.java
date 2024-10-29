package com.mcsl.hbotchamberapp.repository;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mcsl.hbotchamberapp.Service.PidService;

public class PIDRepository {
    private static PIDRepository instance;
    private Context context;

    private MutableLiveData<Long> elapsedTime = new MutableLiveData<>();
    private MutableLiveData<Double> setPoint = new MutableLiveData<>();
    private MutableLiveData<String> pidPhase = new MutableLiveData<>();

    private PIDRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized PIDRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PIDRepository(context);
        }
        return instance;
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

    public void startPidControl() {
        Intent pidIntent = new Intent(context, PidService.class);
        pidIntent.setAction("com.mcsl.hbotchamberapp.action.START_PID");
        context.startService(pidIntent);
    }

    public void pausePidControl() {
        Intent pauseIntent = new Intent(context, PidService.class);
        pauseIntent.setAction("com.mcsl.hbotchamberapp.action.PAUSE_PID");
        context.startService(pauseIntent);
    }

    public void resumePidControl() {
        Intent resumeIntent = new Intent(context, PidService.class);
        resumeIntent.setAction("com.mcsl.hbotchamberapp.action.RESUME_PID");
        context.startService(resumeIntent);
    }

    public void stopPidControl() {
        Intent stopIntent = new Intent(context, PidService.class);
        stopIntent.setAction("com.mcsl.hbotchamberapp.action.STOP_PID");
        context.startService(stopIntent);
    }
}
