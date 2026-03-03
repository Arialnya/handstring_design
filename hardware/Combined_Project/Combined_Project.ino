#include <SPI.h>
#include <Wire.h>
#include <MFRC522.h>
#include "MAX30105.h"
#include "heartRate.h"

// RFID 引脚定义
#define SS_PIN    53  // MEGA 的默认 SS 引脚
#define RST_PIN   9   // RFID 复位引脚

// 心率传感器引脚定义
#define SDA_PIN   20  // I2C SDA
#define SCL_PIN   21  // I2C SCL
#define RESET_PIN 8   // 心率传感器复位引脚

// 创建传感器实例
MFRC522 rfid(SS_PIN, RST_PIN);
MAX30105 particleSensor;
MFRC522::MIFARE_Key key;

// RFID 相关变量
byte nuidPICC[4]; // 存储卡片的 NUID
bool cardDetected = false;
String lastCardUID = "";

// 心率相关变量
const byte RATE_SIZE = 4;
byte rates[RATE_SIZE];
byte rateSpot = 0;
long lastBeat = 0;
long lastDataUpdate = 0;
float beatsPerMinute = 0;
int beatAvg = 0;
const int MIN_HEART_RATE = 40;
bool fingerDetected = false;
bool hadValidData = false;

// 系统状态
enum SystemState {
  WAITING_FOR_CARD,    // 等待刷卡
  WAITING_FOR_FINGER,  // 等待手指放置
  MEASURING_HEART_RATE // 测量心率
};
SystemState currentState = WAITING_FOR_CARD;

void setup() {
  Serial.begin(115200);
  Serial.println(F("系统初始化中..."));

  // 初始化 RFID
  SPI.begin();
  rfid.PCD_Init();
  for (byte i = 0; i < 6; i++) {
    key.keyByte[i] = 0xFF;
  }

  // 初始化心率传感器
  Wire.begin(SDA_PIN, SCL_PIN);
  pinMode(RESET_PIN, OUTPUT);
  digitalWrite(RESET_PIN, LOW);

  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
    Serial.println(F("未找到 MAX30105 传感器，请检查接线/电源。"));
    while (1);
  }

  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x0A);
  particleSensor.setPulseAmplitudeGreen(0);

  Serial.println(F("系统初始化完成！"));
  Serial.println(F("请刷卡开始测量..."));
}

void resetHeartRateSensor() {
  Serial.println(F("重置心率传感器..."));
  digitalWrite(RESET_PIN, HIGH);
  delay(100);
  digitalWrite(RESET_PIN, LOW);
  
  particleSensor.begin(Wire, I2C_SPEED_FAST);
  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x0A);
  particleSensor.setPulseAmplitudeGreen(0);
  
  lastDataUpdate = millis();
}

void handleRFID() {
  if (!rfid.PICC_IsNewCardPresent()) return;
  if (!rfid.PICC_ReadCardSerial()) return;

  // 检查是否是新卡片
  if (rfid.uid.uidByte[0] != nuidPICC[0] || 
      rfid.uid.uidByte[1] != nuidPICC[1] || 
      rfid.uid.uidByte[2] != nuidPICC[2] || 
      rfid.uid.uidByte[3] != nuidPICC[3]) {
    
    // 存储新卡片的 NUID
    for (byte i = 0; i < 4; i++) {
      nuidPICC[i] = rfid.uid.uidByte[i];
    }

    // 生成卡片 UID 字符串
    String cardUID = "";
    for (byte i = 0; i < rfid.uid.size; i++) {
      cardUID += String(rfid.uid.uidByte[i], HEX);
    }
    lastCardUID = cardUID;

    Serial.print(F("检测到新卡片，UID: "));
    Serial.println(cardUID);
    
    currentState = WAITING_FOR_FINGER;
    Serial.println(F("请将手指放在心率传感器上..."));
  }

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
}

void handleHeartRate() {
  long irValue = particleSensor.getIR();
  bool currentFingerState = irValue > 50000;
  
  if (currentFingerState != fingerDetected) {
    delay(50);
    currentFingerState = particleSensor.getIR() > 50000;
    fingerDetected = currentFingerState;
    
    if (fingerDetected) {
      currentState = MEASURING_HEART_RATE;
      Serial.println(F("开始测量心率..."));
    }
  }

  if (checkForBeat(irValue) == true && fingerDetected) {
    long delta = millis() - lastBeat;
    lastBeat = millis();

    float newBPM = 60 / (delta / 1000.0);
    if (newBPM > MIN_HEART_RATE && newBPM < 255) {
      beatsPerMinute = newBPM;
      hadValidData = true;
      
      rates[rateSpot++] = (byte)beatsPerMinute;
      rateSpot %= RATE_SIZE;

      beatAvg = 0;
      for (byte x = 0; x < RATE_SIZE; x++)
        beatAvg += rates[x];
      beatAvg /= RATE_SIZE;
      
      lastDataUpdate = millis();

      // 输出测量结果
      Serial.print(F("卡片 UID: "));
      Serial.print(lastCardUID);
      Serial.print(F(", 当前心率: "));
      Serial.print(beatsPerMinute);
      Serial.print(F(" BPM, 平均心率: "));
      Serial.print(beatAvg);
      Serial.println(F(" BPM"));
    }
  }

  // 检查是否需要重置传感器
  if (fingerDetected && (millis() - lastDataUpdate > 5000)) {
    resetHeartRateSensor();
  }

  // 如果手指移开，返回等待卡片状态
  if (!fingerDetected && currentState == MEASURING_HEART_RATE) {
    currentState = WAITING_FOR_CARD;
    Serial.println(F("测量结束，请刷卡开始新的测量..."));
  }
}

void loop() {
  switch (currentState) {
    case WAITING_FOR_CARD:
      handleRFID();
      break;
      
    case WAITING_FOR_FINGER:
      handleHeartRate();
      break;
      
    case MEASURING_HEART_RATE:
      handleHeartRate();
      break;
  }
  
  delay(20); // 防止串口输出过快
} 