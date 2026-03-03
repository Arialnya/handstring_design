package com.example.handstring.model;

public class SleepData {
    private long timestamp;
    private int deepSleepMinutes;
    private int lightSleepMinutes;
    private int remSleepMinutes;

    public SleepData(long timestamp, int deepSleepMinutes, int lightSleepMinutes, int remSleepMinutes) {
        this.timestamp = timestamp;
        this.deepSleepMinutes = deepSleepMinutes;
        this.lightSleepMinutes = lightSleepMinutes;
        this.remSleepMinutes = remSleepMinutes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public int getRemSleepMinutes() {
        return remSleepMinutes;
    }

    public void setRemSleepMinutes(int remSleepMinutes) {
        this.remSleepMinutes = remSleepMinutes;
    }

    public int getTotalSleepMinutes() {
        return deepSleepMinutes + lightSleepMinutes + remSleepMinutes;
    }
} 