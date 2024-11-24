// SensorRepository.java

package com.mcsl.hbotchamberapp.repository;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.mcsl.hbotchamberapp.model.SensorData;
import com.mcsl.hbotchamberapp.Service.SensorService;

public class SensorRepository {
    private static SensorRepository instance;
    private SensorService sensorService;
    private boolean isServiceBound = false;
    private Context context;

    private MutableLiveData<SensorData> sensorDataLiveData = new MutableLiveData<>();

    private SensorRepository(Context context) {
        this.context = context.getApplicationContext();
        bindSensorService();
    }

    public static synchronized SensorRepository getInstance(Context context) {
        if (instance == null) {
            instance = new SensorRepository(context);
        }
        return instance;
    }

    private void bindSensorService() {
        Intent intent = new Intent(context, SensorService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
            sensorService = binder.getService();
            isServiceBound = true;

            // 센서 데이터 관찰
            sensorService.getSensorData().observeForever(sensorDataObserver);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    private Observer<SensorData> sensorDataObserver = new Observer<SensorData>() {
        @Override
        public void onChanged(SensorData data) {
            sensorDataLiveData.postValue(data);
        }
    };

    public LiveData<SensorData> getSensorData() {
        return sensorDataLiveData;
    }

    public void unbindService() {
        if (isServiceBound) {
            sensorService.getSensorData().removeObserver(sensorDataObserver);
            context.unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}
