//uv 제습기 수위

#include <Arduino.h>
#ifdef ESP32
#include <WiFi.h>
#elif defined(ESP8266)
#include <ESP8266WiFi.h>
#include "arduino_secrets.h"
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

// UV 램프와 제습기 제어 핀 설정
#define UV_LAMP_PIN D1      // UV 램프가 연결된 핀 (D1는 예시)
#define DEHUMIDIFIER_PIN D2  // 제습기가 연결된 핀 (D2는 예시)

// **전역 변수 선언**
unsigned long uvStartTime = 0;  // UV 램프가 켜진 시간을 저장
unsigned long uvDuration = 0;   // UV 램프가 켜져야 할 시간을 저장
bool isUvTimerActive = false;   // 타이머가 활성화되었는지 여부

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

    // uv 제습기 핀 설정
    pinMode(D1, OUTPUT);
    pinMode(D2, OUTPUT);

    // 수위 센서 핀 설정
    pinMode(waterPin, INPUT);
}

void loop() {
  uvFunction();
  dehumFunction();
  waterFunction();
  
  delay(2000);  // 2초 대기 후 다시 확인
}

void uvFunction(){
 // Firebase에서 UV 램프 제어 신호 읽기
  if(Firebase.RTDB.getInt(&fbdo,"/RemoteControl/uvState")) {
    if (fbdo.dataType() == "int") {
      int uvLampStatus = fbdo.intData();  // Firebase에서 int 값을 가져옴

      if (uvLampStatus == 1) {
        // UV 램프 켜기
        digitalWrite(UV_LAMP_PIN, HIGH);
        Serial.println("UV Lamp ON");

        // Firebase에서 타이머 값(초 단위)을 읽어옴
        if (Firebase.RTDB.getInt(&fbdo, "/RemoteControl/uvTimer")) {
          if (fbdo.dataType() == "int") {
            uvDuration = fbdo.intData() * 1000; // 초를 밀리초로 변환
            uvStartTime = millis();  // 현재 시간 저장
            isUvTimerActive = true;  // 타이머 활성화
            Serial.print("UV Timer set for: ");
            Serial.print(uvDuration / 1000);
            Serial.println(" seconds");
          } else {
            Serial.println("Error: Wrong data type for uvTimer");
          }
        } else {
          Serial.print("Firebase read failed (UV Timer): ");
          Serial.println(fbdo.errorReason().c_str());
        }
      } else if (uvLampStatus == 0) {
        digitalWrite(UV_LAMP_PIN, LOW);   // UV 램프 끄기
        Serial.println("UV Lamp OFF");
        isUvTimerActive = false;  // 타이머 비활성화
      }
    } else {
      Serial.println("Error: Wrong data type for uvState");
    }
  } else {
    Serial.print("Firebase read failed (UV Lamp): ");
    Serial.println(fbdo.errorReason().c_str());
  }

  // UV 램프 타이머 동작
  if (isUvTimerActive) {
    if (millis() - uvStartTime >= uvDuration) {
      digitalWrite(UV_LAMP_PIN, LOW);  // 타이머가 끝나면 UV 램프 끔
      Serial.println("UV Lamp OFF (Timer)");

      // Firebase에 uvState를 0으로 업데이트
      if (Firebase.RTDB.setInt(&fbdo, "/RemoteControl/uvState", 0)) {
        Serial.println("UV Lamp state updated to 0 in Firebase");
      } else {
        Serial.print("Failed to update uvState: ");
        Serial.println(fbdo.errorReason().c_str());
      }
      
      isUvTimerActive = false;  // 타이머 비활성화
    }
  }
}

void dehumFunction(){
  // Firebase에서 제습기 제어 신호 읽기
  if(Firebase.RTDB.getInt(&fbdo, "/RemoteControl/dehumState")) {
    if (fbdo.dataType() == "int") {
      int dehumidifierStatus = fbdo.intData();  // Firebase에서 int 값을 가져옴

      if (dehumidifierStatus == 1) {
        digitalWrite(DEHUMIDIFIER_PIN, HIGH);  // 제습기 켜기
        Serial.println("Dehumidifier ON");
      } else if (dehumidifierStatus == 0) {
        digitalWrite(DEHUMIDIFIER_PIN, LOW);   // 제습기 끄기
        Serial.println("Dehumidifier OFF");
      }
    } else {
      Serial.println("Error: Wrong data type for humStatus");
    }
  } else {
    Serial.print("Firebase read failed (Dehumidifier): ");
    Serial.println(fbdo.errorReason().c_str());
  }

}

void waterFunction(){
  // 수위 측정 값 읽기
    int waterLevel = analogRead(waterPin);

    // 수위에 따른 메시지 출력
    if (waterLevel >= 380) {
        Serial.println("물을 비워주세요");
    } else {
        Serial.println("물이 없습니다");
    }

    // Firebase에 데이터 업로드 (5초마다)
    if (Firebase.ready() && (millis() - sendDataPrevMillis > 5000 || sendDataPrevMillis == 0)) {
        sendDataPrevMillis = millis();

        int waterFullStatus = (waterLevel >= 380) ? 1 : 0; // 380 이상이면 1, 아니면 0 전송

        // 수위 상태 업로드
        if (Firebase.RTDB.setInt(&fbdo, "/Arduino/dehumiIsWaterFull", waterFullStatus)) {
            Serial.println("Water level status sent successfully");
        } else {
            Serial.print("Error sending water level status: ");
            Serial.println(fbdo.errorReason());
        }
    }
}
