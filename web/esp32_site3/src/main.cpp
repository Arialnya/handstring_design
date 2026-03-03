#include <WiFi.h>
#include <HTTPClient.h>

// Wi-Fi 热点配置（连接到第一块 ESP32 的热点）
const char* ssid = "ESP32_AP";
const char* password = "12345678";

// 第一块 ESP32 的接口地址
const char* dataURL = "http://192.168.4.1/data";             // 实时传感器数据
const char* thresholdURL = "http://192.168.4.1/getThresholds"; // 阈值数据

// LED 引脚定义（低电平触发 LED PWM 控制）
#define LED_TEMPERATURE_PIN 16
#define LED_HUMIDITY_PIN    17
#define LED_DISTANCE_PIN    5

// 蜂鸣器引脚（低电平触发蜂鸣器鸣叫）
#define BUZZER_PIN 18

// 定义 PWM 输出范围
#define PWM_MIN 0
#define PWM_MAX 255

// 存储传感器数据（从第一块 ESP32 获取的实时数据）
float temperature = 0.0;
float humidity = 0.0;
float distance = 0.0;

// 存储阈值（从网页获取）
float thresholdTemp  = 0.0;
float thresholdHumid = 0.0;
float thresholdDist  = 0.0;

// 初始化 Wi-Fi 连接
void setupWiFi() {
  WiFi.begin(ssid, password);
  Serial.print("正在连接 Wi-Fi 热点");
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }
  Serial.println("\nWi-Fi 已连接！");
  Serial.print("ESP32 IP 地址: ");
  Serial.println(WiFi.localIP());
}

// 请求传感器数据，此处使用简单字符串解析，实际建议使用 ArduinoJson
bool fetchSensorData() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(dataURL);
    int httpResponseCode = http.GET();
    if (httpResponseCode > 0) {
      String payload = http.getString();
      Serial.println("收到传感器数据:");
      Serial.println(payload);

      // 解析 JSON 数据
      int tempStart = payload.indexOf("\"temperature\":") + 14;
      int tempEnd   = payload.indexOf(",", tempStart);
      temperature   = payload.substring(tempStart, tempEnd).toFloat();

      int humidStart = payload.indexOf("\"humidity\":") + 11;
      int humidEnd   = payload.indexOf(",", humidStart);
      humidity       = payload.substring(humidStart, humidEnd).toFloat();

      int distStart = payload.indexOf("\"distance\":") + 11;
      int distEnd   = payload.indexOf("}", distStart);
      distance      = payload.substring(distStart, distEnd).toFloat();

      http.end();
      return true;
    } else {
      Serial.print("HTTP 请求失败，错误代码: ");
      Serial.println(httpResponseCode);
    }
    http.end();
  }
  return false;
}

// 请求阈值数据
bool fetchThresholds() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(thresholdURL);
    int httpResponseCode = http.GET();
    if (httpResponseCode > 0) {
      String payload = http.getString();
      Serial.println("收到阈值数据:");
      Serial.println(payload);

      // 解析 JSON 数据，键名与网页返回一致
      int tempStart = payload.indexOf("\"temp\":") + 7;
      int tempEnd   = payload.indexOf(",", tempStart);
      thresholdTemp = payload.substring(tempStart, tempEnd).toFloat();

      int humidStart = payload.indexOf("\"humid\":") + 8;
      int humidEnd   = payload.indexOf(",", humidStart);
      thresholdHumid = payload.substring(humidStart, humidEnd).toFloat();

      int distStart = payload.indexOf("\"dist\":") + 7;
      int distEnd   = payload.indexOf("}", distStart);
      thresholdDist  = payload.substring(distStart, distEnd).toFloat();

      http.end();
      return true;
    } else {
      Serial.print("HTTP 请求阈值失败，错误代码: ");
      Serial.println(httpResponseCode);
    }
    http.end();
  }
  return false;
}

// 将传感器数据映射到 PWM 值
int mapToPWM(float value, float minInput, float maxInput) {
  if (value < minInput) value = minInput;
  if (value > maxInput) value = maxInput;
  return map(value, minInput, maxInput, PWM_MIN, PWM_MAX);
}

// 设置 LED 的亮度
void setLEDBrightness() {
  // 温度假设范围 0°C ~ 50°C
  int tempPWM = mapToPWM(temperature, 0.0, 50.0);
  ledcWrite(0, tempPWM);

  // 湿度假设范围 0% ~ 100%
  int humidPWM = mapToPWM(humidity, 0.0, 100.0);
  ledcWrite(1, humidPWM);

  // 距离假设范围 0 cm ~ 400 cm
  int distPWM = mapToPWM(distance, 0.0, 400.0);
  ledcWrite(2, distPWM);

  Serial.print("温度 PWM: ");
  Serial.println(tempPWM);
  Serial.print("湿度 PWM: ");
  Serial.println(humidPWM);
  Serial.print("距离 PWM: ");
  Serial.println(distPWM);
}

// 根据传感器数据与阈值比较，判断是否超过阈值，并控制蜂鸣器（低电平触发鸣叫）
void checkThresholdAndBuzz() {
  bool exceed = false;
  if (temperature > thresholdTemp) {
    Serial.println("温度超过阈值");
    exceed = true;
  }
  if (humidity > thresholdHumid) {
    Serial.println("湿度超过阈值");
    exceed = true;
  }
  if (distance < thresholdDist) {
    Serial.println("距离小于阈值");
    exceed = true;
  }
  
  if (exceed) {
    digitalWrite(BUZZER_PIN, LOW);  // 触发蜂鸣器鸣叫
  } else {
    digitalWrite(BUZZER_PIN, HIGH); // 关闭蜂鸣器
  }
}

void setup() {
  Serial.begin(115200);
  setupWiFi();

  // 配置 LED PWM 通道
  ledcSetup(0, 5000, 8);  // 通道 0，5kHz，8 位分辨率
  ledcAttachPin(LED_TEMPERATURE_PIN, 0);
  
  ledcSetup(1, 5000, 8);  // 通道 1
  ledcAttachPin(LED_HUMIDITY_PIN, 1);
  
  ledcSetup(2, 5000, 8);  // 通道 2
  ledcAttachPin(LED_DISTANCE_PIN, 2);
  
  // 配置蜂鸣器引脚，低电平触发
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, HIGH); // 初始关闭蜂鸣器
}

void loop() {
  // 获取传感器数据
  if (fetchSensorData()) {
    setLEDBrightness();
  } else {
    Serial.println("获取传感器数据失败！");
  }
  
  // 获取阈值数据并输出调试信息
  if (fetchThresholds()) {
    Serial.print("当前温度阈值: ");
    Serial.println(thresholdTemp);
    Serial.print("当前湿度阈值: ");
    Serial.println(thresholdHumid);
    Serial.print("当前距离阈值: ");
    Serial.println(thresholdDist);
  } else {
    Serial.println("获取阈值失败！");
  }
  
  // 检查传感器数值与阈值的对比，超过阈值则触发蜂鸣器
  checkThresholdAndBuzz();

  delay(2000);  // 每 2 秒更新一次
}
