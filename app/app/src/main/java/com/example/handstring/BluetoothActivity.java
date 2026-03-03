package com.example.handstring;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.handstring.adapter.DeviceAdapter;
import com.example.handstring.manager.BLEManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class BluetoothActivity extends AppCompatActivity implements BLEManager.BluetoothCallback {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final long SCAN_PERIOD = 10000; // 扫描10秒
    
    private BLEManager bleManager;
    private BluetoothLeScanner bluetoothLeScanner;
    private DeviceAdapter deviceAdapter;
    private MaterialButton btnScan;
    private boolean isScanning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, 
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            BluetoothDevice device = result.getDevice();
            // 显示所有扫描到的设备
            deviceAdapter.addDevice(device);
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        
        // 初始化蓝牙管理器
        bleManager = BLEManager.getInstance(this);
        bleManager.setCallback(this);
        
        // 初始化UI
        RecyclerView recyclerView = findViewById(R.id.deviceList);
        btnScan = findViewById(R.id.btnScan);
        
        deviceAdapter = new DeviceAdapter(new ArrayList<>(), device -> {
            if (!bleManager.isConnected()) {
                bleManager.connectToDevice(device);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(deviceAdapter);
        
        btnScan.setOnClickListener(v -> {
            if (!isScanning) {
                startScan();
            } else {
                stopScan();
            }
        });
        
        // 检查权限
        checkPermissions();
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };
        
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsToRequest.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }
    }
    
    private void startScan() {
        if (!bleManager.isBluetoothEnabled()) {
            Toast.makeText(this, "请打开蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        
        deviceAdapter.clearDevices();
        isScanning = true;
        btnScan.setText("停止扫描");
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        bluetoothLeScanner = bleManager.getBluetoothAdapter().getBluetoothLeScanner();
        bluetoothLeScanner.startScan(scanCallback);
        
        // 10秒后停止扫描
        handler.postDelayed(this::stopScan, SCAN_PERIOD);
    }
    
    private void stopScan() {
        if (!isScanning) return;
        
        isScanning = false;
        btnScan.setText("开始扫描");
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "需要蓝牙权限才能使用此功能", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    @Override
    public void onDeviceConnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "设备已连接", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    
    @Override
    public void onDeviceDisconnected() {
        runOnUiThread(() -> 
            Toast.makeText(this, "设备已断开连接", Toast.LENGTH_SHORT).show());
    }
    
    @Override
    public void onDataReceived(String data) {
        runOnUiThread(() -> 
            Toast.makeText(this, "收到数据: " + data, Toast.LENGTH_SHORT).show());
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }
    
    @Override
    public void onDeviceFound(BluetoothDevice device) {
        runOnUiThread(() -> {
            // 只显示HM-10设备（可以根据设备名称或地址过滤）
            if (device.getName() != null && device.getName().contains("HM-10")) {
                deviceAdapter.addDevice(device);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        bleManager.disconnect();
    }
} 