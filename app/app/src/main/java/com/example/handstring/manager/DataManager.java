package com.example.handstring.manager;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.handstring.model.HealthData;
import com.example.handstring.model.SensorData;
import com.example.handstring.model.SleepData;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class DataManager {
    private static final String TAG = "DataManager";
    private static DataManager instance;
    private final List<SensorData> sensorDataList;
    private final List<DataUpdateListener> listeners;
    private final Handler mainHandler;
    private boolean isUpdating;
    private boolean isSimulationEnabled;
    private final Random random;
    private boolean isSimulating = false;
    private final List<HealthData> healthDataList;
    private final List<SleepData> sleepDataList;


    public interface DataUpdateListener {
        void onDataUpdated(SensorData data);
    }

    private DataManager() {
        sensorDataList = new ArrayList<>();
        listeners = new CopyOnWriteArrayList<>();
        mainHandler = new Handler(Looper.getMainLooper());
        random = new Random();
        healthDataList = new ArrayList<>();
        sleepDataList = new ArrayList<>();

    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public void addListener(DataUpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(DataUpdateListener listener) {
        listeners.remove(listener);
    }

    public void startDataUpdate() {
        if (!isUpdating) {
            isUpdating = true;
            updateData();
        }
    }

    public void stopDataUpdate() {
        isUpdating = false;
        mainHandler.removeCallbacksAndMessages(null);
    }

    public void setSimulationEnabled(boolean enabled) {
        isSimulationEnabled = enabled;
        if (enabled && !isUpdating) {
            startDataUpdate();
        } else if (!enabled && isUpdating) {
            stopDataUpdate();
        }
    }

    public boolean isSimulationEnabled() {
        return isSimulationEnabled;
    }

    private SensorData generateSimulatedSensorData() {
        SensorData data = new SensorData();
        Date now = new Date();
        data.setTimestamp(now);

        // 生成心率 (60-100)
        data.setHeartRate(80 + random.nextInt(3));

        // 生成血氧 (95-100%)
        data.setBloodOxygen(95 + random.nextInt(3));

        // 生成亮度 (0-1000)
        data.setBrightness(random.nextInt(1001));

        // 生成温度 (36.0-37.2°C)

        float temperature = 36.2f + random.nextFloat() * 0.2f; // 原始范围：36.0-37.2
        // 保留1位小数
        temperature = Math.round(temperature * 10) / 10.0f;
        data.setTemperature(temperature);


        // 生成活动数据
        data.setActivityData(new SensorData.ActivityData(
                100 + random.nextInt(500),    // 卡路里: 100-600
                1.0f + random.nextFloat() * 5.0f  // 距离: 1-6km
        ));

        // 每天午夜生成睡眠数据
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) < 5) {
            data.setSleepData(new SensorData.SleepData(
                    60 + random.nextInt(120),   // 深睡: 60-180分钟
                    120 + random.nextInt(180), // 浅睡: 120-300分钟
                    30 + random.nextInt(60)    // 清醒: 30-90分钟
            ));
        }

        return data;
    }

    private HealthData generateSimulatedHealthData() {
        return new HealthData(
                System.currentTimeMillis(),
                70 + random.nextInt(30),      // 心率: 70-100
                95 + random.nextInt(6),       // 血氧: 95-100%
                36.0f + random.nextFloat() * 1.2f  // 温度: 36.0-37.2°C
        );
    }

    private SleepData generateSimulatedSleepData() {
        return new SleepData(
                System.currentTimeMillis(),
                60 + random.nextInt(120),     // 深睡: 60-180分钟
                120 + random.nextInt(180),   // 浅睡: 120-300分钟
                30 + random.nextInt(60)      // 清醒: 30-90分钟
        );
    }

    private void updateData() {
        if (!isUpdating || !isSimulationEnabled) return;

        // 生成所有模拟数据
        SensorData sensorData = generateSimulatedSensorData();
        HealthData healthData = generateSimulatedHealthData();
        SleepData sleepData = generateSimulatedSleepData();

        // 更新数据列表
        sensorDataList.add(sensorData);
        healthDataList.add(healthData);
        sleepDataList.add(sleepData);

        // 限制数据量
        if (sensorDataList.size() > 1000) {
            sensorDataList.remove(0);
        }
        if (healthDataList.size() > 1000) {
            healthDataList.remove(0);
        }
        if (sleepDataList.size() > 1000) {
            sleepDataList.remove(0);
        }

        // 通知监听器
        for (DataUpdateListener listener : listeners) {
            mainHandler.post(() -> listener.onDataUpdated(sensorData));
        }

        // 设置下一次更新
        mainHandler.postDelayed(this::updateData, 1000);
    }

    public List<SensorData> getSensorDataList() {
        return new ArrayList<>(sensorDataList);
    }

    public List<SensorData> getSensorDataByTimeRange(Date startTime, Date endTime) {
        List<SensorData> result = new ArrayList<>();
        for (SensorData data : sensorDataList) {
            if (data.getTimestamp().after(startTime) && data.getTimestamp().before(endTime)) {
                result.add(data);
            }
        }
        return result;
    }

    public void clearData() {
        sensorDataList.clear();
    }

    public void startSimulation() {
        if (!isSimulating) {
            isSimulating = true;
            startDataUpdate();  // 使用统一的更新机制
        }
    }

    public void stopSimulation() {
        isSimulating = false;
        stopDataUpdate();
    }



    public List<HealthData> getHealthData() {
        return new ArrayList<>(healthDataList);
    }

    public List<SleepData> getSleepData() {
        return new ArrayList<>(sleepDataList);
    }


    public void clearAllData() {
        healthDataList.clear();
        sleepDataList.clear();
    }
} 