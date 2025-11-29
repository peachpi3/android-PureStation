package com.example.purestation;

import java.util.List;

public class WeatherResponse {
    public List<DailyData> data; // 일별 날씨 리스트

    // 일별 데이터 클래스
    public class DailyData {
        public double min_temp; // 최저 기온
        public double max_temp; // 최고 기온
        public Weather weather; // 날씨 정보 (아이콘 포함)
    }

    // 날씨 정보 클래스
    public class Weather {
        public String icon; // 날씨 아이콘 코드
    }
}

