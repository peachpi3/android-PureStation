package com.example.purestation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.jetbrains.annotations.Nullable;

public class DetailActivty extends AppCompatActivity {
    private DatabaseReference cloth = FirebaseDatabase.getInstance().getReference("Clothes");
    private TextView colorInfo, materialInfo, seasonInfo, washingInfo, cautionInfo;
    private ImageButton clothImg,  backHome;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        colorInfo = findViewById(R.id.colorInfo);
        materialInfo = findViewById(R.id.materialInfo);
        seasonInfo = findViewById(R.id.seasonInfo);
        washingInfo = findViewById(R.id.washingInfo);
        cautionInfo = findViewById(R.id.cautionInfo);
        clothImg = findViewById(R.id.clothImg);
        backHome = findViewById(R.id.back_home);

        // 옷장 화면으로
        backHome.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), AllClosetActivity.class);
            startActivity(intent);
            finish();
        });

        // ClothAdapter에서 전달받은 Intent로 데이터 가져오기
        Intent intent = getIntent();
        String imageUrl = intent.getStringExtra("imageUrl");
        String color = intent.getStringExtra("color");
        String material = intent.getStringExtra("material");
        String season = intent.getStringExtra("season");
        String washing = intent.getStringExtra("washing");
        String caution = intent.getStringExtra("caution");

        // 데이터를 뷰에 설정
        Glide.with(this).load(imageUrl).into(clothImg);
        colorInfo.setText(color);
        materialInfo.setText(material);
        seasonInfo.setText(season);
        washingInfo.setText(washing);
        cautionInfo.setText(caution);

    }
}