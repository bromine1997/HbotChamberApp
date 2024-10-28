package com.mcsl.hbotchamberapp.repository;



import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.mcsl.hbotchamberapp.Service.ValveService;

public class ValveRepository {
    private static ValveRepository instance;
    private ValveService valveService;
    private boolean isServiceBound = false;
    private Context context;

    private MutableLiveData<Double> pressValveCurrentLiveData = new MutableLiveData<>(4.0);
    private MutableLiveData<Double> ventValveCurrentLiveData = new MutableLiveData<>(4.0);

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

            valveService.getPressValveCurrentLiveData().observeForever(pressValveCurrentObserver);
            valveService.getVentValveCurrentLiveData().observeForever(ventValveCurrentObserver);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            if (valveService != null) {
                valveService.getPressValveCurrentLiveData().removeObserver(pressValveCurrentObserver);
                valveService.getVentValveCurrentLiveData().removeObserver(ventValveCurrentObserver);
            }
            valveService = null;
        }
    };

    private Observer<Double> pressValveCurrentObserver = new Observer<Double>() {
        @Override
        public void onChanged(Double value) {
            pressValveCurrentLiveData.postValue(value);
        }
    };

    private Observer<Double> ventValveCurrentObserver = new Observer<Double>() {
        @Override
        public void onChanged(Double value) {
            ventValveCurrentLiveData.postValue(value);
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

    public LiveData<Double> getPressValveCurrent() {
        return pressValveCurrentLiveData;
    }

    public LiveData<Double> getVentValveCurrent() {
        return ventValveCurrentLiveData;
    }

    // 나머지 밸브 제어 메소드들도 추가

    public void unbindService() {
        if (isServiceBound) {
            if (valveService != null) {
                valveService.getPressValveCurrentLiveData().removeObserver(pressValveCurrentObserver);
                valveService.getVentValveCurrentLiveData().removeObserver(ventValveCurrentObserver);
            }
            context.unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}