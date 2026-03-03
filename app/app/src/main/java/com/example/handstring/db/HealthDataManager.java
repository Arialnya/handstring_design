package com.example.handstring.db;

import android.content.Context;

import com.example.handstring.model.HealthData;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class HealthDataManager {
    private HealthDataDbHelper dbHelper;
    private static HealthDataManager instance;

    private HealthDataManager(Context context) {
        dbHelper = new HealthDataDbHelper(context);
    }

    public static synchronized HealthDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new HealthDataManager(context);
        }
        return instance;
    }

    public void insertHealthData(HealthData data) {
        dbHelper.insertHealthData(data);
    }

    public List<HealthData> getAllHealthData() {
        return dbHelper.getAllHealthData();
    }

    public List<HealthData> getHealthDataByDateRange(Date startDate, Date endDate) {
        return dbHelper.getHealthDataByDateRange(startDate, endDate);
    }

    public void generateSampleData() {
        Calendar calendar = Calendar.getInstance();
        Random random = new Random();

        // 生成最近7天的数据
        for (int i = 0; i < 7; i++) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            Date date = calendar.getTime();

            // 生成心率数据（每小时一个数据点）
            for (int hour = 0; hour < 24; hour++) {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                int heartRate = 60 + random.nextInt(40); // 60-100之间的随机心率
                HealthData data = new HealthData(
                    calendar.getTime(),
                    heartRate,
                    0, 0, 0 // 睡眠数据在下面单独生成
                );
                insertHealthData(data);
            }

            // 生成睡眠数据
            int deepSleep = 180 + random.nextInt(60); // 3-4小时深睡
            int lightSleep = 240 + random.nextInt(120); // 4-6小时浅睡
            int awake = 60 + random.nextInt(60); // 1-2小时清醒
            HealthData sleepData = new HealthData(
                date,
                0,
                deepSleep,
                lightSleep,
                awake
            );
            insertHealthData(sleepData);
        }
    }
} 