package com.mcsl.hbotchamberapp.model;

public class SensorData {

    private double pressure;     // 압력 값
    private double temperature;  // 온도 값
    private double humidity;     // 습도 값
    private double oxygen;       // 산소 값
    private double co2;          // 이산화탄소 값
    private double flowRate;     // 유량 값

    // 기본 생성자
    public SensorData() {}

    // 생성자
    public SensorData(double pressure, double temperature, double humidity, double oxygen, double co2, double flowRate) {
        this.pressure = pressure;
        this.temperature = temperature;
        this.humidity = humidity;
        this.oxygen = oxygen;
        this.co2 = co2;
        this.flowRate = flowRate;
    }

    // Getter/Setter 메서드
    public double getPressure() {
        return pressure;
    }


    public double getTemperature() {
        return temperature;
    }


    public double getHumidity() {
        return humidity;
    }

    public double getOxygen() {
        return oxygen;
    }

    public double getFlowRate() {
        return flowRate;
    }

    public double getCo2() {
        return co2;
    }

    public void setCo2(double co2) {
        this.co2 = co2;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }


    public void setOxygen(double oxygen) {
        this.oxygen = oxygen;
    }

    public void setFlowRate(double flowRate) {
        this.flowRate = flowRate;
    }
}
