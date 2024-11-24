package com.mcsl.hbotchamberapp.model;

public enum PIDState {
    STARTED,   // PID 제어가 시작된 상태
    RUNNING,   // PID 제어가 정상적으로 실행 중인 상태
    PAUSED,    // PID 제어가 일시정지된 상태
    STOPPED    // PID 제어가 종료된 상태
}