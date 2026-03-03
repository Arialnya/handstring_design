package com.example.handstring.manager;

// 原 import 保持不变
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.UUID;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

public class BLEManager {
    private static final String TAG = "BLEManager";
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final UUID HM10_SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID HM10_CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic writeCharacteristic;

    private static BLEManager instance;

    private Handler connectionTimeoutHandler = new Handler(Looper.getMainLooper());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean isConnected = false;
    private String targetDeviceAddress = null;

    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;

    public interface BluetoothCallback {
        void onDeviceConnected();
        void onDeviceDisconnected();
        void onDataReceived(String data);
        void onError(String error);
        void onDeviceFound(BluetoothDevice device);
    }

    private BluetoothCallback callback;

    private BLEManager(Context context) {
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }


    public static synchronized BLEManager getInstance(Context context) {
        if (instance == null) {
            instance = new BLEManager(context);
        }
        return instance;
    }

    public void setCallback(BluetoothCallback callback) {
        this.callback = callback;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    private void startConnectionTimeout() {
        connectionTimeoutHandler.postDelayed(() -> {
            if (!isConnected) {
                Log.e(TAG, "连接超时");
                disconnect();
                if (callback != null) callback.onError("连接超时");
            }
        }, CONNECTION_TIMEOUT);
    }

    public void connectToDevice(BluetoothDevice device) {
        if (device == null) return;

        String deviceAddress = device.getAddress();
        String deviceName = device.getName();
        Log.d(TAG, String.format("尝试连接设备 - 地址: %s, 名称: %s", deviceAddress, deviceName));

        if (!"ESP32-HealthMonitor".equals(deviceName)) {
            Log.e(TAG, "设备名称不匹配");
            if (callback != null) callback.onError("设备名称不匹配");
            return;
        }

        targetDeviceAddress = deviceAddress;

        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        startConnectionTimeout();

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (callback != null) callback.onError("缺少蓝牙连接权限");
            return;
        }

        bluetoothGatt = device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                String connectedAddress = gatt.getDevice().getAddress();
                Log.d(TAG, String.format("连接状态改变 - 设备: %s, 目标设备: %s, 状态: %d, 新状态: %d",
                        connectedAddress, targetDeviceAddress, status, newState));

                connectionTimeoutHandler.removeCallbacksAndMessages(null);

                if (!connectedAddress.equals(targetDeviceAddress)) {
                    Log.e(TAG, "连接到错误的设备");
                    gatt.disconnect();
                    return;
                }

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "连接失败，状态码: " + status);
                    gatt.close();
                    if (callback != null) callback.onError("连接失败: " + status);
                    return;
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "已连接到设备: " + gatt.getDevice().getName());
                    isConnected = true;
                    gatt.requestMtu(512);
                    gatt.discoverServices();
                    if (callback != null) mainHandler.post(callback::onDeviceConnected);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "设备已断开连接");
                    isConnected = false;
                    if (callback != null) mainHandler.post(callback::onDeviceDisconnected);
                }
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                Log.d(TAG, "MTU 已更改: " + mtu + ", 状态: " + status);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = gatt.getService(HM10_SERVICE_UUID);
                    if (service != null) {
                        writeCharacteristic = service.getCharacteristic(HM10_CHARACTERISTIC_UUID);
                        if (writeCharacteristic != null) {
                            gatt.setCharacteristicNotification(writeCharacteristic, true);

                            BluetoothGattDescriptor descriptor = writeCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                if (HM10_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                    byte[] data = characteristic.getValue();
                    String receivedData = new String(data);
                    Log.d(TAG, "收到数据: " + receivedData);
                    if (callback != null) mainHandler.post(() -> callback.onDataReceived(receivedData));
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "数据写入失败，状态码: " + status);
                    if (callback != null) mainHandler.post(() -> callback.onError("数据发送失败"));
                }
            }
        }, BluetoothDevice.TRANSPORT_LE);
    }

    public void sendData(String data) {
        if (!isConnected || writeCharacteristic == null) {
            if (callback != null) callback.onError("设备未连接");
            return;
        }

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (callback != null) callback.onError("缺少蓝牙权限");
            return;
        }

        writeCharacteristic.setValue(data.getBytes());
        bluetoothGatt.writeCharacteristic(writeCharacteristic);
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
            if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
            }
        }
        isConnected = false;
        targetDeviceAddress = null;
    }

    public boolean isConnected() {
        return bluetoothGatt != null && isConnected;
    }

    public void startScan() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            if (callback != null) callback.onError("缺少蓝牙扫描权限");
            return;
        }

        if (bluetoothAdapter == null) return;

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) return;

        if (scanCallback == null) {
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    BluetoothDevice device = result.getDevice();
                    if (device != null && "ESP32-HealthMonitor".equals(device.getName())) {
                        if (callback != null) {
                            mainHandler.post(() -> callback.onDeviceFound(device));
                        }
                    }
                }
            };
        }

        bluetoothLeScanner.startScan(scanCallback);
    }

    public void stopScan() {
        if (bluetoothLeScanner != null && scanCallback != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    public void setBluetoothName(String name) {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (callback != null) callback.onError("缺少蓝牙权限");
            return;
        }
        bluetoothAdapter.setName(name);
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
        }
    }
}
