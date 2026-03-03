package com.example.handstring.model;

import java.util.Date;

public class SensorData {
    private Date timestamp;
    private int heartRate;
    private int bloodOxygen;
    private int brightness;
    private SleepData sleepData;
    private ActivityData activityData;
    private float temperature;
    private MedicationRecord medicationRecord;

    // 添加getter和setter
    public MedicationRecord getMedicationRecord() {
        return medicationRecord;
    }

    public void setMedicationRecord(MedicationRecord medicationRecord) {
        this.medicationRecord = medicationRecord;
    }
    public static class SleepData {
        private int deepSleep; // 深睡时间（分钟）
        private int lightSleep; // 浅睡时间（分钟）
        private int awake; // 清醒时间（分钟）

        public SleepData(int deepSleep, int lightSleep, int awake) {
            this.deepSleep = deepSleep;
            this.lightSleep = lightSleep;
            this.awake = awake;
        }

        // Getters and Setters
        public int getDeepSleep() { return deepSleep; }
        public void setDeepSleep(int deepSleep) { this.deepSleep = deepSleep; }
        public int getLightSleep() { return lightSleep; }
        public void setLightSleep(int lightSleep) { this.lightSleep = lightSleep; }
        public int getAwake() { return awake; }
        public void setAwake(int awake) { this.awake = awake; }
    }

    public static class ActivityData {
        private int calories;
        private float distance;

        public ActivityData(int calories, float distance) {
            this.calories = calories;
            this.distance = distance;
        }

        // Getters and Setters
        public int getCalories() { return calories; }
        public void setCalories(int calories) { this.calories = calories; }
        public float getDistance() { return distance; }
        public void setDistance(float distance) { this.distance = distance; }
    }

    public SensorData() {
        this.timestamp = new Date();
    }

    // Getters and Setters
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public int getHeartRate() { return heartRate; }
    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }
    public int getBloodOxygen() { return bloodOxygen; }
    public void setBloodOxygen(int bloodOxygen) { this.bloodOxygen = bloodOxygen; }
    public int getBrightness() { return brightness; }
    public void setBrightness(int brightness) { this.brightness = brightness; }
    public SleepData getSleepData() { return sleepData; }
    public void setSleepData(SleepData sleepData) { this.sleepData = sleepData; }
    public ActivityData getActivityData() { return activityData; }
    public void setActivityData(ActivityData activityData) { this.activityData = activityData; }
    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }
} 