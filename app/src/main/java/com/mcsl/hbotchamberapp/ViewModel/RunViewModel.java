package com.mcsl.hbotchamberapp.ViewModel;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.mcsl.hbotchamberapp.model.ProfileSection;
import com.mcsl.hbotchamberapp.Controller.SensorData;
import com.mcsl.hbotchamberapp.repository.ProfileRepository;
import com.mcsl.hbotchamberapp.repository.PIDRepository;
import com.mcsl.hbotchamberapp.repository.SensorRepository;

import java.util.ArrayList;
import java.util.List;

public class RunViewModel extends AndroidViewModel {
    private final MutableLiveData<List<ProfileSection>> profileData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> pidControlRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isWebSocketConnected = new MutableLiveData<>(false);

    private PIDRepository pidRepository;
    private SensorRepository sensorRepository;

    private LiveData<Long> elapsedTime;
    private LiveData<String> formattedElapsedTime;
    private LiveData<SensorData> sensorData;
    private LiveData<Double> setPoint;
    private LiveData<String> pidPhase;

    public RunViewModel(Application application) {
        super(application);
        Context context = application.getApplicationContext();

        pidRepository = PIDRepository.getInstance(context);
        sensorRepository = SensorRepository.getInstance(context);

        elapsedTime = pidRepository.getElapsedTime();
        formattedElapsedTime = Transformations.map(elapsedTime, this::formatElapsedTime);
        sensorData = sensorRepository.getSensorData();
        setPoint = pidRepository.getSetPoint();
        pidPhase = pidRepository.getPidPhase();
    }

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

    public LiveData<Double> getSetPoint() {
        return setPoint;
    }

    public LiveData<String> getPidPhase() {
        return pidPhase;
    }

    public LiveData<Boolean> isPidControlRunning() {
        return pidControlRunning;
    }

    public void startPidControl() {
        pidControlRunning.setValue(true);
        pidRepository.startPidControl();
    }

    public void pausePidControl() {
        pidRepository.pausePidControl();
    }

    public void resumePidControl() {
        pidRepository.resumePidControl();
    }

    public void stopPidControl() {
        pidControlRunning.setValue(false);
        pidRepository.stopPidControl();
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

