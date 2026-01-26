//온습도 가습기 수위

// 온습도 센서 라이브러리
#include "DHT.h"
#include "arduino_secrets.h"

#include <Arduino.h>
#ifdef ESP32
#include <WiFi.h>
#elif defined(ESP8266)
#include <ESP8266WiFi.h>
#endif

#include <Firebase_ESP_Client.h>

// Provide the token generation process info.
#include <addons/TokenHelper.h>

// Provide the RTDB payload printing info and other helper functions.
#include <addons/RTDBHelper.h>

// Firebase 객체 선언
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// DHT 설정
#define DHTPIN D4
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// 가습기 제어 핀 설정
#define HUMIDIFIER_PIN D3    // 가습기가 연결된 핀

// Firebase에서 온도 값을 저장할 변수 선언
float currentHumidity = 0.0;  // Firebase에서 가져올 현재 습도
float wantHum = 0.0;         // 사용자가 설정한 습도 값 

// 수위 측정 설정
const int waterPin = A0; // 사용할 아날로그 핀 번호 지정

unsigned long sendDataPrevMillis = 0;

void setup() {
  Serial.begin(115200);

  // Wi-Fi 연결
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Connecting to Wi-Fi");
    while (WiFi.status() != WL_CONNECTED) {
        Serial.print(".");
        delay(300);
    }
    Serial.println();
    Serial.print("Connected with IP: ");
    Serial.println(WiFi.localIP());
    Serial.println();

    Serial.printf("Firebase Client v%s\n\n", FIREBASE_CLIENT_VERSION);

    // Firebase 설정
    config.api_key = API_KEY;
    auth.user.email = USER_EMAIL;
    auth.user.password = USER_PASSWORD;
    config.database_url = DATABASE_URL;
    config.token_status_callback = tokenStatusCallback; // 토큰 갱신 콜백 함수

    // Firebase 연결
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);

    // 온습도 센서 시작
    dht.begin();

    // 가습기 핀 설정
    pinMode(D3, OUTPUT);
    digitalWrite(D3, LOW); // 가습기 초기 상태 꺼짐

    // 수위 센서 핀 설정
    pinMode(waterPin, INPUT);
}

void loop() {
  temperFunction();
  humFunction();
  waterFunction();
  
  delay(2000);  // 2초 대기 후 다시 확인
}

void temperFunction(){
 float humidity = dht.readHumidity();  // 습도
    float temperature = dht.readTemperature();  // 온도

    // 센서 값 출력
    Serial.print("Humidity: ");
    Serial.print(humidity);
    Serial.print(" %\t");
    Serial.print("Temperature: ");
    Serial.print(temperature);
    Serial.println(" C");

    // Firebase에 데이터 업로드 (5초마다)
    if (Firebase.ready() && (millis() - sendDataPrevMillis > 5000 || sendDataPrevMillis == 0)) {
        sendDataPrevMillis = millis();

        // 습도 업로드
        if (Firebase.RTDB.setFloat(&fbdo, "/Arduino/humidity", humidity)) {
            Serial.println("Humidity data sent successfully");
        } else {
            Serial.print("Error sending humidity: ");
            Serial.println(fbdo.errorReason());
        }

        // 온도 업로드
        if (Firebase.RTDB.setFloat(&fbdo, "/Arduino/temperature", temperature)) {
            Serial.println("Temperature data sent successfully");
        } else {
            Serial.print("Error sending temperature: ");
            Serial.println(fbdo.errorReason());
        }
    }
}

void humFunction(){
  // Firebase에서 가습기 상태 및 사용자 설정 습도, 현재 습도 가져오기
    if (Firebase.ready()) {
        // 현재 습도 읽기
        if (Firebase.RTDB.getFloat(&fbdo, "/Arduino/humidity")) {
            currentHumidity = fbdo.floatData(); // Firebase에서 가져온 현재 습도 값
            Serial.print("current Humidity: ");
            Serial.println(currentHumidity);
        } else {
            Serial.print("Failed to get currentHumidity: ");
            Serial.println(fbdo.errorReason());
        }

        // 사용자 설정 습도 읽기
        if (Firebase.RTDB.getInt(&fbdo, "/RemoteControl/wantHum")) {
            int wantHum = fbdo.intData(); // 사용자가 설정한 습도 값
            Serial.print("Want Hum: ");
            Serial.println(wantHum);

            // 설정한 습도보다 현재 습도가 높거나 같으면 가습기 끄기
            if (currentHumidity >= wantHum) {
                digitalWrite(HUMIDIFIER_PIN, LOW); // 가습기 끄기
                Serial.println("Humidifier OFF (Humidity reached set point)");

                // 가습기 상태 Firebase에 업로드 (0: OFF)
                if (Firebase.RTDB.setInt(&fbdo, "/RemoteControl/humState", 0)) {
                    Serial.println("Humidifier state (OFF) sent successfully");
                } else {
                    Serial.print("Error sending humidifier state (OFF): ");
                    Serial.println(fbdo.errorReason());
                }
            } else {
                // 설정한 습도보다 현재 습도가 낮으면 가습기 켜기
                digitalWrite(HUMIDIFIER_PIN, HIGH); // 가습기 켜기
                Serial.println("Humidifier ON (Humidity below set point)");

                // 가습기 상태 Firebase에 업로드 (1: ON)
                if (Firebase.RTDB.setInt(&fbdo, "/RemoteControl/humState", 1)) {
                    Serial.println("Humidifier state (ON) sent successfully");
                } else {
                    Serial.print("Error sending humidifier state (ON): ");
                    Serial.println(fbdo.errorReason());
                }
            }
        } else {
            Serial.print("Failed to get setHimidity: ");
            Serial.println(fbdo.errorReason());
        }
    }

    // Firebase에 가습기 상태 업로드 (5초마다)
    if (millis() - sendDataPrevMillis > 5000 || sendDataPrevMillis == 0) {
        sendDataPrevMillis = millis();

        // 가습기 상태 Firebase에 업로드
        int currentState = digitalRead(HUMIDIFIER_PIN); // 현재 가습기 상태 (LOW or HIGH)
        if (Firebase.RTDB.setInt(&fbdo, "/RemoteControl/humState", currentState)) {
            Serial.println("Current humidifier state sent successfully");
        } else {
            Serial.print("Error sending current humidifier state: ");
            Serial.println(fbdo.errorReason());
        }
    }

}

void waterFunction(){
  // 수위 측정 값 읽기
    int waterLevel = analogRead(waterPin);

    // 수위에 따른 메시지 출력
    if (waterLevel <= 200) {
        Serial.println("물을 채워주세요");
    } else {
        Serial.println("물이 있습니다");
    }
        int waterFullStatus = (waterLevel <= 200) ? 1 : 0; // 200 이하이면 1, 아니면 0 전송

        // 수위 상태 업로드
        if (Firebase.RTDB.setInt(&fbdo, "/Arduino/humiIsWaterFull", waterFullStatus)) {
            Serial.println("Water level status sent successfully");
        } else {
            Serial.print("Error sending water level status: ");
            Serial.println(fbdo.errorReason());
        }
}
