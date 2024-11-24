package com.mcsl.hbotchamberapp.model;

public class SensorDataPacket {
    private String deviceId;
    private String sessionId; // 추가된 필드
    private SensorData sensorData;
    private long elapsedTime;
    private double setPoint;
    private String userId; // userId 필드 추가

    // 생성
    public SensorDataPacket(String deviceId, String sessionId, String userId, SensorData sensorData, long elapsedTime, double setPoint) {
        this.deviceId = deviceId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.sensorData = sensorData;
        this.elapsedTime = elapsedTime;
        this.setPoint = setPoint;
    }


    // Getter와 Setter
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSessionId() { // 추가된 getter
        return sessionId;
    }

    public void setSessionId(String sessionId) { // 추가된 setter
        this.sessionId = sessionId;
    }

    public SensorData getSensorData() {
        return sensorData;
    }

    public void setSensorData(SensorData sensorData) {
        this.sensorData = sensorData;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public double getSetPoint() {
        return setPoint;
    }

    public void setSetPoint(double setPoint) {
        this.setPoint = setPoint;
    }
}
