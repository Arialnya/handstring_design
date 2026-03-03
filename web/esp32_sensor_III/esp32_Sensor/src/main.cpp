#include <Arduino.h>
#include <WiFi.h>
#include <ESPAsyncWebServer.h>
#include "HCSR04.h"
#include <DHT.h>
#include <SPIFFS.h>
#include <ArduinoJson.h>

// 热点配置
const char* ssid = "ESP32_AP";
const char* password = "12345678";  // 热点密码，至少8个字符

// 创建 Web 服务器
AsyncWebServer server(80);

// DHT11 配置
#define DHTPIN 13
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// 超声波传感器配置
#define TRIG_PIN 12
#define ECHO_PIN 14
UltraSonicDistanceSensor sensor(TRIG_PIN, ECHO_PIN);

// 全局传感器数据变量
float humidity = 0, temperature = 0, distance = 0;

// 全局阈值变量（初始值，可根据需要调整）
float thresholdTemp = 30.0;   // 温度上限 (°C)
float thresholdHumid = 60.0;    // 湿度上限 (%)
float thresholdDist = 5.0;     // 距离阈值 (cm)

// 读取传感器数据
void readSensors() {
  humidity = dht.readHumidity();
  temperature = dht.readTemperature();
  distance = sensor.measureDistanceCm(temperature);
  if (isnan(humidity) || isnan(temperature)) {
    humidity = -1;
    temperature = -1;
  }
  if (distance < 0) {
    distance = -1;
  }
}

void setup() {
  Serial.begin(115200);
  delay(100);

  // 初始化 SPIFFS 文件系统
  if (!SPIFFS.begin(true)) {
    Serial.println("SPIFFS 文件系统挂载失败！");
    while (true);
  }

  // 初始化 DHT11
  dht.begin();

  // 配置为 Wi-Fi 热点（AP 模式）
  WiFi.softAP(ssid, password);
  Serial.println("Wi-Fi 热点已启动！");
  Serial.print("IP 地址: ");
  Serial.println(WiFi.softAPIP());

  // HTTP 路由设置

  // 根目录：发送网页
  server.on("/", HTTP_GET, [](AsyncWebServerRequest *request) {
    request->send(SPIFFS, "/index.html", "text/html");
  });

  // 实时获取传感器数据
  server.on("/data", HTTP_GET, [](AsyncWebServerRequest *request) {
    readSensors();
    String json = "{\"temperature\": " + String(temperature, 1) +
                  ", \"humidity\": " + String(humidity, 1) +
                  ", \"distance\": " + String(distance, 1) + "}";
    request->send(200, "application/json", json);
  });

  // 获取当前阈值（用于页面初始化）
  server.on("/getThresholds", HTTP_GET, [](AsyncWebServerRequest *request) {
    String json = "{\"temp\": " + String(thresholdTemp, 1) +
                  ", \"humid\": " + String(thresholdHumid, 1) +
                  ", \"dist\": " + String(thresholdDist, 1) + "}";
    request->send(200, "application/json", json);
  });

  // 设置阈值：使用 onBody 回调读取 JSON 请求体
  server.on("/setThresholds", HTTP_POST,
    // 完成时的回调，用于发送响应
    [](AsyncWebServerRequest *request) {
      request->send(200, "application/json", "{\"status\":\"ok\"}");
    },
    // 没有文件上传的回调参数，设置为 NULL
    NULL,
    // onBody 回调：处理完整的请求体数据
    [](AsyncWebServerRequest *request, uint8_t *data, size_t len, size_t index, size_t total) {
      String body = "";
      for (size_t i = 0; i < len; i++) {
        body += (char)data[i];
      }
      Serial.printf("接收到阈值数据: %s\n", body.c_str());
      
      StaticJsonDocument<200> doc;
      DeserializationError error = deserializeJson(doc, body);
      if (!error) {
        if (doc.containsKey("temp"))
          thresholdTemp = doc["temp"].as<float>();
        if (doc.containsKey("humid"))
          thresholdHumid = doc["humid"].as<float>();
        if (doc.containsKey("dist"))
          thresholdDist = doc["dist"].as<float>();
    
        Serial.println("阈值更新成功：");
        Serial.print("温度上限: ");
        Serial.println(thresholdTemp);
        Serial.print("湿度上限: ");
        Serial.println(thresholdHumid);
        Serial.print("距离阈值: ");
        Serial.println(thresholdDist);
      } else {
        Serial.println("解析 JSON 数据失败！");
      }
    }
  );

  // 启动 Web 服务器
  server.begin();
}

void loop() {
  // 空循环，所有逻辑由任务和事件驱动
}
