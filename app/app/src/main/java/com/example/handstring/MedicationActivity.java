package com.example.handstring;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.handstring.adapter.MedicationAdapter;
import com.example.handstring.manager.MedicationManager;
import com.example.handstring.model.MedicationRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MedicationActivity extends AppCompatActivity implements MedicationManager.MedicationUpdateListener {
    private RecyclerView medicationRecyclerView;
    private MedicationAdapter medicationAdapter;
    private final List<MedicationRecord> medicationRecords = new CopyOnWriteArrayList<>();
    private MedicationManager medicationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication);

        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 初始化MedicationManager
        medicationManager = MedicationManager.getInstance();
        medicationManager.addListener(this);

        // 初始化RecyclerView
        medicationRecyclerView = findViewById(R.id.medicationRecyclerView);
        medicationRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 初始化适配器
        medicationAdapter = new MedicationAdapter(medicationRecords);
        medicationRecyclerView.setAdapter(medicationAdapter);

        // 加载当天的药物记录
        loadTodayMedicationRecords();
    }

    private void loadTodayMedicationRecords() {
        Date today = new Date();
        List<MedicationRecord> todayRecords = medicationManager.getMedicationRecordsByDate(today);
        
        // 清空当前列表
        medicationRecords.clear();
        
        if (todayRecords.isEmpty()) {
            // 如果没有今天的记录，生成示例数据
            List<MedicationRecord> sampleRecords = generateSampleData();
            // 先添加到MedicationManager
            for (MedicationRecord record : sampleRecords) {
                medicationManager.addMedicationRecord(record);
            }
            // 然后添加到本地列表
            medicationRecords.addAll(sampleRecords);
        } else {
            // 添加今天的记录
            medicationRecords.addAll(todayRecords);
        }
        
        // 通知适配器更新
        runOnUiThread(() -> medicationAdapter.notifyDataSetChanged());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        medicationManager.removeListener(this);
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
    public void onMedicationUpdated(List<MedicationRecord> records) {
        runOnUiThread(() -> {
            medicationRecords.clear();
            medicationRecords.addAll(records);
            medicationAdapter.notifyDataSetChanged();
        });
    }

    private List<MedicationRecord> generateSampleData() {
        List<MedicationRecord> records = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // 添加示例数据
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        records.add(new MedicationRecord("阿司匹林", "100mg", calendar.getTime(), calendar.getTime(), true));

        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 30);
        records.add(new MedicationRecord("维生素C", "500mg", calendar.getTime(), calendar.getTime(), true));

        calendar.set(Calendar.HOUR_OF_DAY, 18);
        calendar.set(Calendar.MINUTE, 0);
        records.add(new MedicationRecord("钙片", "1片", calendar.getTime(), null, false));

        return records;
    }
} 