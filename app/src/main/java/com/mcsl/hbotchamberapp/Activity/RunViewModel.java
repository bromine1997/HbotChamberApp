package com.mcsl.hbotchamberapp.Activity;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.mcsl.hbotchamberapp.model.ProfileSection;
import com.mcsl.hbotchamberapp.Controller.SensorData;
import com.mcsl.hbotchamberapp.repository.ProfileRepository;

import java.util.ArrayList;
import java.util.List;

public class RunViewModel extends ViewModel {
    private final MutableLiveData<List<ProfileSection>> profileData = new MutableLiveData<>();
    private final MutableLiveData<Long> elapsedTime = new MutableLiveData<>();
    private final LiveData<String> formattedElapsedTime = Transformations.map(elapsedTime, this::formatElapsedTime);
    private final MutableLiveData<SensorData> sensorData = new MutableLiveData<>();
    private final MutableLiveData<String> pidPhase = new MutableLiveData<>();
    private final MutableLiveData<Double> setPoint = new MutableLiveData<>();
    private final MutableLiveData<Boolean> pidControlRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isWebSocketConnected = new MutableLiveData<>(false);

    // Load profile data using ProfileRepository
    public void loadProfileData(Context context) {
        ProfileRepository repository = new ProfileRepository(context);
        List<ProfileSection> data = repository.loadProfileDataFromFile();
        setProfileData(data);
    }

    public LiveData<List<ProfileSection>> getProfileData() {
        return profileData;
    }

    public void setProfileData(List<ProfileSection> data) {
        profileData.setValue(data);
    }

    public LiveData<Long> getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long time) {
        elapsedTime.setValue(time);
    }

    public LiveData<String> getFormattedElapsedTime() {
        return formattedElapsedTime;
    }

    private String formatElapsedTime(long elapsedTime) {
        int seconds = (int) (elapsedTime / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public LiveData<SensorData> getSensorData() {
        return sensorData;
    }

    public void updateSensorData(SensorData data) {
        sensorData.setValue(data);
    }

    public LiveData<Double> getSetPoint() {
        return setPoint;
    }

    public void setSetPoint(double setPointValue) {
        setPoint.setValue(setPointValue);
    }

    public LiveData<String> getPidPhase() {
        return pidPhase;
    }

    public void setPidPhase(String phase) {
        pidPhase.setValue(phase);
    }

    public LiveData<Boolean> isPidControlRunning() {
        return pidControlRunning;
    }

    public void setPidControlRunning(boolean isRunning) {
        pidControlRunning.setValue(isRunning);
    }

    public LiveData<Boolean> getIsWebSocketConnected() {
        return isWebSocketConnected;
    }

    public void setWebSocketConnected(boolean isConnected) {
        isWebSocketConnected.setValue(isConnected);
    }

    public void addProfileSection(ProfileSection section) {
        List<ProfileSection> currentData = profileData.getValue();
        if (currentData == null) {
            currentData = new ArrayList<>();
        }
        currentData.add(section);
        profileData.setValue(currentData);
    }

    public void updateProfileSection(int index, ProfileSection updatedSection) {
        List<ProfileSection> currentData = profileData.getValue();
        if (currentData != null && index >= 0 && index < currentData.size()) {
            currentData.set(index, updatedSection);
            profileData.setValue(currentData);
        }
    }
}
