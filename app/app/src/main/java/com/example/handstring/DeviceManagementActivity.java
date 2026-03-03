package com.example.handstring;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.handstring.manager.BLEManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class DeviceManagementActivity extends AppCompatActivity implements BLEManager.BluetoothCallback {
    private static final int PERMISSION_REQUEST_CODE = 1;

    private BLEManager bleManager;
    private EditText bluetoothNameInput;
    private MaterialButton btnSetName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_management);

        // 初始化蓝牙管理器
        bleManager = BLEManager.getInstance(this);
        bleManager.setCallback(this);

        // 初始化UI
        bluetoothNameInput = findViewById(R.id.bluetoothNameInput);
        btnSetName = findViewById(R.id.btnSetName);

        btnSetName.setOnClickListener(v -> {
            String newName = bluetoothNameInput.getText().toString();
            if (!newName.isEmpty()) {
                // 设置蓝牙名称
                if (bleManager != null) {
                    bleManager.setBluetoothName(newName);
                }
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
            // 处理扫描到的设备
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.disconnect();
    }
} 