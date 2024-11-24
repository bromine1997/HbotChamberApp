package com.mcsl.hbotchamberapp.model;

import java.util.List;

public class ProfileRequest {
    private String userId; // userId 필드 추가
    private String name;   // 프로파일 이름 필드
    private List<ProfileSection> profileSections;

    // 생성자 수정
    public ProfileRequest(String userId, String name, List<ProfileSection> profileSections) {
        this.userId = userId;
        this.name = name;
        this.profileSections = profileSections;
    }

    // Getter와 Setter 추가
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // 기존 Getter와 Setter 유지
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ProfileSection> getProfileSections() {
        return profileSections;
    }

    public void setProfileSections(List<ProfileSection> profileSections) {
        this.profileSections = profileSections;
    }
}