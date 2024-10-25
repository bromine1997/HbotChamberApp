package com.mcsl.hbotchamberapp.ViewModel;


import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.mcsl.hbotchamberapp.repository.GpioRepository;


import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mcsl.hbotchamberapp.Controller.SensorData;
import com.mcsl.hbotchamberapp.repository.GpioRepository;
import com.mcsl.hbotchamberapp.repository.SensorRepository;
import com.mcsl.hbotchamberapp.repository.ValveRepository;

public class IoPortViewModel extends AndroidViewModel {
    private SensorRepository sensorRepository;
    private GpioRepository gpioRepository;
    private ValveRepository valveRepository;

    private LiveData<Double> pressValveCurrent;
    private LiveData<Double> ventValveCurrent;

    private LiveData<SensorData> sensorData;
    private LiveData<Byte> inputStatus;

    public IoPortViewModel(@NonNull Application application) {
        super(application);
        sensorRepository = SensorRepository.getInstance(application);
        gpioRepository = GpioRepository.getInstance(application);
        valveRepository = ValveRepository.getInstance(application);

        sensorData = sensorRepository.getSensorData();
        inputStatus = gpioRepository.getInputStatus();

        pressValveCurrent = valveRepository.getPressValveCurrent();
        ventValveCurrent = valveRepository.getVentValveCurrent();
    }

    public LiveData<SensorData> getSensorData() {
        return sensorData;
    }

    public LiveData<Byte> getInputStatus() {
        return inputStatus;
    }

    // GPIO 제어 메소드
    public void toggleLed(int ledNumber) {
        gpioRepository.toggleLed(ledNumber);
    }

    // 밸브 제어 메소드

    // 가압 Sol 벨브 제어
    public void solPressOn() {
        valveRepository.solPressOn();
    }

    public void solPressOff() {
        valveRepository.solPressOff();
    }

    // 배기 Sol 벨브 제어
    public void solVentOn() {
        valveRepository.solPressOn();
    }

    public void solVentOff() {
        valveRepository.solPressOff();
    }

    //가압 비례제어벨브
    public void pressProportionalValveUp() {
        valveRepository.pressValveUp();
    }

    public void pressProportionalValveDown() {
        valveRepository.pressValveDown();
    }
    ////

    //감압 비례제어벨브
    public void ventProportionalValveUp() {
        valveRepository.ventValveUp();
    }

    public void ventProportionalValveDown() {
        valveRepository.ventValveDown();
    }
    ////


    public LiveData<Double> getPressValveCurrent() {
        return pressValveCurrent;
    }

    public LiveData<Double> getVentValveCurrent() {
        return ventValveCurrent;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        sensorRepository.unbindService();
        gpioRepository.unbindService();
        valveRepository.unbindService();
    }
}