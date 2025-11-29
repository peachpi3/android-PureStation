package com.example.purestation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class OuterActivity extends AppCompatActivity {
    private String TAG = "OuterActivity";

    private FirebaseStorage storage = FirebaseStorage.getInstance(); // 파이어스토어 (옷 사진)
    private StorageReference clothesImg;

    private RecyclerView recyclerView;
    private ClothAdapter adapter;
    private List<String> imageUrls;

    private Button btnAll, btnTop, btnBottom, btnSet;
    private ImageButton btnEdit, btnClickedEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outer);

        // 파이어스토어 경로
        clothesImg = storage.getReference().child("Clothes");

        // imageUrls 리스트를 초기화 (스토리지에서 가져온 다운로드 URL 저장)
        imageUrls = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2열 그리드

        // 어댑터 초기화 및 RecyclerView에 설정
        adapter = new ClothAdapter(this, imageUrls, this::showClothDetail);
        recyclerView.setAdapter(adapter);

        // 카테고리 버튼
        btnAll = findViewById(R.id.btnAll);
        btnTop = findViewById(R.id.btnTop);
        btnBottom = findViewById(R.id.btnBottom);
        btnSet = findViewById(R.id.btnSet);

        // 삭제 버튼 (클릭 시 편집 모드)
        btnEdit = findViewById(R.id.btnEdit);
        btnClickedEdit = findViewById(R.id.btnClickedEdit);
        btnEdit.setOnClickListener(v -> {
            // 어댑터에 편집 모드 설정 전달, UI 갱신
            adapter.setEditMode(true);
            adapter.notifyDataSetChanged();
            btnEdit.setVisibility(View.GONE);
            btnClickedEdit.setVisibility(View.VISIBLE);
        });

        btnClickedEdit.setOnClickListener(v -> {
            // 어댑터에 편집 모드 설정 전달, UI 갱신
            adapter.setEditMode(false);
            adapter.notifyDataSetChanged();
            btnEdit.setVisibility(View.VISIBLE);
            btnClickedEdit.setVisibility(View.GONE);
        });

        imageUrls.clear(); // 기존의 이미지 URL을 초기화
        clothesImg.listAll().addOnSuccessListener(listResult -> {
            for (StorageReference item : listResult.getItems()) {
                if (item.getName().contains("outer")) { // 파일명에 "outer"이 포함된 항목만 추가
                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        Log.d(TAG, "Bottom Image URL: " + uri.toString());
                        imageUrls.add(uri.toString());
                        adapter.notifyDataSetChanged(); // 어댑터 갱신
                    });
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "데이터 가져오기 실패");
        });


        // 홈 화면으로
        ImageButton back_home = findViewById(R.id.back_home);
        back_home.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            startActivity(intent);
        });

        // 전체 화면 전환
        btnAll.setOnClickListener(view -> {
            Intent intent = new Intent(this, AllClosetActivity.class);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            startActivity(intent);
        });


        //상의 화면 전환
        btnTop.setOnClickListener(view -> {
            Intent intent = new Intent(this, TopActivity.class);
            startActivity(intent);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        //하의 화면 전환
        btnBottom.setOnClickListener(view -> {
            Intent intent = new Intent(this, BottomActivity.class);
            startActivity(intent);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        //세트/원피스 화면 전환
        btnSet.setOnClickListener(view -> {
            Intent intent = new Intent(this, SetActivity.class);
            startActivity(intent);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

    }

    private void showClothDetail(String imageUrl) {
        // Intent를 사용하여 옷 상세정보 액티비티로 이동 (추후 구현)
    }

} // class
