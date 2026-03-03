package com.example.handstring.model;

import java.util.Date;

public class HealthData {
    private long id;
    private Date timestamp;
    private int heartRate;
    private int bloodOxygen;
    private int brightness;
    private int deepSleepMinutes;
    private int lightSleepMinutes;
    private int awakeMinutes;
    private int steps;
    private float temperature;

    public HealthData() {
        this.timestamp = new Date();
    }

    public HealthData(Date timestamp, int heartRate, int deepSleepMinutes, int lightSleepMinutes, int awakeMinutes) {
        this.timestamp = timestamp;
        this.heartRate = heartRate;
        this.deepSleepMinutes = deepSleepMinutes;
        this.lightSleepMinutes = lightSleepMinutes;
        this.awakeMinutes = awakeMinutes;
    }

    public HealthData(long timestamp, int heartRate, int bloodOxygen, float temperature) {
        this.timestamp = new Date(timestamp);
        this.heartRate = heartRate;
        this.bloodOxygen = bloodOxygen;
        this.temperature = temperature;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public int getBloodOxygen() {
        return bloodOxygen;
    }

    public void setBloodOxygen(int bloodOxygen) {
        this.bloodOxygen = bloodOxygen;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    public int getDeepSleepMinutes() {
        return deepSleepMinutes;
    }

    public void setDeepSleepMinutes(int deepSleepMinutes) {
        this.deepSleepMinutes = deepSleepMinutes;
    }

    public int getLightSleepMinutes() {
        return lightSleepMinutes;
    }

    public void setLightSleepMinutes(int lightSleepMinutes) {
        this.lightSleepMinutes = lightSleepMinutes;
    }

    public int getAwakeMinutes() {
        return awakeMinutes;
    }

    public void setAwakeMinutes(int awakeMinutes) {
        this.awakeMinutes = awakeMinutes;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
} 