#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <ArduinoJson.h>

// BLE服务UUID (与Android端匹配)
#define SERVICE_UUID        "0000FFE0-0000-1000-8000-00805F9B34FB"
#define CHARACTERISTIC_UUID "0000FFE1-0000-1000-8000-00805F9B34FB"

// 全局变量
BLEServer *pServer;
BLEService *pService;
BLECharacteristic *pCharacteristic;
bool deviceConnected = false;
uint32_t lastSendTime = 0;

// 传感器数据结构体（预留修改点）
struct SensorData {
    int heartRate = 72;       // 默认心率
    int bloodOxygen = 98;     // 默认血氧
    float temperature = 36.5; // 默认温度
    // 添加更多传感器字段...
};

SensorData currentData; // 当前传感器数据

// BLE回调类
class MyServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
        Serial.print("设备已连接 - 客户端地址: ");
        Serial.println(pServer->getPeerAddress().toString().c_str());
        Serial.print("客户端名称: ");
        Serial.println(pServer->getPeerName().c_str());
        Serial.print("连接间隔: ");
        Serial.println(pServer->getConnectionInterval());
    }

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.print("设备断开 - 客户端地址: ");
        Serial.println(pServer->getPeerAddress().toString().c_str());
        Serial.println("重新开始广播...");
        pServer->startAdvertising();
    }
};

// 生成JSON数据
String generateJsonData() {
    StaticJsonDocument<256> doc;
    doc["heartRate"] = currentData.heartRate;
    doc["bloodOxygen"] = currentData.bloodOxygen;
    doc["temperature"] = currentData.temperature;
    // 添加更多字段...

    String jsonString;
    serializeJson(doc, jsonString);
    return jsonString;
}

// 修改传感器数据的接口
void updateSensorData(int hr, int spo2, float temp) {
    currentData.heartRate = hr;
    currentData.bloodOxygen = spo2;
    currentData.temperature = temp;
    // 更新其他字段...
}

void setup() {
    Serial.begin(115200);
    Serial.println("\n启动BLE服务...");
    Serial.println("设备名称: ESP32-HealthMonitor");
    Serial.print("服务UUID: ");
    Serial.println(SERVICE_UUID);
    Serial.print("特征值UUID: ");
    Serial.println(CHARACTERISTIC_UUID);

    // 初始化BLE
    BLEDevice::init("ESP32-HealthMonitor");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    pService = pServer->createService(SERVICE_UUID);
    pCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_NOTIFY
    );
    pCharacteristic->addDescriptor(new BLE2902());
    pService->start();

    // 开始广播
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinInterval(0x06);  // 7.5ms
    pAdvertising->setMaxInterval(0x12);  // 30ms
    pAdvertising->setName("ESP32-HealthMonitor");
    BLEDevice::startAdvertising();
    
    Serial.println("BLE服务已启动");
    Serial.println("等待客户端连接...");
}

void loop() {
    if (deviceConnected) {
        if (millis() - lastSendTime > 5000) {
            String jsonData = generateJsonData();
            pCharacteristic->setValue(jsonData.c_str());
            pCharacteristic->notify();
            Serial.print("发送数据: ");
            Serial.println(jsonData);
            lastSendTime = millis();
        }
    } else {
        // 未连接时打印状态
        static uint32_t lastStatusTime = 0;
        if (millis() - lastStatusTime > 10000) {  // 每10秒打印一次状态
            Serial.println("等待连接...");
            lastStatusTime = millis();
        }
    }
    delay(100);  // 降低CPU占用
}