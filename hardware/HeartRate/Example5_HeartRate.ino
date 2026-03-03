#include <Wire.h>
#include "MAX30105.h"
#include "heartRate.h"

// ESP8266 NodeMCU pin definitions
#define SDA D2    // GPIO4
#define SCL D1    // GPIO5
#define RESET_PIN D5 // GPIO14 for reset

MAX30105 particleSensor;

const byte RATE_SIZE = 4; // Increase this for more averaging. 4 is good.
byte rates[RATE_SIZE];    // Array of heart rates
byte rateSpot = 0;
long lastBeat = 0;        // Time at which the last beat occurred
long lastDataUpdate = 0;  // Track last time we got valid data

float beatsPerMinute = 0;
int beatAvg = 0;
const int MIN_HEART_RATE = 40; // Minimum valid heart rate
bool fingerDetected = false;   // Track finger presence
bool hadValidData = false;     // Track if we ever had valid data

void setup() {
  Serial.begin(115200);
  Serial.println("Initializing...");

  // Initialize reset pin
  pinMode(RESET_PIN, OUTPUT);
  digitalWrite(RESET_PIN, LOW);

  // Initialize I2C with specific pins for ESP8266
  Wire.begin(SDA, SCL);

  // Initialize sensor
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
    Serial.println("MAX30105 was not found. Please check wiring/power.");
    while (1);
  }
  Serial.println("Place your index finger on the sensor with steady pressure.");

  // Configure sensor with default settings
  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x0A); // Turn Red LED to low to indicate sensor is running
  particleSensor.setPulseAmplitudeGreen(0);  // Turn off Green LED

  lastDataUpdate = millis(); // Initialize timer
}

void resetSensor() {
  Serial.println("Resetting sensor...");
  digitalWrite(RESET_PIN, HIGH);
  delay(100); // Keep reset high for 100ms
  digitalWrite(RESET_PIN, LOW);
  
  // Reinitialize sensor
  particleSensor.begin(Wire, I2C_SPEED_FAST);
  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x0A);
  particleSensor.setPulseAmplitudeGreen(0);
  
  lastDataUpdate = millis(); // Reset timer after reset
}

void loop() {
  long irValue = particleSensor.getIR();
  bool currentFingerState = irValue > 50000;
  
  // Update finger detection status with debouncing
  if (currentFingerState != fingerDetected) {
    delay(50); // Debounce delay
    currentFingerState = particleSensor.getIR() > 50000;
    fingerDetected = currentFingerState;
  }

  if (checkForBeat(irValue) == true) {
    // We sensed a beat!
    long delta = millis() - lastBeat;
    lastBeat = millis();

    if (fingerDetected) {
      float newBPM = 60 / (delta / 1000.0);

      // Only accept heart rates between 40 and 255 BPM
      if (newBPM > MIN_HEART_RATE && newBPM < 255) {
        beatsPerMinute = newBPM;
        hadValidData = true;
        
        rates[rateSpot++] = (byte)beatsPerMinute; // Store this reading in the array
        rateSpot %= RATE_SIZE; // Wrap variable

        // Take average of readings
        beatAvg = 0;
        for (byte x = 0; x < RATE_SIZE; x++)
          beatAvg += rates[x];
        beatAvg /= RATE_SIZE;
        
        lastDataUpdate = millis(); // Update last data time
      }
      else {
        Serial.print("Invalid heart rate detected: ");
        Serial.print(newBPM);
        Serial.println(" BPM (Ignored)");
      }
    }
  }

  // Check if finger is present but no valid data for 5 seconds
  if (fingerDetected && (millis() - lastDataUpdate > 5000)) {
    resetSensor();
    return; // Skip the rest of the loop to allow sensor to stabilize
  }

  Serial.print("IR=");
  Serial.print(irValue);
  
  if (hadValidData) {
    Serial.print(", BPM=");
    Serial.print(beatsPerMinute);
    Serial.print(", Avg BPM=");
    Serial.print(beatAvg);
  } else {
    Serial.print(", BPM=--");
    Serial.print(", Avg BPM=--");
  }
  
  Serial.print(", lastBeat=");
  Serial.print(lastBeat);

  if (!fingerDetected) {
    Serial.print(" No finger detected");
  }

  Serial.println();

  // Add a small delay to prevent flooding the serial output
  delay(20);
}