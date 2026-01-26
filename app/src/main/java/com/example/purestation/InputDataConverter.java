package com.example.purestation;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;

/*
    계절, 날씨 (맑은 날, 비 오는 날 등..), 모양, 재질, 색상 값들에 대한
    인덱스를 지정 -> ClothingRecomendationModel에 적용
 */
public class InputDataConverter {

    // 계절을 수치로 변환
    public int convertSeason(String season) {
        int seasonIndex = 0;

        if (season != null) {
            if (season.equals("봄")) {
                seasonIndex = 305;
            } else if (season.equals("여름")) {
                seasonIndex = 608;
            } else if (season.equals("가을")) {
                seasonIndex = 911;
            } else if (season.equals("겨울")) {
                seasonIndex = 1202;
            }
        }

        return seasonIndex;
    }

    // 계절에 따른 각 날씨를 수치로 변환
    public int convertWeather(String description) {
        int descriptionIndex = 0;

        if (description != null) {
            
                if (description.equals("맑은 날")) {
                    descriptionIndex = 1;
                } else if (description.equals("흐린 날")) {
                    descriptionIndex = 2;
                } else if (description.equals("비 오는 날")) {
                    descriptionIndex = 3;
                } else if (description.equals("바람 부는 날")) {
                    descriptionIndex = 4;
                } else if (description.equals("눈 오는 날")) {
                    descriptionIndex = 5;
                }
            }

        return descriptionIndex;
    }

    /*
        < 디코딩 메소드 >
        추천된 색상, 재질, 모양을 한글화
     */

    public String convertColor(int color) {
        switch (color) {
            case 0 : return "흰색";
            case 1 : return "검정색";

            case 10 : return "아이보리색";
            case 11 : return "네이비색";

            case 20 : return "회색";
            case 21 : return "밝은회색";
            case 22 : return "짙은회색";

            case 30 : return "하늘색";
            case 31 : return "파란색";

            case 40 : return "베이지색";
            case 41 : return "모래색";
            case 42 : return "갈색";

            case 50 : return "빨간색";
            case 60: return "노란색";
            case 70 : return "초록색";

            case 80 : return "옅은분홍색";
            case 81 : return "분홍색";

            case 90 : return "민트색";
            default: return "Error";
        }
    }

    public String convertMaterial(int material) {
        switch (material) {
            // 천연섬유
            case 0 : return "면";
            case 1 : return "린넨";
            case 2 : return "울";
//            case 003 : return "실크";

            // 인조섬유
            case 100 : return "폴리에스터";
            case 101 : return "나일론";
            case 102 : return "레이온";
            case 103 : return "아크릴";
            
            // 혼방섬유
            case 200 : return "폴리코튼";
            case 201 : return "울 블렌드";
           
            // 특수섬유
            case 300 : return "고어텍스";
            case 301 : return "스판덱스/엘라스탄";

            // 가죽
            case 400 : return "천연가죽";
            case 401 : return "인조가죽";
            default: return "Error";
        }
    }

    public String convertShape(int shape) {
        switch (shape) {
            // 상의
            case 1000 : return "민소매";
            case 1001 : return "반팔";
            case 1002 : return "5부 소매";
            case 1003 : return "7부 소매";
            case 1004 : return "긴팔";
            
            // 하의
            case 2000 : return "반바지";
            case 2001 : return "5부 바지";
            case 2002 : return "7부 바지";
            case 2003 : return "긴바지";
            case 2004 : return "배기바지";
            case 2005 : return "레깅스";
            
            // 스커트
            case 3000 : return "미니 스커트";
            case 3001 : return "미디 스커트";
            case 3002 : return "롱 스커트";
            case 3003 : return "플레어 스커트";
            case 3004 : return "타이트 스커트";
            
            // 원피스
            case 4000 : return "미니 원피스";
            case 4001 : return "미디 원피스";
            case 4002 : return "맥시 원피스";
            case 4003 : return "A라인 원피스";
            case 4004 : return "셔츠 원피스";

            // 아우터
            case 6000 : return "후드";
            case 6001 : return "가디건";
            case 6002 : return "코트";
            case 6003 : return "트렌치코트";
            case 6004 : return "재킷";
            case 6005 : return "가죽재킷";
            case 6006 : return "파카";
            case 6007 : return "블레이저";
            case 6008 : return "다운패딩";
            
            default: return "Error";
        }
    }

    // 계절에 따른 날씨 값을 반환
    // 예) 봄: 305, 맑은 날:1 => 3051
//    public int convertSeasonAndWeather(String season, String description) {
//        // 각 항목들을 먼저 따로 변환
//        int seasonValue = convertSeason(season);
//        int descriptionValue = convertWeather(description);
//
//        // seasonValue 또는 weatherValue가 유효하지 않으면 에러 처리
//        if (seasonValue == -1 || descriptionValue == -1) {
//            return -1; // 알 수 없는 값
//        }
//
//        return seasonValue * 10 + descriptionValue; // 계절과 날씨 값을 합친 값
//    }
}
