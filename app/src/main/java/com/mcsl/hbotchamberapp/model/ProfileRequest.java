package com.mcsl.hbotchamberapp.model;

import java.util.List;

public class ProfileRequest {
    private List<ProfileSection> profileSections;

    public ProfileRequest(List<ProfileSection> profileSections) {
        this.profileSections = profileSections;
    }

    // Getters and Setters
    public List<ProfileSection> getProfileSections() {
        return profileSections;
    }

    public void setProfileSections(List<ProfileSection> profileSections) {
        this.profileSections = profileSections;
    }
}
