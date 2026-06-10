package com.mcsl.hbotchamberapp;

import android.app.Application;
import android.util.Log;

public class HBOTApplication extends Application {
    private static final String TAG = "HBOTApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        installCrashHandler();
    }

    private void installCrashHandler() {
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception — 긴급 밸브 정지 실행", throwable);
            try {
                HardwareEmergencyManager.getInstance().emergencyStop();
            } catch (Exception e) {
                Log.e(TAG, "긴급 정지 실패", e);
            }
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }
}
