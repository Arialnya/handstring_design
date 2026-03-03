package com.example.handstring.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "settings_pref";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_AGE = "age";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_AUTO_SYNC = "auto_sync";
    private static final String KEY_HEART_RATE_ALERT = "heart_rate_alert";
    private static final String KEY_SLEEP_ALERT = "sleep_alert";
    private static final String KEY_METRIC_UNIT = "metric_unit";

    private static SettingsManager instance;
    private final SharedPreferences preferences;

    private SettingsManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context.getApplicationContext());
        }
        return instance;
    }

    // 个人信息设置
    public void setNickname(String nickname) {
        preferences.edit().putString(KEY_NICKNAME, nickname).apply();
    }

    public String getNickname() {
        return preferences.getString(KEY_NICKNAME, "");
    }

    public void setGender(String gender) {
        preferences.edit().putString(KEY_GENDER, gender).apply();
    }

    public String getGender() {
        return preferences.getString(KEY_GENDER, "");
    }

    public void setAge(int age) {
        preferences.edit().putInt(KEY_AGE, age).apply();
    }

    public int getAge() {
        return preferences.getInt(KEY_AGE, 0);
    }

    public void setHeight(float height) {
        preferences.edit().putFloat(KEY_HEIGHT, height).apply();
    }

    public float getHeight() {
        return preferences.getFloat(KEY_HEIGHT, 0f);
    }

    public void setWeight(float weight) {
        preferences.edit().putFloat(KEY_WEIGHT, weight).apply();
    }

    public float getWeight() {
        return preferences.getFloat(KEY_WEIGHT, 0f);
    }

    // 设备管理
    public void setDeviceName(String deviceName) {
        preferences.edit().putString(KEY_DEVICE_NAME, deviceName).apply();
    }

    public String getDeviceName() {
        return preferences.getString(KEY_DEVICE_NAME, "未连接");
    }

    // 数据同步设置
    public void setAutoSync(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply();
    }

    public boolean isAutoSyncEnabled() {
        return preferences.getBoolean(KEY_AUTO_SYNC, true);
    }

    // 通知设置
    public void setHeartRateAlert(boolean enabled) {
        preferences.edit().putBoolean(KEY_HEART_RATE_ALERT, enabled).apply();
    }

    public boolean isHeartRateAlertEnabled() {
        return preferences.getBoolean(KEY_HEART_RATE_ALERT, true);
    }

    public void setSleepAlert(boolean enabled) {
        preferences.edit().putBoolean(KEY_SLEEP_ALERT, enabled).apply();
    }

    public boolean isSleepAlertEnabled() {
        return preferences.getBoolean(KEY_SLEEP_ALERT, true);
    }

    // 单位设置
    public void setMetricUnit(boolean enabled) {
        preferences.edit().putBoolean(KEY_METRIC_UNIT, enabled).apply();
    }

    public boolean isMetricUnitEnabled() {
        return preferences.getBoolean(KEY_METRIC_UNIT, true);
    }

    // 清除所有设置
    public void clearAllSettings() {
        preferences.edit().clear().apply();
    }
} 