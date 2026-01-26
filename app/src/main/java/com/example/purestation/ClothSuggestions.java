package com.example.purestation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ClothSuggestions extends AppCompatActivity {
    private String TAG = "ClothSuggestions";
    private ImageButton backIcon, previousTop, nextTop, previousBottom, nextBottom;
    private ImageView topImg, bottomImg;
    private Button btnEndCodi;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference clothesRef = database.getReference("Clothes");

    // 현재 계절
    private String currentSeason;

    // 추천된 옷을 저장할 리스트
    private List<ClothingItem> topRecommendations = new ArrayList<>();
    private List<ClothingItem> bottomRecommendations = new ArrayList<>();

    // 현재 보여지는 아이템의 인덱스
    private int currentTopIndex = 0;
    private int currentBottomIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cloth_suggestions);

        topImg = findViewById(R.id.outfitTop);
        bottomImg = findViewById(R.id.outfitBottom);
        backIcon = findViewById(R.id.backIcon);
        previousTop = findViewById(R.id.previousTop);
        nextTop = findViewById(R.id.nextTop);
        previousBottom = findViewById(R.id.previousBottom);
        nextBottom = findViewById(R.id.nextBottom);
        btnEndCodi = findViewById(R.id.btnEndCodi);

        // 홈 화면으로
        backIcon.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        btnEndCodi.setOnClickListener(view -> {
            // AlertDialog 생성
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("코디 완료")
                    .setMessage("코디를 완료 하시겠습니까?")
                    .setPositiveButton("확인", (dialog, which) -> {
                        // 현재 상의 및 하의 ID 가져오기
                        String selectedTopId = topRecommendations.get(currentTopIndex).id;
                        String selectedBottomId = bottomRecommendations.get(currentBottomIndex).id;

                        // Firebase에 저장
                        DatabaseReference userCodiRef = database.getReference("UserCodi");
                        userCodiRef.child("상의").setValue(selectedTopId);
                        userCodiRef.child("하의").setValue(selectedBottomId)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "코디 저장 성공!");
                                        Toast.makeText(this, "오늘의 코디 완료!", Toast.LENGTH_LONG).show();
                                    } else {
                                        Log.e(TAG, "코디 저장 실패: " + task.getException());
                                        Toast.makeText(this, "다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                                    }
                                });
                    })
                    .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                    .show();
        });


        // 현재 계절 계산
        currentSeason = getCurrentSeason();

        // 옷 이미지 변경 버튼 리스너 설정
        setupClickListeners();

        // Firebase에서 계절에 맞는 옷 가져오기
        fetchClothesForCurrentSeason();
    }

    private void setupClickListeners() {
        previousTop.setOnClickListener(v -> showPreviousTop());
        nextTop.setOnClickListener(v -> showNextTop());
        previousBottom.setOnClickListener(v -> showPreviousBottom());
        nextBottom.setOnClickListener(v -> showNextBottom());
    }

    private String getCurrentSeason() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;

        if (month >= 3 && month <= 5) {
            return "봄";
        }
        else if (month >= 6 && month <= 8) {
            return "여름";
        }
        else if (month >= 9 && month <= 11) {
            return "가을";
        }
        else if (month == 12 || month == 1 || month == 2) {
            return "겨울";
        } else {
            return "error";
        }
    }

    private void fetchClothesForCurrentSeason() {
        clothesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String id = itemSnapshot.getKey();
                    String category = itemSnapshot.child("category").getValue(String.class);
                    String season = itemSnapshot.child("season").getValue(String.class);
                    String imageUrl = itemSnapshot.child("url").getValue(String.class);

                    // 현재 달의 계절과 옷의 계절감이 일치할 경우
                    if (season != null && season.equals(currentSeason)) {
                        if ("상의".equals(category) || "아우터".equals(category) || "원피스".equals(category)) {
                            topRecommendations.add(new ClothingItem(id, category, imageUrl));
                        } else if ("하의".equals(category) || "스커트".equals(category)) {
                            bottomRecommendations.add(new ClothingItem(id, category, imageUrl));
                        }
                    }
                }

                // 초기 이미지 표시
                runOnUiThread(() -> {
                    updateTopImage();
                    updateBottomImage();
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Error fetching clothes: " + error.getMessage());
            }
        });
    }

    private void showPreviousTop() {
        if (!topRecommendations.isEmpty()) {
            currentTopIndex = (currentTopIndex - 1 + topRecommendations.size()) % topRecommendations.size();
            updateTopImage();
        }
    }

    private void showNextTop() {
        if (!topRecommendations.isEmpty()) {
            currentTopIndex = (currentTopIndex + 1) % topRecommendations.size();
            updateTopImage();
        }
    }

    private void showPreviousBottom() {
        if (!bottomRecommendations.isEmpty()) {
            currentBottomIndex = (currentBottomIndex - 1 + bottomRecommendations.size()) % bottomRecommendations.size();
            updateBottomImage();
        }
    }

    private void showNextBottom() {
        if (!bottomRecommendations.isEmpty()) {
            currentBottomIndex = (currentBottomIndex + 1) % bottomRecommendations.size();
            updateBottomImage();
        }
    }

    private void updateTopImage() {
        if (!topRecommendations.isEmpty() && currentTopIndex < topRecommendations.size()) {
            String imageUrl = topRecommendations.get(currentTopIndex).getUrl();
            Glide.with(this)
                    .load(imageUrl)
                    .into(topImg);
        }
    }

    private void updateBottomImage() {
        if (!bottomRecommendations.isEmpty() && currentBottomIndex < bottomRecommendations.size()) {
            String imageUrl = bottomRecommendations.get(currentBottomIndex).getUrl();
            Glide.with(this)
                    .load(imageUrl)
                    .into(bottomImg);
        }
    }

    private static class ClothingItem {
        private String id;
        private String category;
        private String url;

        public ClothingItem(String id, String category, String url) {
            this.id = id;
            this.category = category;
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }
}
