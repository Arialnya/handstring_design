package com.example.handstring;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;

import com.example.handstring.manager.DataManager;
import com.example.handstring.model.SensorData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity implements DataManager.DataUpdateListener {
    private LineChart heartRateChart;
    private LineChart bloodOxygenChart;
    private LineChart brightnessChart;
    private BarChart sleepChart;
    private TabLayout tabLayout;
    private DataManager dataManager;
    private int currentTimeRange = 0; // 0: 日, 1: 周, 2: 月

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // 初始化数据管理器
        dataManager = DataManager.getInstance();
        dataManager.addListener(this);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // 初始化视图
        initViews();
        // 设置图表样式
        setupCharts();
        // 设置时间选择器
        setupTimeSelector();
        // 加载示例数据
        loadSampleData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataManager.startDataUpdate();
        updateData(currentTimeRange);
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataManager.stopDataUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataManager.removeListener(this);
    }

    private void initViews() {
        heartRateChart = findViewById(R.id.heartRateChart);
        bloodOxygenChart = findViewById(R.id.bloodOxygenChart);
        brightnessChart = findViewById(R.id.brightnessChart);
        sleepChart = findViewById(R.id.sleepChart);
        tabLayout = findViewById(R.id.tabLayout);
    }

    private void setupCharts() {
        // 设置心率图表
        setupLineChart(heartRateChart, "心率 (bpm)", Color.RED);
        // 设置血氧图表
        setupLineChart(bloodOxygenChart, "血氧 (%)", Color.BLUE);
        // 设置亮度图表
        setupLineChart(brightnessChart, "亮度 (lux)", Color.YELLOW);
        // 设置睡眠图表
        setupBarChart(sleepChart);
    }

    private void setupLineChart(LineChart chart, String label, int color) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);

        // 设置X轴
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        // 设置Y轴
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);

        // 设置图例
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(12f);

        // 设置动画
        chart.animateX(1000);
    }

    private void setupBarChart(BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);

        // 设置X轴
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"深睡", "浅睡", "清醒"}));

        // 设置Y轴
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);

        // 设置图例
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextSize(12f);

        // 设置动画
        chart.animateY(1000);
    }

    private void setupTimeSelector() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 根据选择的时间范围更新数据
                updateData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateData(int timeRange) {
        currentTimeRange = timeRange;
        Calendar calendar = Calendar.getInstance();
        Date endTime = calendar.getTime();
        
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        switch (timeRange) {
            case 0: // 日
                // 使用当天数据
                break;
            case 1: // 周
                calendar.add(Calendar.DAY_OF_WEEK, -7);
                break;
            case 2: // 月
                calendar.add(Calendar.MONTH, -1);
                break;
        }
        Date startTime = calendar.getTime();

        List<SensorData> dataList = dataManager.getSensorDataByTimeRange(startTime, endTime);
        updateCharts(dataList);
    }

    private void updateCharts(List<SensorData> dataList) {
        if (dataList.isEmpty()) {
            loadSampleData(); // 如果没有数据，显示示例数据
            return;
        }

        // 更新心率图表
        List<Entry> heartRateEntries = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            SensorData data = dataList.get(i);
            heartRateEntries.add(new Entry(i, data.getHeartRate()));
        }
        LineDataSet heartRateDataSet = new LineDataSet(heartRateEntries, "心率");
        heartRateDataSet.setColor(Color.RED);
        heartRateDataSet.setCircleColor(Color.RED);
        heartRateDataSet.setDrawValues(false);
        heartRateChart.setData(new LineData(heartRateDataSet));
        heartRateChart.invalidate();

        // 更新血氧图表
        List<Entry> bloodOxygenEntries = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            SensorData data = dataList.get(i);
            bloodOxygenEntries.add(new Entry(i, data.getBloodOxygen()));
        }
        LineDataSet bloodOxygenDataSet = new LineDataSet(bloodOxygenEntries, "血氧");
        bloodOxygenDataSet.setColor(Color.BLUE);
        bloodOxygenDataSet.setCircleColor(Color.BLUE);
        bloodOxygenDataSet.setDrawValues(false);
        bloodOxygenChart.setData(new LineData(bloodOxygenDataSet));
        bloodOxygenChart.invalidate();

        // 更新亮度图表
        List<Entry> brightnessEntries = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            SensorData data = dataList.get(i);
            brightnessEntries.add(new Entry(i, data.getBrightness()));
        }
        LineDataSet brightnessDataSet = new LineDataSet(brightnessEntries, "亮度");
        brightnessDataSet.setColor(Color.YELLOW);
        brightnessDataSet.setCircleColor(Color.YELLOW);
        brightnessDataSet.setDrawValues(false);
        brightnessChart.setData(new LineData(brightnessDataSet));
        brightnessChart.invalidate();

        // 更新睡眠图表（使用最后一条数据的睡眠信息）
        SensorData lastData = dataList.get(dataList.size() - 1);
        if (lastData.getSleepData() != null) {
            List<BarEntry> sleepEntries = new ArrayList<>();
            sleepEntries.add(new BarEntry(0, lastData.getSleepData().getDeepSleep()));
            sleepEntries.add(new BarEntry(1, lastData.getSleepData().getLightSleep()));
            sleepEntries.add(new BarEntry(2, lastData.getSleepData().getAwake()));
            BarDataSet sleepDataSet = new BarDataSet(sleepEntries, "睡眠时间(分钟)");
            sleepDataSet.setColors(Color.BLUE, Color.CYAN, Color.GRAY);
            sleepChart.setData(new BarData(sleepDataSet));
            sleepChart.invalidate();
        }
    }

    private void loadSampleData() {
        // 加载心率数据
        List<Entry> heartRateEntries = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            heartRateEntries.add(new Entry(i, 60 + (int)(Math.random() * 40)));
        }
        LineDataSet heartRateDataSet = new LineDataSet(heartRateEntries, "心率");
        heartRateDataSet.setColor(Color.RED);
        heartRateDataSet.setCircleColor(Color.RED);
        heartRateDataSet.setDrawValues(false);
        heartRateChart.setData(new LineData(heartRateDataSet));
        heartRateChart.invalidate();

        // 加载血氧数据
        List<Entry> bloodOxygenEntries = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            bloodOxygenEntries.add(new Entry(i, 95 + (int)(Math.random() * 5)));
        }
        LineDataSet bloodOxygenDataSet = new LineDataSet(bloodOxygenEntries, "血氧");
        bloodOxygenDataSet.setColor(Color.BLUE);
        bloodOxygenDataSet.setCircleColor(Color.BLUE);
        bloodOxygenDataSet.setDrawValues(false);
        bloodOxygenChart.setData(new LineData(bloodOxygenDataSet));
        bloodOxygenChart.invalidate();

        // 加载亮度数据
        List<Entry> brightnessEntries = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            brightnessEntries.add(new Entry(i, (int)(Math.random() * 1000)));
        }
        LineDataSet brightnessDataSet = new LineDataSet(brightnessEntries, "亮度");
        brightnessDataSet.setColor(Color.YELLOW);
        brightnessDataSet.setCircleColor(Color.YELLOW);
        brightnessDataSet.setDrawValues(false);
        brightnessChart.setData(new LineData(brightnessDataSet));
        brightnessChart.invalidate();

        // 加载睡眠数据
        List<BarEntry> sleepEntries = new ArrayList<>();
        sleepEntries.add(new BarEntry(0, 120)); // 深睡
        sleepEntries.add(new BarEntry(1, 180)); // 浅睡
        sleepEntries.add(new BarEntry(2, 30));  // 清醒
        BarDataSet sleepDataSet = new BarDataSet(sleepEntries, "睡眠时间(分钟)");
        sleepDataSet.setColors(Color.BLUE, Color.CYAN, Color.GRAY);
        sleepChart.setData(new BarData(sleepDataSet));
        sleepChart.invalidate();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDataUpdated(SensorData data) {
        updateData(currentTimeRange);
    }
} 