package com.example.purestation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ModifyActivity extends AppCompatActivity {
    private String TAG = "ModifyActivity";
    private SharedPreferences preferences;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private ImageButton btnClose, btnLogout;
    private Button btnModify;
    private TextView tvEmail;
    private EditText etPasswd, etName, etPhone, etArduino;
    private RadioButton male, female;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 바 제거
        setContentView(R.layout.activity_modify);

        // 파이어베이스 초기화
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        btnClose = findViewById(R.id.btnClose);
        btnModify = findViewById(R.id.btnModify);
        btnLogout = findViewById(R.id.btnLogout);

        tvEmail = findViewById(R.id.tvEmail);
        etPasswd = findViewById(R.id.etPasswd);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etArduino = findViewById(R.id.etArduino);
        male = findViewById(R.id.male);
        female = findViewById(R.id.female);

        // SharedPreferences에서 사용자 정보 불러오기
        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        String name = sharedPreferences.getString("NAME", "");
        String email = sharedPreferences.getString("EMAIL", "");
        String passwd = sharedPreferences.getString("PASSWD", "");
        String phone = sharedPreferences.getString("PHONE", "");
        String arduinoId = sharedPreferences.getString("ARDUINOID", "");
        String gender = sharedPreferences.getString("GENDER", "");

        // 정보를 텍스트뷰에 설정
        if (firebaseUser != null) {
            Log.w(TAG, "데이터를 가져왔습니다.");
            tvEmail.setText(email);
            etPasswd.setText(passwd);
            etName.setText(name);
            etPhone.setText(phone);
            etArduino.setText(arduinoId);

            if (gender != null) {
                if (gender.equals("여성")) {
                    female.setChecked(true);
                } else if (gender.equals("남성")) {
                    male.setChecked(true);
                }
            }
        }

        //엑스 - 홈 화면으로
        btnClose.setOnClickListener(v -> {
            Intent intent = new Intent(ModifyActivity.this, MainActivity.class);
            startActivity(intent);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        // 완료 - 홈 화면으로
        btnModify.setOnClickListener(v -> {
            Intent intent = new Intent(ModifyActivity.this, MainActivity.class);
            startActivity(intent);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        //로그아웃
        btnLogout.setOnClickListener(v -> {
            firebaseAuth.signOut(); // 인증 로그아웃
            Toast.makeText(this, "로그아웃 완료", Toast.LENGTH_LONG).show();

            // SharedPreferences에서 사용자 정보 삭제
            SharedPreferences sharedPreferences1 = getSharedPreferences("user", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences1.edit();
            editor.clear(); // 모든 데이터 삭제
            editor.apply();

            // 로그인 화면으로 이동
            Intent intent = new Intent(ModifyActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });


    }
}