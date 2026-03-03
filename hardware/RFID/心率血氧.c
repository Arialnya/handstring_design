#include <Wire.h>
#include "MAX30105.h"
#include "spo2_algorithm.h"

// ESP8266 NodeMCU 引脚定义
#define SDA D2     // GPIO4
#define SCL D1     // GPIO5
#define RESET_PIN D5 // GPIO14

MAX30105 particleSensor;

// 血氧相关变量
uint32_t irBuffer[100];   // 红外数据缓冲区（用于血氧计算）
uint32_t redBuffer[100];  // 红光数据缓冲区（用于血氧计算）
int32_t spo2;             // 血氧饱和度值 (SPO2%)
int8_t validSPO2;         // 血氧值有效性标志 (1=有效)
int32_t heartRateSPO2;    // 血氧算法计算的心率（仅作参考）
int8_t validHeartRateSPO2;// 血氧心率有效性标志 (1=有效)

// 系统状态变量
bool fingerDetected = false;           // 是否检测到手指
unsigned long lastUpdateTime = 0;        // 数据上次输出时间
const unsigned long UPDATE_INTERVAL = 1000; // 数据输出间隔 (ms)
const unsigned long NO_DATA_TIMEOUT = 5000;   // 无数据超时时间 (ms)
unsigned long lastDataUpdate = 0;        // 最后一次有效数据更新时间

void setup() {
  Serial.begin(115200);
  
  // 初始化复位引脚
  pinMode(RESET_PIN, OUTPUT);
  digitalWrite(RESET_PIN, LOW);
  delay(100);
  digitalWrite(RESET_PIN, HIGH);
  delay(100);

  // 初始化I2C
  Wire.begin(SDA, SCL);
  Wire.setClock(400000);

  // 初始化传感器
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
    Serial.println("未找到MAX30102传感器! 请检查连接");
    while (1);
  }

  // 传感器配置
  particleSensor.setup(55, 4, 2, 400, 411, 4096); // 400Hz采样率
  particleSensor.setPulseAmplitudeRed(0x1F);       // 红光LED亮度
  particleSensor.setPulseAmplitudeIR(0x1F);        // 红外LED亮度

  // 预填充血氧算法缓冲区，并探测初始是否有手指
  for (int i = 0; i < 100; i++) {
    while (!particleSensor.available()) {
      particleSensor.check();
    }
    redBuffer[i] = (uint32_t)particleSensor.getRed();
    irBuffer[i] = (uint32_t)particleSensor.getIR();
    particleSensor.nextSample();
    
    // 当IR信号大于50000时认为有手指
    if (irBuffer[i] > 50000) {
      fingerDetected = true;
    }
  }
  // 所有提示信息均不输出
}

void resetSensor() {
  // 静默复位传感器
  digitalWrite(RESET_PIN, LOW);
  delay(100);
  digitalWrite(RESET_PIN, HIGH);
  delay(100);
  particleSensor.begin(Wire, I2C_SPEED_FAST);
  particleSensor.setup(55, 4, 2, 400, 411, 4096);
  particleSensor.setPulseAmplitudeRed(0x1F);
  particleSensor.setPulseAmplitudeIR(0x1F);
  lastDataUpdate = millis();
}

void loop() {
  // 读取IR信号值
  long irValue = particleSensor.getIR();

  // 手指检测及防抖处理
  bool currentFingerState = (irValue > 50000);
  if (currentFingerState != fingerDetected) {
    delay(50);
    currentFingerState = (particleSensor.getIR() > 50000);
    if (currentFingerState != fingerDetected) {
      fingerDetected = currentFingerState;
      if (!fingerDetected) {
        // 手指移开时，重置血氧数据，且不输出任何信息
        spo2 = 0;
        validSPO2 = 0;
        heartRateSPO2 = 0;
        validHeartRateSPO2 = 0;
      }
    }
  }

  // 只有检测到手指时才进行数据更新与输出
  if (fingerDetected) {
    // 超时处理: 若长时间无更新则复位传感器
    if (millis() - lastDataUpdate > NO_DATA_TIMEOUT) {
      resetSensor();
      return;
    }
    
    // 每秒更新血氧数据
    static unsigned long lastSPO2Update = 0;
    if (millis() - lastSPO2Update > 1000) {
      // 滑动缓冲区，保留最新75个样本
      for (int i = 25; i < 100; i++) {
        redBuffer[i - 25] = redBuffer[i];
        irBuffer[i - 25] = irBuffer[i];
      }
      // 获取25个新样本
      for (int i = 75; i < 100; i++) {
        while (!particleSensor.available()) {
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
          &heartRateSPO2, &validHeartRateSPO2
      );
      
      lastSPO2Update = millis();
      lastDataUpdate = millis();
    }
    
    // 每秒输出一次数据
    if (millis() - lastUpdateTime > UPDATE_INTERVAL) {
      Serial.print("红外信号=");
      Serial.print(irValue);
  
      // 若血氧数据有效，显示血氧及对应心率；否则两项都显示'---'
      if (validSPO2 == 1) {
        Serial.print(" | 血氧=");
        Serial.print(spo2);
        Serial.print("%");
        Serial.print(" | 血氧心率=");
        if (validHeartRateSPO2 == 1) {
          Serial.print(heartRateSPO2);
        } else {
          Serial.print("---");
        }
      } else {
        Serial.print(" | 血氧=---");
        Serial.print(" | 血氧心率=---");
      }
  
      Serial.println();
      lastUpdateTime = millis();
    }
  }
  
  // 未检测到手指时不进行任何输出
  
  delay(10);
}
