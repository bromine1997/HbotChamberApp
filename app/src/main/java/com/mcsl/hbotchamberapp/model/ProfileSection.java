package com.mcsl.hbotchamberapp.model;
public class ProfileSection {

    private int sectionNumber;   // 섹션 번호를 숫자로 저장
    private float startPressure; // 시작 압력
    private float endPressure;   // 종료 압력
    private float duration;      // 구간 지속 시간 (분 단위)

    // 기본 생성자
    public ProfileSection() {}

    // String으로 섹션 번호를 받아 숫자로 변환하여 저장하는 생성자
    public ProfileSection(String sectionName, float startPressure, float endPressure, float duration) {
        try {
            this.sectionNumber = Integer.parseInt(sectionName);  // String을 int로 변환
        } catch (NumberFormatException e) {
            this.sectionNumber = 0;  // 변환 실패 시 기본값 0으로 설정
            e.printStackTrace();
        }
        this.startPressure = startPressure;
        this.endPressure = endPressure;
        this.duration = duration;
    }

    // Getter/Setter 메서드
    public int getSectionNumber() {
        return sectionNumber;
    }

    public void setSectionNumber(int sectionNumber) {
        this.sectionNumber = sectionNumber;
    }

    public float getStartPressure() {
        return startPressure;
    }

    public void setStartPressure(float startPressure) {
        this.startPressure = startPressure;
    }

    public float getEndPressure() {
        return endPressure;
    }

    public void setEndPressure(float endPressure) {
        this.endPressure = endPressure;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }
}
