package com.mcsl.hbotchamberapp;

import android.util.Log;

import com.mcsl.hbotchamberapp.Service.ValveService;

/**
 * 앱 크래시 시 밸브를 안전하게 닫기 위한 싱글톤 레지스트리.
 * ValveService가 시작될 때 자신을 등록하고, HBOTApplication의 UncaughtExceptionHandler가
 * 이를 통해 긴급 정지를 호출한다.
 */
public class HardwareEmergencyManager {
    private static final String TAG = "HardwareEmergencyMgr";

    private static volatile HardwareEmergencyManager instance;
    private volatile ValveService valveService;

    private HardwareEmergencyManager() {}

    public static HardwareEmergencyManager getInstance() {
        if (instance == null) {
            synchronized (HardwareEmergencyManager.class) {
                if (instance == null) instance = new HardwareEmergencyManager();
            }
        }
        return instance;
    }

    public void register(ValveService service) {
        this.valveService = service;
        Log.d(TAG, "ValveService 등록 완료");
    }

    public void unregister() {
        this.valveService = null;
    }

    public void emergencyStop() {
        ValveService service = this.valveService;
        if (service != null) {
            try {
                service.stopAllValves();
                Log.e(TAG, "긴급 밸브 정지 완료");
            } catch (Exception e) {
                Log.e(TAG, "긴급 밸브 정지 실패", e);
            }
        } else {
            Log.e(TAG, "긴급 정지: ValveService 미등록 상태");
        }
    }
}
