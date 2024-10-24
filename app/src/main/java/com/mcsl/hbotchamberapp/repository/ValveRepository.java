package com.mcsl.hbotchamberapp.repository;



import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.mcsl.hbotchamberapp.Service.ValveService;

public class ValveRepository {
    private static ValveRepository instance;
    private ValveService valveService;
    private boolean isServiceBound = false;
    private Context context;

    private ValveRepository(Context context) {
        this.context = context.getApplicationContext();
        bindValveService();
    }

    public static synchronized ValveRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ValveRepository(context);
        }
        return instance;
    }

    private void bindValveService() {
        Intent intent = new Intent(context, ValveService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ValveService.LocalBinder binder = (ValveService.LocalBinder) service;
            valveService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    // 밸브 제어 메소드들

    public void solPressOn() {
        if (isServiceBound) {
            valveService.solPressOn();
        }
    }
    public void solPressOff() {
        if (isServiceBound) {
            valveService.solPressOff();
        }
    }

    public void pressValveUp() {
        if (isServiceBound) {
            valveService.pressValveUp();
        }
    }

    public void pressValveDown() {
        if (isServiceBound) {
            valveService.pressValveDown();
        }
    }

    // 밸브 제어 메소드들

    public void solVentOn() {
        if (isServiceBound) {
            valveService.solVentOn();
        }
    }
    public void solVentOff() {
        if (isServiceBound) {
            valveService.solVentOff();
        }
    }

    public void ventValveUp() {
        if (isServiceBound) {
            valveService.ventValveUp();
        }
    }

    public void ventValveDown() {
        if (isServiceBound) {
            valveService.ventValveDown();
        }
    }

    // 나머지 밸브 제어 메소드들도 추가

    public void unbindService() {
        if (isServiceBound) {
            context.unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}