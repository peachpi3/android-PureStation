package com.example.purestation;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClothingRecommendationModel {
    private String TAG = "ClothingRecommendationModel";

    // 3개의 모델 인터프리터
    private Interpreter topInterpreter;
    private Interpreter bottomInterpreter;
    private Interpreter idInterpreter;

    int numMaterials = 14; // 재질의 총 개수
    int numColors = 18;   // 색상의 총 개수
    int numShapes = 30;    // 모양의 총 개수

    private Context context;

    // 모델 경로 저장
    private static final String TOP_MODEL_PATH = "top_clothing_model.tflite";
    private static final String BOTTOM_MODEL_PATH = "bottom_clothing_model.tflite";
    private static final String ID_MODEL_PATH = "clothes_recommendation_model.tflite";

    private InputDataConverter converter = new InputDataConverter();

    // 의류 데이터 경로
    private DatabaseReference clothesRef = FirebaseDatabase.getInstance().getReference("Clothes");

    public ClothingRecommendationModel(Context context) {
        this.context = context;
    }

    // TensorFlow Lite 모델 로드
    public void loadModels(OnModelsLoadedCallback callback) {
        try {
            // Assets에서 모델 파일 로드
            topInterpreter = new Interpreter(loadModelFile(context, TOP_MODEL_PATH));
            bottomInterpreter = new Interpreter(loadModelFile(context, BOTTOM_MODEL_PATH));
            idInterpreter = new Interpreter(loadModelFile(context, ID_MODEL_PATH));
            callback.onModelsLoaded();

        } catch (IOException e) {
            e.printStackTrace();
            callback.onError(e);
        }
    }

    // 안드로이드 메모리 버퍼에 저장
    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        String assetPath = modelPath;
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(assetPath);

        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // 날씨 기반 의류 추천 메소드
    public void recommendClothing(int season, int description, int avgTemp, OnRecommendationComplete callback) {

        try {
            // 입력 데이터 (계절, 날씨, 평균기온)
            float[][][] weatherArray = new float[1][1][3];
            weatherArray[0][0][0] = season;
            weatherArray[0][0][1] = description;
            weatherArray[0][0][2] = avgTemp;

            // 상의, 하의 예측 모델 출력 배열 준비 (색상, 재질, 모양)
            float[][] topOutput = new float[1][402];
            float[][] bottomOutput = new float[1][402];

            // 상의, 하의 예측 모델 실행
            Log.d(TAG, "입력 데이터: season=" + season + ", description=" + description + ", avgTemp=" + avgTemp);
            topInterpreter.run(weatherArray, topOutput);
            bottomInterpreter.run(weatherArray, bottomOutput);

            Log.d(TAG, "상의 예측 결과: " + Arrays.deepToString(topOutput));
            Log.d(TAG, "하의 예측 결과: " + Arrays.deepToString(bottomOutput));

            // predictId 메소드는 예측된 속성 데이터를 입력으로 하여,
            // 파이어베이스에 저장된 의류 ID 중 예측 속성에 가장 유사한 상위 3개의 ID를 선택함
            // 상위 3개를 뽑는 건 getThreeClothes 메소드에서 수행

            // 상의 ID 예측
            List<String> topIds = predictId(true, topOutput[0]);
            // 하의 ID 예측
            List<String> bottomIds = predictId(false, bottomOutput[0]);

            // ID 리스트를 ClothingItem 리스트로 변환 후 콜백 전달
            fetchClothingItemsByIds(topIds, bottomIds, callback);

        } catch (Exception e) {
            Log.e(TAG, "모델을 사용할 수 없습니다.");
            e.printStackTrace();
            callback.onError(e);
        }

    } // recommendClothing()

    private List<String> predictId(boolean isTop, float[] attributes) {
        // 원-핫 인코딩된 출력을 각 속성의 인덱스로 변환
        int colorIndex = convertOneHotToIndex(Arrays.copyOfRange(attributes, 0, numColors));
        int materialIndex  = convertOneHotToIndex(Arrays.copyOfRange(attributes, numColors, numColors + numMaterials));
        int shapeIndex = convertOneHotToIndex(Arrays.copyOfRange(attributes, numColors + numMaterials, numColors + numMaterials + numShapes));

        // 인덱스 값을 한글로 변환하여 로깅
        String colorName = converter.convertColor(colorIndex);
        String materialName = converter.convertMaterial(materialIndex);
        String shapeName = converter.convertShape(shapeIndex);

        Log.d(TAG, "예측된 속성 데이터: \n"
                + "색상 : " + colorIndex + " / " + colorName + " "
                + "재질 : " + materialIndex + " / " + materialName + " "
                + "모양 : " + shapeIndex + " / " + shapeName);

        // ID 모델 입력 준비
        float[][] idInput = new float[1][402];
        idInput[0][0] = isTop ? 0 : 1; // 상의: 0, 하의: 1 구분
        idInput[0][1] = colorIndex; // 색상
        idInput[0][2] = materialIndex; // 재질
        idInput[0][3] = shapeIndex; // 모양

        // ID 모델 출력 준비
        float[][] idOutput = new float[100][36];

        // ID 모델 실행
        idInterpreter.run(idInput, idOutput);

        if (isTop) {
            Log.d("PredictId", "추천된 상의 아이디 : " + Arrays.deepToString(idOutput));
        } else {
            Log.d("PredictId", "추천된 하의 아이디 : " + Arrays.deepToString(idOutput));
        }


        // 상위 3개의 ID 찾기
        getThreeClothes(isTop,colorName, materialName, shapeName);
        return new ArrayList<>();
    }

    // 원-핫 인코딩된 배열을 인덱스로 변환
    private int convertOneHotToIndex(float[] oneHotArray) {
        int maxIndex = 0;
        float maxValue = oneHotArray[0];

        for (int i = 1; i < oneHotArray.length; i++) {
            if (oneHotArray[i] > maxValue) {
                maxValue = oneHotArray[i];
                maxIndex = i;
            }
        }

        return maxIndex;
    }


    // 20개의 ID 중 상위 3개 찾기
    private void getThreeClothes(boolean isTop,String colorName, String materialName, String shapeName) {
        // ID와 점수를 함께 저장할 클래스
        class IdScore implements Comparable<IdScore> {
            String id;
            float score; // 속성 유사도 클래스

            IdScore(String id, float score) {
                this.id = id;
                this.score = score;
            }

            @Override
            public int compareTo(IdScore other) {
                // 점수를 내림차순 정렬
                return Float.compare(other.score, this.score);
            }
        } // Class of IdScore

        // ID와 점수를 저장할 리스트 생성
        List<IdScore> idScores = new ArrayList<>();

        clothesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String itemId = itemSnapshot.getKey();
                    String category = itemSnapshot.child("category").getValue(String.class);
                    String color = itemSnapshot.child("color").getValue(String.class);
                    String material = itemSnapshot.child("material").getValue(String.class);
                    String shape = itemSnapshot.child("shape").getValue(String.class);

                    if (category.equals("상의") || category.equals("하의")) {
                        float similarityScore = 0f;
                        similarityScore += (color.equals(String.valueOf(colorName))) ? 0.2f : 0;
                        similarityScore += (material.equals(String.valueOf(materialName))) ? 0.3f : 0;
                        similarityScore += (shape.equals(String.valueOf(shapeName))) ? 0.5f : 0;


                        idScores.add(new IdScore(itemId, similarityScore));

                        if (isTop) {
                            Log.i(TAG, "상의 ID : " + itemId + "점수 : " + similarityScore);
                        } else if (!isTop) {
                            Log.i(TAG, "하의 ID : " + itemId + "점수 : " + similarityScore);
                        }
                    } // if


                } // for

                Collections.sort(idScores);
                List<String> threeIds = new ArrayList<>();

                for (int i = 0; i < Math.min(3, idScores.size()); i++) {
                    threeIds.add(idScores.get(i).id);
                }

                if (isTop) {
                    saveTopThreeId(threeIds);
                } else {
                    saveBottomThreeId(threeIds);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "옷 데이터를 가져올 수 없습니다.");
            }
        });
    } // getThreeClothes()

    // 상위 3개의 상의 ID를 Firebase에 저장
    private void saveTopThreeId(List<String> threeIds) {
        DatabaseReference topRef = FirebaseDatabase.getInstance().getReference("RecommendedClothId");
        topRef.setValue(threeIds).addOnCompleteListener(task -> {

           if (task.isSuccessful()) {
               Log.d(TAG, "상위 3개의 상의 ID가 저장되었습니다.");

           } else {
               Log.d(TAG, "상위 3개의 상의 ID 저장에 실패했습니다.");
           }
        });
    } // OnTop3IdsRetrievedListener()

    // 상위 3개의 하의 ID를 Firebase에 저장
    private void saveBottomThreeId(List<String> threeIds) {
        DatabaseReference bottomRef = FirebaseDatabase.getInstance().getReference("RecommendedClothId");
        bottomRef.setValue(threeIds).addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                Log.d(TAG, "상위 3개의 하의 ID가 저장되었습니다.");

            } else {
                Log.d(TAG, "상위 3개의 하의 ID 저장에 실패했습니다.");
            }
        });
    }

    // ID 리스트를 통해 ClothingItem 데이터를 Firebase에서 가져옴
    private void fetchClothingItemsByIds(List<String> topIds, List<String> bottomIds, OnRecommendationComplete callback) {
        List<ClothingItem> topItems = new ArrayList<>();
        List<ClothingItem> bottomItems = new ArrayList<>();

        clothesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {

                    ClothingItem item = itemSnapshot.getValue(ClothingItem.class);

                    if (item != null) {
                        if (topIds.contains(item.getId())) {
                            topItems.add(item);

                        } else if (bottomIds.contains(item.getId())) {
                            bottomItems.add(item);
                        }
                    } // if
                } // for

                callback.onComplete(topItems, bottomItems);

            } // onDataChange()

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(new Exception("Firebase 데이터 로드 실패"));
            }
        });
    }



    // 콜백 인터페이스
    interface OnRecommendationComplete {
        void onComplete(List<ClothingItem> topItems, List<ClothingItem> bottomItems);

        void onError(Exception e);
    }

    interface OnModelsLoadedCallback {
        void onModelsLoaded();

        void onError(Exception e);
    }

}