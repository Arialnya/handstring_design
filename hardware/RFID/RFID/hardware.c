#include <SPI.h>
#include <MFRC522.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include "MAX30105.h"
#include "spo2_algorithm.h"
#include <WiFi.h>

// ESP32 引脚定义
#define RST_PIN 27     // RFID 复位引脚
#define SS_PIN 5       // RFID 片选引脚
#define ONE_WIRE_BUS 4 // DS18B20 数据引脚
#define SDA 21
#define SCL 22
#define RESET_PIN_SPO2 14

// BLE服务UUID
#define SERVICE_UUID "0000FFE0-0000-1000-8000-00805F9B34FB"
#define CHARACTERISTIC_UUID "0000FFE1-0000-1000-8000-00805F9B34FB"

// BLE全局变量
BLEServer *pServer;
BLEService *pService;
BLECharacteristic *pCharacteristic;
bool deviceConnected = false;
uint32_t lastSendTime = 0;

// 数据结构体
struct RFIDData
{
    String cardUID = "";
    String firstName = "";
    String lastName = "";
    float temperature = 0.0;
    String mode = "read";
};

RFIDData currentData;

// BLE回调类
class MyServerCallbacks : public BLEServerCallbacks
{
    void onConnect(BLEServer *pServer)
    {
        deviceConnected = true;
        Serial.println("BLE设备已连接");
    }

    void onDisconnect(BLEServer *pServer)
    {
        deviceConnected = false;
        Serial.println("BLE设备断开");
        pServer->startAdvertising();
    }
};

// 血氧相关变量
uint32_t irBuffer[100];    // 红外数据缓冲区（用于血氧计算）
uint32_t redBuffer[100];   // 红光数据缓冲区（用于血氧计算）
int32_t spo2;              // 血氧饱和度值 (SPO2%)
int8_t validSPO2;          // 血氧值有效性标志 (1=有效)
int32_t heartRateSPO2;     // 血氧算法计算的心率（仅作参考）
int8_t validHeartRateSPO2; // 血氧心率有效性标志 (1=有效)

// 系统状态变量
bool fingerDetected = false;                // 是否检测到手指
unsigned long lastUpdateTime = 0;           // 数据上次输出时间
const unsigned long UPDATE_INTERVAL = 1000; // 数据输出间隔 (ms)
const unsigned long NO_DATA_TIMEOUT = 5000; // 无数据超时时间 (ms)
unsigned long lastDataUpdate = 0;           // 最后一次有效数据更新时间

MAX30105 particleSensor;

// 当前模式："read" 表示读取模式，"write" 表示写入模式
String currentMode = "read";

// 标记是否需要发送药品信息
bool sendMedicationInfo = false;
// 药品信息结构体
struct MedicationInfo
{
    String medicineName;
    String dosage;
    String scheduledTime;
    String actualTime;
    bool taken;
};
MedicationInfo medication;

// 生成JSON数据
String generateJsonData()
{
    StaticJsonDocument<512> doc;
    doc["heartRate"] = heartRateSPO2;
    doc["bloodOxygen"] = spo2;
    doc["temperature"] = currentData.temperature;

    if (sendMedicationInfo)
    {
        JsonObject medicationObj = doc.createNestedObject("medication");
        medicationObj["medicineName"] = medication.medicineName;
        medicationObj["dosage"] = medication.dosage;
        medicationObj["scheduledTime"] = medication.scheduledTime;
        medicationObj["actualTime"] = medication.actualTime;
        medicationObj["taken"] = medication.taken;
        sendMedicationInfo = false; // 发送后重置标记
    }

    String jsonString;
    serializeJson(doc, jsonString);
    return jsonString;
}

// 更新RFID数据
void updateRFIDData(String uid, String fName, String lName, float temp, String mode)
{
    currentData.cardUID = uid;
    currentData.firstName = fName;
    currentData.lastName = lName;
    currentData.temperature = temp;
    currentData.mode = mode;
}

// 初始化 RFID 和温度传感器
MFRC522 mfrc522(SS_PIN, RST_PIN);    // 创建 MFRC522 实例
OneWire oneWire(ONE_WIRE_BUS);       // 初始化 OneWire 总线
DallasTemperature sensors(&oneWire); // 传递 OneWire 引用给 DallasTemperature

const char *ssid = "ESP32";        // wifi名
const char *password = "12345678"; // wifi密码

const IPAddress serverIP(192, 168, 1, 100); // 欲访问的服务端IP地址
uint16_t serverPort = 8080;                 // 服务端口号

WiFiClient client; // 声明一个ESP32客户端对象，用于与服务器进行连接

// 模拟传感器数据（实际使用时替换为真实传感器读取）
float getTemperature()
{
    return 25.0 + random(-10, 10) / 10.0;
}

int getHumidity()
{
    return 50 + random(-20, 20);
}

String getAirQuality()
{
    return random(100) > 20 ? "良好" : "一般";
}

int getLightIntensity()
{
    return 300 + random(-200, 200);
}

void setup()
{
    Serial.begin(115200);
    Serial.println();

    WiFi.mode(WIFI_STA);
    WiFi.setSleep(false); // 关闭STA模式下wifi休眠，提高响应速度
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
        Serial.print(".");
    }
    Serial.println("Connected");
    Serial.print("IP Address:");
    Serial.println(WiFi.localIP());

    // 初始化BLE
    BLEDevice::init("ESP32-HealthMonitor");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    pService = pServer->createService(SERVICE_UUID);
    pCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
            BLECharacteristic::PROPERTY_NOTIFY);
    pCharacteristic->addDescriptor(new BLE2902());
    pService->start();

    // 开始广播
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMinPreferred(0x12);
    BLEDevice::startAdvertising();

    // 初始化 RFID
    SPI.begin();
    mfrc522.PCD_Init();

    // 初始化温度传感器
    sensors.begin();

    // 初始化复位引脚
    pinMode(RESET_PIN_SPO2, OUTPUT);
    digitalWrite(RESET_PIN_SPO2, LOW);
    delay(100);
    digitalWrite(RESET_PIN_SPO2, HIGH);
    delay(100);

    // 初始化I2C
    Wire.begin(SDA, SCL, 400000);

    // 初始化传感器
    if (!particleSensor.begin(Wire, I2C_SPEED_FAST))
    {
        Serial.println("未找到MAX30102传感器! 请检查连接");
        while (1)
            ;
    }
    // 传感器配置
    particleSensor.setup(55, 4, 2, 400, 411, 4096); // 400Hz采样率
    particleSensor.setPulseAmplitudeRed(0x1F);      // 红光LED亮度
    particleSensor.setPulseAmplitudeIR(0x1F);       // 红外LED亮度

    // 预填充血氧算法缓冲区，并探测初始是否有手指
    for (int i = 0; i < 100; i++)
    {
        while (!particleSensor.available())
        {
            particleSensor.check();
        }
        redBuffer[i] = (uint32_t)particleSensor.getRed();
        irBuffer[i] = (uint32_t)particleSensor.getIR();
        particleSensor.nextSample();

        // 当IR信号大于50000时认为有手指
        if (irBuffer[i] > 50000)
        {
            fingerDetected = true;
        }
    }

    Serial.println("RFID 读写程序已启动！");
    Serial.println("BLE服务已启动，等待客户端连接...");
    Serial.println("请输入以下指令:");
    Serial.println("mode=read   - 切换到读取模式");
    Serial.println("mode=write  - 切换到写入模式");
    Serial.println("temp        - 读取当前温度");
    Serial.println("当前模式：读取");
}

void resetSensor()
{
    // 静默复位传感器
    digitalWrite(RESET_PIN_SPO2, LOW);
    delay(100);
    digitalWrite(RESET_PIN_SPO2, HIGH);
    delay(100);
    particleSensor.begin(Wire, I2C_SPEED_FAST);
    particleSensor.setup(55, 4, 2, 400, 411, 4096);
    particleSensor.setPulseAmplitudeRed(0x1F);
    particleSensor.setPulseAmplitudeIR(0x1F);
    lastDataUpdate = millis();
}

void loop()
{
    // 检测串口指令
    if (Serial.available() > 0)
    {
        String input = Serial.readStringUntil('\n');
        input.trim();

        if (input.equals("mode=read"))
        {
            currentMode = "read";
            Serial.println("切换到读取模式");
        }
        else if (input.equals("mode=write"))
        {
            currentMode = "write";
            Serial.println("切换到写入模式");
        }
        else if (input.equals("temp"))
        {
            readTemperature(); // 读取并显示温度
        }
        else
        {
            Serial.println("无效指令！可用指令:");
            Serial.println("mode=read, mode=write, temp");
        }
    }

    // 根据当前模式调用对应函数
    if (currentMode.equals("read"))
    {
        readCard();
    }
    else if (currentMode.equals("write"))
    {
        writeCard();
    }

    // 读取IR信号值
    long irValue = particleSensor.getIR();

    // 手指检测及防抖处理
    bool currentFingerState = (irValue > 50000);
    if (currentFingerState != fingerDetected)
    {
        delay(50);
        currentFingerState = (particleSensor.getIR() > 50000);
        if (currentFingerState != fingerDetected)
        {
            fingerDetected = currentFingerState;
            if (!fingerDetected)
            {
                // 手指移开时，重置血氧数据，且不输出任何信息
                spo2 = 0;
                validSPO2 = 0;
                heartRateSPO2 = 0;
                validHeartRateSPO2 = 0;
            }
        }
    }

    // 只有检测到手指时才进行数据更新与输出
    if (fingerDetected)
    {
        // 超时处理: 若长时间无更新则复位传感器
        if (millis() - lastDataUpdate > NO_DATA_TIMEOUT)
        {
            resetSensor();
            return;
        }

        // 每秒更新血氧数据
        static unsigned long lastSPO2Update = 0;
        if (millis() - lastSPO2Update > 1000)
        {
            // 滑动缓冲区，保留最新75个样本
            for (int i = 25; i < 100; i++)
            {
                redBuffer[i - 25] = redBuffer[i];
                irBuffer[i - 25] = irBuffer[i];
            }
            // 获取25个新样本
            for (int i = 75; i < 100; i++)
            {
                while (!particleSensor.available())
                {
                    particleSensor.check();
                }
                redBuffer[i] = (uint32_t)particleSensor.getRed();
                irBuffer[i] = (uint32_t)particleSensor.getIR();
                particleSensor.nextSample();
            }

            // 计算血氧及参考心率
            maxim_heart_rate_and_oxygen_saturation(
                irBuffer, 100, redBuffer,
                &spo2, &validSPO2,
                &heartRateSPO2, &validHeartRateSPO2);

            lastSPO2Update = millis();
            lastDataUpdate = millis();
        }
    }

    // BLE数据发送
    if (deviceConnected && (millis() - lastSendTime > 2000))
    {
        sensors.requestTemperatures();
        currentData.temperature = sensors.getTempCByIndex(0);
        String jsonData = generateJsonData();
        pCharacteristic->setValue(jsonData.c_str());
        pCharacteristic->notify();
        Serial.print("BLE发送数据: ");
        Serial.println(jsonData);
        lastSendTime = millis();
    }

    delay(100); // 降低CPU占用

    Serial.println("尝试访问服务器");
    if (client.connect(serverIP, serverPort))
    {
        Serial.println("访问成功");

        // 创建JSON文档
        StaticJsonDocument<200> doc;

        // 添加传感器数据
        doc["temperature"] = getTemperature();
        doc["humidity"] = getHumidity();
        doc["airQuality"] = getAirQuality();
        doc["lightIntensity"] = getLightIntensity();

        // 序列化JSON
        String jsonString;
        serializeJson(doc, jsonString);

        // 发送数据
        client.println(jsonString);
        Serial.print("发送数据: ");
        Serial.println(jsonString);

        // 等待服务器响应
        while (client.connected() || client.available())
        {
            if (client.available())
            {
                String line = client.readStringUntil('\n');
                Serial.print("服务器响应: ");
                Serial.println(line);
            }
        }

        Serial.println("关闭当前连接");
        client.stop();
    }
    else
    {
        Serial.println("访问失败");
        client.stop();
    }

    delay(5000); // 每5秒发送一次数据
}

// 读取温度传感器数据
void readTemperature()
{
    sensors.requestTemperatures();            // 发送温度读取请求
    float tempC = sensors.getTempCByIndex(0); // 获取第一个传感器的温度(°C)

    if (tempC != DEVICE_DISCONNECTED_C)
    {
        Serial.print("当前温度: ");
        Serial.print(tempC);
        Serial.println(" °C");
    }
    else
    {
        Serial.println("错误: 无法读取温度传感器数据");
    }
}

void readCard()
{
    // 预设默认密钥（6 字节全为 0xFF）
    MFRC522::MIFARE_Key key;
    for (byte i = 0; i < 6; i++)
    {
        key.keyByte[i] = 0xFF;
    }

    // 检查是否有新卡片
    if (!mfrc522.PICC_IsNewCardPresent())
    {
        return;
    }

    // 读取卡片序列号
    if (!mfrc522.PICC_ReadCardSerial())
    {
        return;
    }

    Serial.println("**检测到卡片**");

    // 构建卡片UID字符串
    String cardUID = "";
    for (byte i = 0; i < mfrc522.uid.size; i++)
    {
        if (mfrc522.uid.uidByte[i] < 0x10)
            cardUID += "0";
        cardUID += String(mfrc522.uid.uidByte[i], HEX);
    }

    // 读取温度
    sensors.requestTemperatures();
    float tempC = sensors.getTempCByIndex(0);

    // 读取并输出姓名数据
    Serial.print("名字: ");
    byte buffer1[18]; // 存放名字数据（Block 4）
    byte buffer2[18]; // 存放姓氏数据（Block 1）
    byte len = 18;
    MFRC522::StatusCode status;

    // 读取名字（Block 4）
    status = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, 4, &key, &(mfrc522.uid));
    if (status != MFRC522::STATUS_OK)
    {
        Serial.print("认证失败: ");
        Serial.println(mfrc522.GetStatusCodeName(status));
        return;
    }
    status = mfrc522.MIFARE_Read(4, buffer1, &len);
    if (status != MFRC522::STATUS_OK)
    {
        Serial.print("读取失败: ");
        Serial.println(mfrc522.GetStatusCodeName(status));
        return;
    }

    // 输出名字（跳过空格）
    for (uint8_t i = 0; i < 16; i++)
    {
        if (buffer1[i] != 32)
        { // ASCII 32为空格
            Serial.write(buffer1[i]);
        }
    }
    Serial.print(" ");

    // 读取姓氏（Block 1）
    status = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, 1, &key, &(mfrc522.uid));
    if (status != MFRC522::STATUS_OK)
    {
        Serial.print("认证失败: ");
        Serial.println(mfrc522.GetStatusCodeName(status));
        return;
    }
    status = mfrc522.MIFARE_Read(1, buffer2, &len);
    if (status != MFRC522::STATUS_OK)
    {
        Serial.print("读取失败: ");
        Serial.println(mfrc522.GetStatusCodeName(status));
        return;
    }

    // 输出姓氏
    for (uint8_t i = 0; i < 16; i++)
    {
        Serial.write(buffer2[i]);
    }

    // 更新BLE数据
    updateRFIDData(cardUID, String((char *)buffer1), String((char *)buffer2), tempC, currentMode);

    // 模拟获取药品信息
    medication.medicineName = "阿司匹林";
    medication.dosage = "100mg";
    medication.scheduledTime = "2024-03-20T08:00:00Z";
    medication.actualTime = "2024-03-20T08:05:00Z";
    medication.taken = true;
    sendMedicationInfo = true;

    Serial.println("\n**读取结束**\n");

    delay(1000);               // 读取间隔
    mfrc522.PICC_HaltA();      // 停止卡片通信
    mfrc522.PCD_StopCrypto1(); // 停止加密
}

void writeCard()
{
    // 预设默认密钥（6 字节全为 0xFF）
    MFRC522::MIFARE_Key key;
    for (byte i = 0; i < 6; i++)
    {
        key.keyByte[i] = 0xFF;
    }

    // 检查是否有新卡片
    if (!mfrc522.PICC_IsNewCardPresent())
    {
        return;
    }

    // 读取卡片序列号
    if (!mfrc522.PICC_ReadCardSerial())
    {
        return;
    }

    // 输出卡片 UID 以及卡片类型
    Serial.print("卡片UID:");
    for (byte i = 0; i < mfrc522.uid.size; i++)
    {
        Serial.print(mfrc522.uid.uidByte[i] < 0x10 ? " 0" : " ");
        Serial.print(mfrc522.uid.uidByte[i], HEX);
    }
    Serial.print(" 卡片类型: ");
    MFRC522::PICC_Type piccType = mfrc522.PICC_GetType(mfrc522.uid.sak);
    Serial.println(mfrc522.PICC_GetTypeName(piccType));

    byte buffer[34]; // 用于存储输入数据，最大 34 字节
    byte block;
    MFRC522::StatusCode status;
    byte len;

    // 设置串口输入超时为 20 秒
    Serial.setTimeout(20000L);

    // 输入名字（以 # 结束）
    Serial.println("请输入名字，以#结束");
    len = Serial.readBytesUntil('#', (char *)buffer, 30);
    // 若输入不足 30 字节，则后面补空格
    for (byte i = len; i < 30; i++)
    {
        buffer[i] = ' ';
    }

    // 写入 Block 1（写入姓氏的前 16 字节）
    block = 1;
    status = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, block, &key, &(mfrc522.uid));
    if (status != MFRC522::STATUS_OK)
    {
        Serial.print("认证失败: ");
        Serial.println(mfrc522.GetStatusCodeName(status));
        return;
    }
    status = mfrc522.MIFARE_Write(block, buffer, 16);
    if (status != MFRC522::STATUS_OK)
    {
        Serial.print("写入失败: ");
        Serial.println(mfrc522.GetStatusCodeName(status));
        return;
    }

    // 写入 Block 2（写入姓氏的后 16 字节）
    block = 2;
    status = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, block, &key, &(mfrc522.uid));
    status = mfrc522.MIFARE_Write(block, &buffer[16], 16);
    if (status != MFRC522::STATUS_OK)
    {
        Serial.print("写入失败: ");
        Serial.println(mfrc522.GetStatusCodeName(status));
        return;
    }

    // 输入名字（以 # 结束）
    Serial.println("请输入姓氏，以#结束（若您命名的是药品，可以空出此项）");
    len = Serial.readBytesUntil('#', (char *)buffer, 20);
    for (byte i = len; i < 20; i++)
    {
        buffer[i] = ' ';
    }

    // 写入 Block 4（写入名字的前 16 字节）
    block = 4;
    status = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, block, &key, &(mfrc522.uid));
    status = mfrc522.MIFARE_Write(block, buffer, 16);
    if (status != MFRC522::STATUS_OK)
    {
        Serial.print("写入失败: ");
        Serial.println(mfrc522.GetStatusCodeName(status));
        return;
    }

    // 写入 Block 5（写入名字的后 16 字节）
    block = 5;
    status = mfrc522.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, block, &key, &(mfrc522.uid));
    status = mfrc522.MIFARE_Write(block, &buffer[16], 16);
    if (status != MFRC522::STATUS_OK)
    {
        Serial.print("写入失败: ");
        Serial.println(mfrc522.GetStatusCodeName(status));
        return;
    }

    // 构建卡片UID字符串
    String cardUID = "";
    for (byte i = 0; i < mfrc522.uid.size; i++)
    {
        if (mfrc522.uid.uidByte[i] < 0x10)
            cardUID += "0";
        cardUID += String(mfrc522.uid.uidByte[i], HEX);
    }

    // 读取温度
    sensors.requestTemperatures();
    float tempC = sensors.getTempCByIndex(0);

    // 更新BLE数据
    updateRFIDData(cardUID, String((char *)buffer), String((char *)&buffer[16]), tempC, currentMode);

    // 模拟获取药品信息
    medication.medicineName = "阿司匹林";
    medication.dosage = "100mg";
    medication.scheduledTime = "2024-03-20T08:00:00Z";
    medication.actualTime = "2024-03-20T08:05:00Z";
    medication.taken = true;
    sendMedicationInfo = true;

    Serial.println("写入成功！");

    mfrc522.PICC_HaltA();      // 停止卡片通信
    mfrc522.PCD_StopCrypto1(); // 停止加密

    delay(1000);
}