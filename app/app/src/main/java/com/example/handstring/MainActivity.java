package com.example.handstring;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.handstring.adapter.DeviceAdapter;
import com.example.handstring.manager.DataManager;
import com.example.handstring.manager.BLEManager;
import com.example.handstring.manager.MedicationManager;
import com.example.handstring.model.SensorData;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceClickListener, DataManager.DataUpdateListener, BLEManager.BluetoothCallback {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private DeviceAdapter deviceAdapter;
    private List<BluetoothDevice> deviceList;
    private BluetoothAdapter bluetoothAdapter;

    private ImageView bluetoothStatus;
    private TextView heartRateValue;
    private TextView temperatureValue;
    private TextView bloodOxygenValue;
    private MaterialButton btnConnect;
    private RecyclerView deviceRecyclerView;

    private DataManager dataManager;
    private BLEManager bleManager;

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // 蓝牙已启用，刷新设备列表
                    refreshDeviceList();
                } else {
                    Toast.makeText(this, "请启用蓝牙以连接设备", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化数据管理器
        dataManager = DataManager.getInstance();
        dataManager.addListener(this);

        // 初始化BLE管理器
        bleManager = BLEManager.getInstance(this);
        bleManager.setCallback(this);

        // 初始化视图
        initViews();
        // 初始化蓝牙
        initBluetooth();
        // 设置点击事件
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataManager.startDataUpdate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataManager.stopDataUpdate();
    }

    private void initViews() {
        bluetoothStatus = findViewById(R.id.bluetoothStatus);
        heartRateValue = findViewById(R.id.heartRateValue);
        temperatureValue = findViewById(R.id.temperatureValue);
        bloodOxygenValue = findViewById(R.id.bloodOxygenValue);
        btnConnect = findViewById(R.id.btnConnect);
        deviceRecyclerView = findViewById(R.id.deviceRecyclerView);

        // 设置RecyclerView
        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(deviceList, this);
        deviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceRecyclerView.setAdapter(deviceAdapter);
    }

    private void initBluetooth() {
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            checkBluetoothPermissions();
        }
    }

    private void setupClickListeners() {
        btnConnect.setOnClickListener(v -> {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBluetoothLauncher.launch(enableBtIntent);
            } else {
                refreshDeviceList();
            }
        });

        findViewById(R.id.btnStatistics).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnMedication).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MedicationActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void checkBluetoothPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            refreshDeviceList();
        }
    }

    private void refreshDeviceList() {
        // 调用BLEManager进行BLE扫描
        deviceAdapter.clearDevices();
        bleManager.startScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                refreshDeviceList();
            } else {
                Toast.makeText(this, "需要蓝牙权限才能连接设备", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDataUpdated(SensorData data) {
        runOnUiThread(() -> {
            heartRateValue.setText(String.valueOf(data.getHeartRate()));
            bloodOxygenValue.setText(String.valueOf(data.getBloodOxygen()));
            temperatureValue.setText(String.valueOf(data.getTemperature()));
            // TODO: 更新其他UI组件
        });
    }

    @Override
    public void onDeviceClick(BluetoothDevice device) {
        // 调用BLEManager连接设备
        bleManager.connectToDevice(device);
    }

    // BLEManager回调实现
    @Override
    public void onDeviceConnected() {
        runOnUiThread(() -> {
            bluetoothStatus.setImageResource(R.drawable.ic_bluetooth_connected);
            btnConnect.setText("断开连接");
            Toast.makeText(this, "设备已连接", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDeviceDisconnected() {
        runOnUiThread(() -> {
            bluetoothStatus.setImageResource(R.drawable.ic_bluetooth_disconnected);
            btnConnect.setText("扫描设备");
            Toast.makeText(this, "设备已断开", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDataReceived(String data) {
        try {
            JSONObject jsonData = new JSONObject(data);
            SensorData sensorData = new SensorData();
            
            // 处理时间戳
            if (jsonData.has("timestamp")) {
                try {
                    String timestampStr = jsonData.getString("timestamp");
                    // 解析ISO 8601格式的时间戳
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    Date timestamp = sdf.parse(timestampStr);
                    sensorData.setTimestamp(timestamp);
                } catch (ParseException e) {
                    // 如果时间戳解析失败，使用当前时间
                    sensorData.setTimestamp(new Date());
                    e.printStackTrace();
                }
            } else {
                // 如果没有时间戳，使用当前时间
                sensorData.setTimestamp(new Date());
            }

            // 处理药物信息
            if (jsonData.has("medication")) {
                try {
                    JSONObject medicationData = jsonData.getJSONObject("medication");
                    String medicineName = medicationData.getString("medicineName");
                    String dosage = medicationData.getString("dosage");
                    String scheduledTimeStr = medicationData.getString("scheduledTime");
                    boolean taken = medicationData.getBoolean("taken");
                    
                    // 解析计划服药时间
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    Date scheduledTime = sdf.parse(scheduledTimeStr);
                    
                    // 获取实际服药时间（如果有）
                    Date actualTime = null;
                    if (medicationData.has("actualTime")) {
                        String actualTimeStr = medicationData.getString("actualTime");
                        actualTime = sdf.parse(actualTimeStr);
                    }
                    
                    // 更新药物记录
                    MedicationManager.getInstance().updateMedicationStatus(
                        medicineName, scheduledTime, taken, actualTime);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            if (jsonData.has("heartRate")) {
                sensorData.setHeartRate(jsonData.getInt("heartRate"));
            }
            if (jsonData.has("bloodOxygen")) {
                sensorData.setBloodOxygen(jsonData.getInt("bloodOxygen"));
            }
            if (jsonData.has("brightness")) {
                sensorData.setBrightness(jsonData.getInt("brightness"));
            }
            if (jsonData.has("temperature")) {
                sensorData.setTemperature((float) jsonData.getDouble("temperature"));
            }
            if (jsonData.has("sleep")) {
                JSONObject sleepData = jsonData.getJSONObject("sleep");
                SensorData.SleepData sleep = new SensorData.SleepData(
                    sleepData.getInt("deepSleep"),
                    sleepData.getInt("lightSleep"),
                    sleepData.getInt("awake")
                );
                sensorData.setSleepData(sleep);
            }
            if (jsonData.has("activity")) {
                JSONObject activityData = jsonData.getJSONObject("activity");
                SensorData.ActivityData activity = new SensorData.ActivityData(
                    activityData.getInt("calories"),
                    (float) activityData.getDouble("distance")
                );
                sensorData.setActivityData(activity);
            }

            runOnUiThread(() -> {
                heartRateValue.setText(String.valueOf(sensorData.getHeartRate()));
                bloodOxygenValue.setText(String.valueOf(sensorData.getBloodOxygen()));
                temperatureValue.setText(String.valueOf(sensorData.getTemperature()));
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        runOnUiThread(() -> {
            if (device.getName() != null && device.getName().contains("ESP32")) {
                deviceAdapter.addDevice(device);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataManager.removeListener(this);
        if (bluetoothAdapter != null) {
            // 删除经典蓝牙断开连接相关
            // if (bluetoothService != null) {
            //     bluetoothService.disconnect();
            // }
        }
    }

}