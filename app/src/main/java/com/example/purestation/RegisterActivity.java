package com.example.purestation;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RegisterActivity extends AppCompatActivity {
    private String TAG = "RegisterActivity";
    private EditText etEmail, etPasswd, etConPasswd, etName, etPhone, etArduinoId;
    private RadioGroup gender;
    private RadioButton male, female;

    private Button ok_join;
    private ImageButton back;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference mDataBase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 파이어베이스 인증, 리얼타임 데이터베이스
        firebaseAuth = FirebaseAuth.getInstance();
        mDataBase = FirebaseDatabase.getInstance().getReference("User");

        ok_join = findViewById(R.id.ok_join);
        back = findViewById(R.id.back);
        
        etEmail = findViewById(R.id.etEmail);
        etPasswd = findViewById(R.id.etPasswd);
        etConPasswd = findViewById(R.id.etConPasswd);
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etArduinoId = findViewById(R.id.etArduinoId);
        male = findViewById(R.id.male);
        female = findViewById(R.id.female);

        // 로그인 화면으로 돌아가기
        back.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
        
        // 회원가입 완료
        ok_join.setOnClickListener(v -> {
            registerUser();
        });


    }

    // 가입하기
    private void registerUser() {
        // 가입 정보 가져오기
        String email = etEmail.getText().toString().trim(); //앞/뒤 공백 제거
        String passwd = etPasswd.getText().toString().trim();
        String conPasswd = etConPasswd.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String arduinoId = etArduinoId.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(passwd)) {
            Toast.makeText(this, "비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!passwd.equals(conPasswd)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (passwd.length() < 6) {
            Toast.makeText(this, "비밀번호를 6자리 이상 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
            }
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "휴대폰 번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(arduinoId)) {
            Toast.makeText(this, "기기 고유번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }


        // 사용자 가입
        firebaseAuth.createUserWithEmailAndPassword(email, passwd).
                addOnCompleteListener(RegisterActivity.this, task -> {
                    // 가입 성공 시
                    if (task.isSuccessful()) {
                        Log.d(TAG, "회원가입 성공!");
                        Toast.makeText(this, "회원가입이 완료 되었습니다.", Toast.LENGTH_SHORT).show();

                        // 인증된 사용자를 반환, 인증 되어있지 않다면 null 반환
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        UserAccount userAccount = new UserAccount();

                        userAccount.setEmail(firebaseUser.getEmail());
                        userAccount.setPasswd(passwd);
                        userAccount.setName(name);
                        userAccount.setPhone(phone);
                        userAccount.setArduinoId(arduinoId);

                        if (female.isChecked()) {
                            userAccount.setGender("여성");
                        } else if (male.isChecked()) {
                            userAccount.setGender("남성");
                        }

                        // database에 정보 저장
                        mDataBase.child(firebaseUser.getUid()).setValue(userAccount);
                        
                        // 로그인 창으로 이동
                        Intent intent = new Intent(this, LoginActivity.class);
                        startActivity(intent);
                        finish();

                    } else {
                        Log.w(TAG, "회원가입 실패", task.getException());
                        Toast.makeText(this, "다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                    }
                    
                }); // createUserWithEmailAndPassword

        
    } // registerUser()

    // 생년월일 포멧
//    private String formatBirthDate(String birth) {
//        if (birth.length() != 8) {
//            return null;
//        }
//        try {
//            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyyMMdd");
//            SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd");
//            Date date = originalFormat.parse(birth);
//            return targetFormat.format(date);
//        } catch (ParseException e) {
//            Log.e(TAG, "생년월일 변환 오류", e);
//            return null;
//        }
//    }
} // class