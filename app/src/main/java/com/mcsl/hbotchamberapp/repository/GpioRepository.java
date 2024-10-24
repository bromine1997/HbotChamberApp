package com.mcsl.hbotchamberapp.repository;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.mcsl.hbotchamberapp.Service.GpioService;

public class GpioRepository {
    private static GpioRepository instance;
    private GpioService gpioService;
    private boolean isServiceBound = false;
    private Context context;

    private MutableLiveData<Byte> inputStatusLiveData = new MutableLiveData<>();

    private GpioRepository(Context context) {
        this.context = context.getApplicationContext();
        bindGpioService();
    }

    public static synchronized GpioRepository getInstance(Context context) {
        if (instance == null) {
            instance = new GpioRepository(context);
        }
        return instance;
    }

    private void bindGpioService() {
        Intent intent = new Intent(context, GpioService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GpioService.LocalBinder binder = (GpioService.LocalBinder) service;
            gpioService = binder.getService();
            isServiceBound = true;

            // 입력 상태 관찰
            gpioService.getInputStatus().observeForever(inputStatusObserver);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    private Observer<Byte> inputStatusObserver = new Observer<Byte>() {
        @Override
        public void onChanged(Byte status) {
            inputStatusLiveData.postValue(status);
        }
    };

    public LiveData<Byte> getInputStatus() {
        return inputStatusLiveData;
    }

    public void toggleLed(int ledNumber) {
        if (gpioService != null) {
            gpioService.toggleLed(ledNumber);
        }
    }

    public void unbindService() {
        if (isServiceBound) {
            gpioService.getInputStatus().removeObserver(inputStatusObserver);
            context.unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}
