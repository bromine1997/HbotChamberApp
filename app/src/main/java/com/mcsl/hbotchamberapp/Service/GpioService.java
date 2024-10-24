package com.mcsl.hbotchamberapp.Service;



import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mcsl.hbotchamberapp.Controller.PinController;


public class GpioService extends Service {
    private static final String TAG = "GpioService";
    private Handler handler;
    private Runnable i2cRunnable;

    private PinController pinController;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public GpioService getService() {
            return GpioService.this;
        }
    }

    private final MutableLiveData<Byte> inputStatusLiveData = new MutableLiveData<>();

    public LiveData<Byte> getInputStatus() {
        return inputStatusLiveData;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        pinController = new PinController();

        HandlerThread handlerThread = new HandlerThread("GpioServiceBackgroundThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        i2cRunnable = new Runnable() {
            @Override
            public void run() {
                readAndUpdateInputStatus();
                handler.postDelayed(this, 100); // 0.1초마다 실행
            }
        };

        // 처음 실행
        handler.postDelayed(i2cRunnable, 100);
    }

    private void readAndUpdateInputStatus() {
        byte inputStatus = pinController.readInputs(); // 외부 입력 스위치 주기적으로 확인
        inputStatusLiveData.postValue(inputStatus);
    }

    public void toggleLed(int ledNumber) {
        pinController.toggleLed(ledNumber);
        // 필요 시, LED 상태를 업데이트하는 로직 추가
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(i2cRunnable);
        handler.getLooper().quit(); // HandlerThread 종료
    }
}


