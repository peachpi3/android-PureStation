package com.example.purestation;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.Nullable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AllClosetActivity extends AppCompatActivity {
    private String TAG = "AllClosetActivity";

    private FirebaseStorage storage = FirebaseStorage.getInstance(); // 파이어스토어 (옷 사진)
    private StorageReference clothesImg;

    private RecyclerView recyclerView;
    private ClothAdapter adapter;
    private List<String> imageUrls;

    private Button btnOuter, btnTop, btnBottom, btnSet;
    private ImageButton btnEdit, btnClickedEdit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allcloset);

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
        btnOuter = findViewById(R.id.btnOuter);
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


        // 홈 화면으로
        ImageButton back_home = findViewById(R.id.back_home);
        back_home.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            startActivity(intent);
        });

        clothesImg.listAll().addOnSuccessListener(listResult -> {
            for (StorageReference item : listResult.getItems()) {
                item.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Image URL: " + uri.toString());
                    imageUrls.add(uri.toString());
                    adapter.notifyDataSetChanged(); // 어댑터 갱신

                });
            } // for
        }).addOnFailureListener(e -> {
            Log.e(TAG, "데이터 가져오기 실패");
        });

        //아우터 화면 전환
        btnOuter.setOnClickListener(view -> {
            Intent intent = new Intent(this, OuterActivity.class);
            startActivity(intent);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
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

    // 옷 삭제
    private void deleteCloth(String imageUrl, int position) {
        // Firebase Storage에서 이미지 URL을 기반으로 파일 경로 추출
        String imagePath = imageUrl.substring(imageUrl.indexOf("/o/") + 3, imageUrl.indexOf("?"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imagePath = URLDecoder.decode(imagePath, StandardCharsets.UTF_8);
        }

        StorageReference imageRef = storage.getReference().child(imagePath);

        // Firebase Storage에서 삭제
        imageRef.delete().addOnSuccessListener(aVoid -> {
            // UI에서 삭제
            imageUrls.remove(position);
            adapter.notifyItemRemoved(position);
            Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error deleting image", e);
        });
    }

    private void showClothDetail(String imageUrl) {
            // Intent를 사용하여 옷 상세정보 액티비티로 이동 (추후 구현)
        }

} // class


