package com.example.purestation;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// Weather Bit
public interface WeatherService {
    @GET("forecast/daily") // 날씨 API 엔드 포인트
    Call<WeatherResponse> getDailyForecast(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("key") String key,
            @Query("days") int days // 예보 일 수
    );
}
