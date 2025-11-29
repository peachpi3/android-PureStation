package com.example.purestation;
/*

    오늘의 날씨를 받아 날씨 아이콘 포멧에 맞게
    최고, 최저, 날씨 (맑은 날, 비 오는 날 등..), 저장 시간 데이터를
    파이어베이스에 저장합니다.

 */

import android.util.Log;

import com.google.firebase.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FirebaseWeatherSaver {
    private DatabaseReference weatherRef = FirebaseDatabase.getInstance().getReference("TodayWeather");
    private String TAG = "FirebaseWeatherSaver";

    public FirebaseWeatherSaver () {}

    // 아이콘 코드에 따른 설명을 반환하는 메서드
    // 맑은 날, 흐린 날, 비 오는 날, 바람 부는 날, 겨울: 눈 오는 날
    private String getWeatherDescription(String iconCode) {
        switch (iconCode) {
            case "t01d" :
            case "t01n" :
            case "t02d" :
            case "t02n" :
            case "t03d" :
            case "t03n" :
            case "t04d" :
            case "t04n" :
            case "t05d" :
            case "t05n" :

            case "d01d" :
            case "d01n" :
            case "d02d" :
            case "d02n" :
            case "d03d" :
            case "d03n" :

            case "r03d" :
            case "r03n" :
            case "r04d" :
            case "r04n" :
            case "r05d" :
            case "r05n" :
            case "r06d" :
            case "r06n" :
            case "u00d" :
            case "u00n" :
                return "비 오는 날";

            case "s01d" :
            case "s01n" :
            case "s02d" :
            case "s02n" :
            case "s03d" :
            case "s03n" :
            case "s04d" :
            case "s04n" :
            case "s05d" :
            case "s05n" :
            case "s06d" :
            case "s06n" :
                return "눈 오는 날";

            case "c01d" :
            case "c01n" :
                return "맑은 날";

            case "a01d" :
            case "a01n" :
            case "a02d" :
            case "a02n" :
            case "a03d" :
            case "a03n" :
            case "a04d" :
            case "a04n" :
            case "a05d" :
            case "a05n" :
            case "a06d" :
            case "a06n" :

            case "c02d" :
            case "c02n" :
            case "c03d" :
            case "c03n" :
            case "c04d" :
            case "c04n" :
                return "흐린 날";

            default : return "error";
        }
    }

    // 매일 아침 9시에 받아온 날씨 데이터를 저장함
    public void saveWeatherData (int maxTemp, int minTemp, String iconCode) {
        Date dateNow = Calendar.getInstance().getTime();
        SimpleDateFormat format = new SimpleDateFormat("M", Locale.getDefault());
        int month = Integer.parseInt(format.format(dateNow));

        int avgTemp = (maxTemp + minTemp) / 2;
        
        // 리얼타임 데이터베이스에 날씨 데이터 저장
        Map<String, Object> weatherData = new HashMap<>();
        weatherData.put("avgTemp", avgTemp); // 평균 기온 저장
        weatherData.put("iconCode", iconCode);
        weatherData.put("description", getWeatherDescription(iconCode));

        if (month >= 3 && month < 6) { // 3~5월: 봄
            weatherData.put("season", "봄");
        } else if (month >= 6 && month < 9) {// 6~8월: 여름
            weatherData.put("season", "여름");
        } else if (month >= 9 && month < 12) { // 9~11월: 가을
            weatherData.put("season", "가을");
        } else if (month == 12 || month == 1 || month == 2) { // 12~2월: 겨울
            weatherData.put("season", "겨울");
        }


        weatherRef.setValue(weatherData).addOnSuccessListener(aVoid -> {
           Log.d(TAG, "날씨 데이터 저장 성공");
        }).addOnFailureListener(e -> {
           Log.d(TAG, "날씨 데이터 저장 실패", e);
        });
    } // saveWeatherData()

    // 타임스탬프 시간을 ex) PM 09:00 으로 포멧
    public String toTimeStamp(long time){
        Date date = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("a hh:mm", Locale.getDefault());
        return sdf.format(date);
    }

}

