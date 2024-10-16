package com.mcsl.hbotchamberapp.Activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mcsl.hbotchamberapp.model.ProfileSection;
import com.mcsl.hbotchamberapp.Controller.SensorData;

import java.util.ArrayList;
import java.util.List;

public class RunViewModel extends ViewModel {
    // 프로파일 데이터를 List<ProfileSection>으로 수정
    private final MutableLiveData<List<ProfileSection>> profileData = new MutableLiveData<>();

    // 경과 시간
    private final MutableLiveData<Long> elapsedTime = new MutableLiveData<>();

    // 센서 데이터 (압력, 온도 등)
    private final MutableLiveData<SensorData> sensorData = new MutableLiveData<>();

    // PID 제어 상태 (가압, 유지, 감압 상태)
    private final MutableLiveData<String> pidPhase = new MutableLiveData<>(); // 압력 단계 상태

    // SetPoint 데이터
    private final MutableLiveData<Double> setPoint = new MutableLiveData<>(); // SetPoint를 저장하는 LiveData

    // PID 제어 상태
    private final MutableLiveData<Boolean> pidControlRunning = new MutableLiveData<>(false);

    // WebSocket 연결 상태
    private final MutableLiveData<Boolean> isWebSocketConnected = new MutableLiveData<>(false);

    // 프로파일 데이터 관련 LiveData
    public LiveData<List<ProfileSection>> getProfileData() {
        return profileData;
    }

    // 프로파일 데이터를 설정하는 메서드
    public void setProfileData(List<ProfileSection> data) {
        profileData.setValue(data);
    }

    // 경과 시간 관련 LiveData
    public LiveData<Long> getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long time) {
        elapsedTime.setValue(time);
    }

    private String formatElapsedTime(long elapsedTime) {

        int seconds = (int) (elapsedTime / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }


    // 센서 데이터 관련 LiveData
    public LiveData<SensorData> getSensorData() {
        return sensorData;
    }

    public void updateSensorData(SensorData data) {
        sensorData.setValue(data);
    }

    // SetPoint 관련 LiveData
    public LiveData<Double> getSetPoint() {
        return setPoint;
    }

    public void setSetPoint(double setPointValue) {
        setPoint.setValue(setPointValue);
    }

    // PID 상태(가압, 유지, 감압) 관련 LiveData
    public LiveData<String> getPidPhase() {
        return pidPhase;
    }

    public void setPidPhase(String phase) {
        pidPhase.setValue(phase);
    }

    // PID 제어 상태 관련 LiveData
    public LiveData<Boolean> isPidControlRunning() {
        return pidControlRunning;
    }

    public void setPidControlRunning(boolean isRunning) {
        pidControlRunning.setValue(isRunning);
    }

    // WebSocket 연결 상태 관련 LiveData
    public LiveData<Boolean> getIsWebSocketConnected() {
        return isWebSocketConnected;
    }

    public void setWebSocketConnected(boolean isConnected) {
        isWebSocketConnected.setValue(isConnected);
    }

    // 프로파일 데이터를 새로 추가하는 메서드
    public void addProfileSection(ProfileSection section) {
        List<ProfileSection> currentData = profileData.getValue();
        if (currentData == null) {
            currentData = new ArrayList<>();
        }
        currentData.add(section);
        profileData.setValue(currentData);
    }

    // 프로파일 데이터를 업데이트하는 메서드
    public void updateProfileSection(int index, ProfileSection updatedSection) {
        List<ProfileSection> currentData = profileData.getValue();
        if (currentData != null && index >= 0 && index < currentData.size()) {
            currentData.set(index, updatedSection);
            profileData.setValue(currentData);
        }
    }
}
