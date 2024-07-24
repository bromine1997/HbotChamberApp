package com.mcsl.hbotchamberapp.Activity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import java.util.List;

public class RunViewModel extends ViewModel {
    private final MutableLiveData<List<String[]>> profileData = new MutableLiveData<>();
    private final MutableLiveData<Long> elapsedTime = new MutableLiveData<>();

    public LiveData<List<String[]>> getProfileData() {
        return profileData;
    }

    public void setProfileData(List<String[]> data) {
        profileData.setValue(data);
    }

    public LiveData<Long> getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long time) {
        elapsedTime.setValue(time);
    }
}
