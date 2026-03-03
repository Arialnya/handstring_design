package com.example.handstring;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.handstring.manager.BLEManager;
import com.example.handstring.manager.DataManager;
import com.example.handstring.utils.SettingsManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREF_NAME = "HandStringSettings";
    private static final String KEY_SIMULATION = "simulation_enabled";
    private ImageView ivAvatar;
    private TextInputEditText etNickname;
    private TextInputEditText etGender;
    private TextInputEditText etAge;
    private TextInputEditText etHeight;
    private TextInputEditText etWeight;
    private TextView tvDeviceName;
    private MaterialButton btnConnectDevice;
    private SwitchMaterial switchAutoSync;
    private MaterialButton btnManualSync;
    private SwitchMaterial switchHeartRateAlert;
    private SwitchMaterial switchSleepAlert;
    private SwitchMaterial switchMetricUnit;
    private MaterialButton btnSaveSettings;
    private SwitchMaterial simulationSwitch;
    private SharedPreferences preferences;
    private DataManager dataManager;
    private BLEManager bleManager;

    private SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 初始化设置管理器
        settingsManager = SettingsManager.getInstance(this);
        
        // 初始化数据管理器
        dataManager = DataManager.getInstance();

        // 初始化蓝牙管理器
        bleManager = BLEManager.getInstance(this);

        // 初始化SharedPreferences
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        // 初始化视图
        initViews();
        
        // 加载保存的设置
        loadSettings();
        
        // 设置点击事件
        setupClickListeners();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        etNickname = findViewById(R.id.etNickname);
        etGender = findViewById(R.id.etGender);
        etAge = findViewById(R.id.etAge);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        tvDeviceName = findViewById(R.id.tvDeviceName);
        btnConnectDevice = findViewById(R.id.btnConnectDevice);
        switchAutoSync = findViewById(R.id.switchAutoSync);
        btnManualSync = findViewById(R.id.btnManualSync);
        switchHeartRateAlert = findViewById(R.id.switchHeartRateAlert);
        switchSleepAlert = findViewById(R.id.switchSleepAlert);
        switchMetricUnit = findViewById(R.id.switchMetricUnit);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        simulationSwitch = findViewById(R.id.simulationSwitch);
        simulationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 保存设置
            preferences.edit().putBoolean(KEY_SIMULATION, isChecked).apply();
            // 更新数据管理器状态
            dataManager.setSimulationEnabled(isChecked);
        });
    }

    private void loadSettings() {
        // 加载个人信息
        etNickname.setText(settingsManager.getNickname());
        etGender.setText(settingsManager.getGender());
        etAge.setText(String.valueOf(settingsManager.getAge()));
        etHeight.setText(String.valueOf(settingsManager.getHeight()));
        etWeight.setText(String.valueOf(settingsManager.getWeight()));
        
        // 加载设备信息
        tvDeviceName.setText(settingsManager.getDeviceName());
        
        // 加载同步设置
        switchAutoSync.setChecked(settingsManager.isAutoSyncEnabled());
        
        // 加载通知设置
        switchHeartRateAlert.setChecked(settingsManager.isHeartRateAlertEnabled());
        switchSleepAlert.setChecked(settingsManager.isSleepAlertEnabled());
        
        // 加载单位设置
        switchMetricUnit.setChecked(settingsManager.isMetricUnitEnabled());

        boolean simulationEnabled = preferences.getBoolean(KEY_SIMULATION, false);
        simulationSwitch.setChecked(simulationEnabled);
        dataManager.setSimulationEnabled(simulationEnabled);
    }

    private void setupClickListeners() {
        // 头像点击
        ivAvatar.setOnClickListener(v -> {
            // TODO: 实现头像选择功能
        });

        // 性别选择
        etGender.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("选择性别")
                .setItems(new String[]{"男", "女"}, (dialog, which) -> {
                    String gender = which == 0 ? "男" : "女";
                    etGender.setText(gender);
                })
                .show();
        });

        // 连接设备
        btnConnectDevice.setOnClickListener(v -> {
            // TODO: 实现蓝牙设备连接
        });

        // 手动同步
        btnManualSync.setOnClickListener(v -> {
            // TODO: 实现手动数据同步
            Toast.makeText(this, "正在同步数据...", Toast.LENGTH_SHORT).show();
        });

        // 保存设置
        btnSaveSettings.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void saveSettings() {
        // 保存个人信息
        settingsManager.setNickname(etNickname.getText().toString());
        settingsManager.setGender(etGender.getText().toString());
        try {
            settingsManager.setAge(Integer.parseInt(etAge.getText().toString()));
            settingsManager.setHeight(Float.parseFloat(etHeight.getText().toString()));
            settingsManager.setWeight(Float.parseFloat(etWeight.getText().toString()));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 保存同步设置
        settingsManager.setAutoSync(switchAutoSync.isChecked());
        
        // 保存通知设置
        settingsManager.setHeartRateAlert(switchHeartRateAlert.isChecked());
        settingsManager.setSleepAlert(switchSleepAlert.isChecked());
        
        // 保存单位设置
        settingsManager.setMetricUnit(switchMetricUnit.isChecked());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // TODO: 处理头像选择和蓝牙连接的结果
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 