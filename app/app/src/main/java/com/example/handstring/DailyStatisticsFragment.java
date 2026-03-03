package com.example.handstring;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.handstring.db.HealthDataManager;
import com.example.handstring.model.HealthData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyStatisticsFragment extends Fragment {

    private LineChart heartRateChart;
    private BarChart sleepChart;
    private HealthDataManager dataManager;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化数据管理器
        dataManager = HealthDataManager.getInstance(requireContext());
        
        // 初始化图表
        heartRateChart = view.findViewById(R.id.heartRateChart);
        sleepChart = view.findViewById(R.id.sleepChart);
        
        setupHeartRateChart();
        setupSleepChart();
        
        // 加载数据
        loadHeartRateData();
        loadSleepData();
    }

    private void setupHeartRateChart() {
        // 设置图表基本属性
        heartRateChart.setDescription(null);
        heartRateChart.setTouchEnabled(true);
        heartRateChart.setDragEnabled(true);
        heartRateChart.setScaleEnabled(true);
        heartRateChart.setPinchZoom(true);
        heartRateChart.setDrawGridBackground(false);
        heartRateChart.setBackgroundColor(Color.WHITE);

        // 设置X轴
        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, (int) value);
                return timeFormat.format(calendar.getTime());
            }
        });

        // 设置Y轴
        YAxis leftAxis = heartRateChart.getAxisLeft();
        leftAxis.setAxisMinimum(50f);
        leftAxis.setAxisMaximum(120f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(10f);

        YAxis rightAxis = heartRateChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void setupSleepChart() {
        // 设置图表基本属性
        sleepChart.setDescription(null);
        sleepChart.setTouchEnabled(true);
        sleepChart.setDragEnabled(true);
        sleepChart.setScaleEnabled(true);
        sleepChart.setPinchZoom(true);
        sleepChart.setDrawGridBackground(false);
        sleepChart.setBackgroundColor(Color.WHITE);

        // 设置X轴
        XAxis xAxis = sleepChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "睡眠";
            }
        });

        // 设置Y轴
        YAxis leftAxis = sleepChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(480f); // 8小时
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(60f); // 1小时
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%d小时", (int)value / 60);
            }
        });

        YAxis rightAxis = sleepChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void loadHeartRateData() {
        // 获取今天的数据
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();
        
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        List<HealthData> dataList = dataManager.getHealthDataByDateRange(startDate, endDate);
        
        // 准备图表数据
        List<Entry> entries = new ArrayList<>();
        for (HealthData data : dataList) {
            if (data.getHeartRate() > 0) { // 只添加心率数据
                calendar.setTime(data.getTimestamp());
                float hour = calendar.get(Calendar.HOUR_OF_DAY);
                entries.add(new Entry(hour, data.getHeartRate()));
            }
        }

        // 创建数据集
        LineDataSet dataSet = new LineDataSet(entries, "心率");
        dataSet.setColor(Color.RED);
        dataSet.setCircleColor(Color.RED);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.argb(50, 255, 0, 0));

        // 设置数据
        LineData lineData = new LineData(dataSet);
        heartRateChart.setData(lineData);
        heartRateChart.invalidate();
    }

    private void loadSleepData() {
        // 获取今天的数据
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();
        
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        List<HealthData> dataList = dataManager.getHealthDataByDateRange(startDate, endDate);
        
        // 准备图表数据
        List<BarEntry> entries = new ArrayList<>();
        float deepSleep = 0f;
        float lightSleep = 0f;
        float awake = 0f;
        
        for (HealthData data : dataList) {
            if (data.getDeepSleepMinutes() > 0 || data.getLightSleepMinutes() > 0 || data.getAwakeMinutes() > 0) {
                deepSleep = data.getDeepSleepMinutes();
                lightSleep = data.getLightSleepMinutes();
                awake = data.getAwakeMinutes();
                break;
            }
        }

        entries.add(new BarEntry(0f, new float[]{deepSleep, lightSleep, awake}));

        // 创建数据集
        BarDataSet dataSet = new BarDataSet(entries, "睡眠质量");
        dataSet.setColors(new int[]{Color.BLUE, Color.CYAN, Color.GRAY});
        dataSet.setStackLabels(new String[]{"深睡", "浅睡", "清醒"});
        dataSet.setValueTextSize(10f);

        // 设置数据
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        sleepChart.setData(barData);
        sleepChart.invalidate();
    }
} 