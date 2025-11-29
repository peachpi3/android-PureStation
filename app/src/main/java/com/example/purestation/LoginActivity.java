package com.example.purestation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.jetbrains.annotations.Nullable;

public class LoginActivity extends AppCompatActivity {
    private String TAG = "LoginActivity";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference mDatabase;

    private EditText etEmail, etPasswd;
    private ImageButton login, register;
    private CheckBox loginMaintain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 사용자가 이미 로그인했는지 확인
        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        String email = sharedPreferences.getString("EMAIL", null);

        if (email != null) {
            // 사용자가 로그인되어 있으므로 MainActivity로 바로 이동
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish(); // LoginActivity 닫기
            return; // onCreate 종료
        }

        // 파이어베이스 초기화
        firebaseAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPasswd = findViewById(R.id.etPasswd);
        login = findViewById(R.id.btnLogin);
        register = findViewById(R.id.btnRegister);
        loginMaintain = findViewById(R.id.loginMaintain);

        // 앱이 시작될 때 SharedPreferences에서 로그인 상태 확인
        SharedPreferences sharedPreferences1 = getSharedPreferences("user", MODE_PRIVATE);
        boolean isRemembered = sharedPreferences1.getBoolean("IS_REMEMBERED", false);

        if (isRemembered) {
            // 저장된 사용자 정보 불러오기
            String reEmail = sharedPreferences1.getString("EMAIL", "");
            String rePasswd = sharedPreferences1.getString("PASSWD", "");
            etEmail.setText(reEmail);
            etPasswd.setText(rePasswd);
            // 자동 로그인 수행
            loginUser(email, rePasswd);
        }

        // 로그인
        login.setOnClickListener(v -> {
            String emailInput = etEmail.getText().toString();
            String passwdInput = etPasswd.getText().toString();

            // 입력값 체크
            if (emailInput.isEmpty() || passwdInput.isEmpty()) {
                Toast.makeText(LoginActivity.this, "로그인 정보를 입력하세요.", Toast.LENGTH_SHORT).show();
                return; // 입력이 비어있으면 함수 종료
            }

            loginUser(emailInput, passwdInput);
        });


        // 회원가입
        register.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });




    } // onCreate()

    private void loginUser (String email, String passwd) {
        firebaseAuth.signInWithEmailAndPassword(etEmail.getText().toString(), etPasswd.getText().toString())
                .addOnCompleteListener(this, task -> { // 로그인 성공 시
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "로그인 완료", Toast.LENGTH_SHORT).show();
                        firebaseUser = firebaseAuth.getCurrentUser();
                        mDatabase = FirebaseDatabase.getInstance()
                                .getReference("User")
                                .child(firebaseUser.getUid());

                        mDatabase.get().addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                UserAccount userAccount = task1.getResult().getValue(UserAccount.class);

                                if (userAccount != null) {
                                    // 사용자 정보를 SharedPreferences에 저장
                                    SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("NAME", userAccount.getName());
                                    editor.putString("EMAIL", userAccount.getEmail());
                                    editor.putString("PASSWD", userAccount.getPasswd());
                                    editor.putString("PHONE", userAccount.getPhone());
                                    editor.putString("ARDUINOID", userAccount.getArduinoId());
                                    editor.putString("GENDER", userAccount.getGender());

                                    // 로그인 유지 여부 체크박스 상태에 따라 설정
                                    if (loginMaintain.isChecked()) {
                                        editor.putBoolean("IS_REMEMBERED", true);
                                    } else {
                                        editor.putBoolean("IS_REMEMBERED", false);
                                    }

                                    editor.apply();

                                    // 메인 화면으로 이동
                                    Intent intent = new Intent(this, MainActivity.class);
                                    startActivity(intent);
                                    finish();

                                }
                            } else {
                                Log.e(TAG, "사용자 정보를 가져오지 못 했습니다.");
                            }
                        });
                    } else { // 로그인 실패 시
                        Toast.makeText(this, "다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
} // class
