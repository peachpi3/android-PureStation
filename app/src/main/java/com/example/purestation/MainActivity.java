package com.example.purestation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private String TAG = "MainActivity";
    private static final int NOTIFICATION_ID = 1; // 알림 ID

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1; // 위치 권한 허용 코드

    // 위치 요청 변수 선언
    private LocationRequest locationRequest; // 위치 요청 설정
    private LocationCallback locationCallback; // 위치 업데이트 콜백

    private Location lastLocation; // 마지막 위치

    private TextView tvUserName; // 사용자명
    
    private FusedLocationProviderClient fusedLocationClient;
    private TextView today, place, maxTem, minTem; // 날짜, 위치, 최고/최저 기온
    private ImageView weather_icon, noWater, fullWater; // 날씨 아이콘, 만수센서 아이콘

    private int nhour, nminute, nsecond, hour, minute, second;
    private EditText hour_et, minute_et;
    private TextView hour_tv, minute_tv, second_tv, static_tv, colons_tv1, colons_tv2, colons_et, hum, temp, tvHumi, tvDehumi;
    private ImageButton start, started, hum_on, hum_off, uv_on, uv_off, suggestionIcon;

    private int progress, nowHumi;

    private BroadcastReceiver timerReceiver;

    private DatabaseReference
    uvState = FirebaseDatabase.getInstance().getReference("RemoteControl").child("uvState"), // 램프 전원 상태
    uvTimer = FirebaseDatabase.getInstance().getReference("RemoteControl").child("uvTimer"), // 램프 시간
    humState = FirebaseDatabase.getInstance().getReference("RemoteControl").child("humState"), // 가습기 전원 상태
    dehumState = FirebaseDatabase.getInstance().getReference("RemoteControl").child("dehumState"), // 제습기 전원 상태
    wantHum = FirebaseDatabase.getInstance().getReference("RemoteControl").child("wantHum"), // 사용자 지정 습도
    arduinoState = FirebaseDatabase.getInstance().getReference("Arduino"), // 온습도 센서
    humiIsWaterFull = FirebaseDatabase.getInstance().getReference("Arduino").child("humiIsWaterFull"), // 가습기 만수 센서
    dehumiIsWaterFull = FirebaseDatabase.getInstance().getReference("Arduino").child("dehumiIsWaterFull"); // 제습기 만수 센서
    private final int ON = 1;
    private final int OFF = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // UI 컴포넌트 초기화
        tvUserName = findViewById(R.id.tvUserName);

        suggestionIcon = findViewById(R.id.suggestionIcon);

        // 첫 번째 상자 (날씨, 날짜, 옷차림 추천)
        today = findViewById(R.id.today);
        place = findViewById(R.id.place);
        maxTem = findViewById(R.id.high_temper2);
        minTem = findViewById(R.id.lower_temper2);
        weather_icon = findViewById(R.id.weather_icon);

        // 두 번째 상자 (습도 조절)
        hum_on = findViewById(R.id.hum_on);
        hum_off = findViewById(R.id.hum_off);
        hum = findViewById(R.id.hum);
        temp = findViewById(R.id.temp);
        noWater = findViewById(R.id.noWater);
        fullWater = findViewById(R.id.fullWater);
        tvHumi = findViewById(R.id.tvHumi);
        tvDehumi = findViewById(R.id.tvDehumi);

        // 세 번째 상자 (램프)
        hour_et = findViewById(R.id.hour_et);
        minute_et = findViewById(R.id.minute_et);

        hour_tv = findViewById(R.id.hour_tv);
        minute_tv = findViewById(R.id.minute_tv);
        second_tv = findViewById(R.id.second_tv);
        static_tv = findViewById(R.id.static_tv);

        colons_tv1 = findViewById(R.id.colons_tv1);
        colons_tv2 = findViewById(R.id.colons_tv2);
        colons_et = findViewById(R.id.colons_et);

        uv_on = findViewById(R.id.uv_on);
        uv_off = findViewById(R.id.uv_off);
        start = findViewById(R.id.start_btn); // 타이머 버튼
        started = findViewById(R.id.started);

        // SharedPreferences에서 사용자 정보 불러오기
        SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
        String name = sharedPreferences.getString("NAME", "");

        // 이름을 텍스트뷰에 설정
        if (name != null) {
            tvUserName.setText(name);
        }

        // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ 위치, 날짜 및 날씨 ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // LocationRequest 객체를 Builder 패턴으로 생성
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 높은 정확도, 10초마다 위치 업데이트
                .setMinUpdateIntervalMillis(5000) // 최소 10초 간격으로 위치 업데이트 , 1000 = 1초
                .build(); // 빌드하여 LocationRequest 객체 생성

        // 위치 콜백 정의
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e(TAG, "위치를 불러올 수 없습니다.");
                    return;
                }
                Location currentLocation = locationResult.getLastLocation();
                if(currentLocation == null) {
                    Log.e(TAG, "현재 위치가 없습니다.");
                    return;
                }

                // 이전 위치와 현재 위치 비교
                if (!currentLocation.equals(lastLocation)) {
                    // 새로운 위치 가져오기
                    double latitude = locationResult.getLastLocation().getLatitude();
                    double longitude = locationResult.getLastLocation().getLongitude();
                    Log.d(TAG, "위도 : " + latitude + " / 경도 : " + longitude);
                    fetchWeatherData(latitude, longitude); // 날씨 데이터 가져오기
                    getAddressFromLocation(latitude, longitude); // 주소 가져오기

                    // 이전 위치 갱신
                    lastLocation = currentLocation;
                }
            }

        };

        // 위치 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates(); // 권한이 허용되면 위치 업데이트 시작
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE); // 권한 요청
        }

        // 날짜 가져오기
        Date dateNow = Calendar.getInstance().getTime();
        SimpleDateFormat format = new SimpleDateFormat("M월 dd일", Locale.getDefault());
        String date = format.format(dateNow);
        today.setText(date);

        // 옷차림 추천
        suggestionIcon.setOnClickListener(view -> {
            Intent intent = new Intent(this, ClothSuggestions.class);
            startActivity(intent);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
//            finish();
        });

        //옷장 화면 전환
        ImageButton closet_icon = findViewById(R.id.closet_icon);
        closet_icon.setOnClickListener(view -> {
            Intent intent = new Intent(this, AllClosetActivity.class);
            startActivity(intent);
            //다음 화면에 실행할 애니메이션, 현재 화면에 실행할 애니메이션
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
//            finish();
        });


        //정보 수정 팝업
        ImageButton user_icon = findViewById(R.id.user_icon);
        user_icon.setOnClickListener(view -> {
            // 버튼을 눌렀을 때 사용자 정보 넘겨주기
            Intent intent = new Intent(getApplicationContext(), ModifyActivity.class);
            startActivity(intent);
        });



        //ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ 습도 조절 ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
        SeekBar seekBar = findViewById(R.id.seekBar);
        TextView seekbar_value = findViewById(R.id.seekbar_value);
        seekBar.setMin(30);
        seekBar.setMax(70);
//        int step = 5; // seekBar 증가값

        // 초기값 설정
        seekBar.setProgress(50); // 50
        seekbar_value.setText(String.format("%d %%", seekBar.getProgress()));
        
        // 현재 온습도 가져옴
        arduinoState.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Arduino arduino = snapshot.getValue(Arduino.class);
                if (arduino != null) {
                    hum.setText(String.format("%d%%", arduino.getHumidity()));
                    temp.setText(String.format("%dº", arduino.getTemperature()));
                    nowHumi = arduino.getHumidity();
                } else {
                    Log.e(TAG, "humidity data is null.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadPost: onCancelled", error.toException());
            }
        });

        // 원하는 습도 읽기
        wantHum.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer wantHumValue = snapshot.getValue(Integer.class);

                if (wantHumValue != null && wantHumValue > 0) {
                    seekBar.setProgress(wantHumValue);
                    seekbar_value.setText(String.format("%d %%", wantHumValue));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadWantHumValue:onCancelled", error.toException());
            }
        });



        // OnSeekBarChange 리스너 - Seekbar 값 변경시 이벤트처리 Listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // onProgressChange - Seekbar 값 변경될때마다 호출
                Log.d(TAG, String.format("onProgressChanged 값 변경 중 : progress [%d] fromUser [%b]", progress, fromUser));
                seekbar_value.setText(String.format("%d %%", seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // onStartTeackingTouch - SeekBar 값 변경위해 첫 눌림에 호출
                Log.d(TAG, String.format("onStartTrackingTouch 값 변경 시작 : progress [%d]", seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // onStopTrackingTouch - SeekBar 값 변경 끝나고 드래그 떼면 호출
                Log.d(TAG, String.format("onStopTrackingTouch 값 변경 종료: progress [%d]", seekBar.getProgress()));
                progress = seekBar.getProgress();
                seekbar_value.setText(String.format("%d %%", progress));
                
                // 파이어베이스에 값 저장, 전원 아이콘 변경
                // 가습기
                if (progress > nowHumi) { // 지정 습도가 현재 습도보다 높을 때
                    humState.setValue(ON); // 가습기 켜짐
                    dehumState.setValue(OFF);
                    hum_off.setVisibility(View.GONE);
                    hum_on.setVisibility(View.VISIBLE);
                    tvHumi.setTextColor(Color.parseColor("#97D9E1"));
                    tvDehumi.setTextColor(Color.parseColor("#49454F"));

                // 제습기
                } else if (progress < nowHumi) { // 지정 습도가 현재 습도보다 낮을 때
                    dehumState.setValue(ON); // 제습기 켜짐
                    humState.setValue(OFF);
                    hum_off.setVisibility(View.GONE);
                    hum_on.setVisibility(View.VISIBLE);
                    tvDehumi.setTextColor(Color.parseColor("#D9AFD9"));
                    tvHumi.setTextColor(Color.parseColor("#49454F"));
                }

                wantHum.setValue(progress); // 사용자 지정 습도
                hum_off.setVisibility(View.GONE);
                hum_on.setVisibility(View.VISIBLE);

                hum_on.setOnClickListener(v -> {
                    hum_off.setVisibility(View.VISIBLE);
                    hum_on.setVisibility(View.GONE);
                    tvHumi.setTextColor(Color.parseColor("#49454F"));
                    tvDehumi.setTextColor(Color.parseColor("#49454F"));
                    humState.setValue(OFF);
                    dehumState.setValue(OFF);
                    wantHum.setValue(0);
                    seekbar_value.setText("50");
                    seekBar.setProgress(50);
                });
            }
        }); // seekBar

        // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ 만수 센서 ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
        // 가습기
        humiIsWaterFull.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int humiIsWaterFullValue = snapshot.getValue(Integer.class);

                    if (humiIsWaterFullValue == 1) { // 가습기 물이 없을 때
                        noWater.setImageResource(R.drawable.no_water_icon); // "물이 비었음"
                    } else { // 물이 있을 때
                        noWater.setImageResource(R.drawable.no_water_icon2);
                    }

            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "data is not available");
            }
        });

        // 제습기
        dehumiIsWaterFull.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int dehumiIsWaterFullValue = snapshot.getValue(Integer.class);

                    if (dehumiIsWaterFullValue == 1) { // 제습기 물이 가득 찼을 때
                        fullWater.setImageResource(R.drawable.full_water_icon); // "물이 가득참"
                    } else { // 물이 가득 차지 않았을 때
                        noWater.setImageResource(R.drawable.full_water_icon2);
                    }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "data is not available");
            }
        });


        // ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ 램프 타이머 ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
        // 타이머 상태 복원
        restoreTimerState();

        // 브로드캐스트 생성 및 등록
        timerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if ("TIMER_UPDATED".equals(action)) {
                    // 남은 시간 업데이트
                    int hour = intent.getIntExtra("hour", 0);
                    int minute = intent.getIntExtra("minute", 0);
                    int second = intent.getIntExtra("second", 0);

                    // UI 업데이트
                    hour_tv.setText(String.format("%02d", hour));
                    minute_tv.setText(String.format("%02d", minute));
                    second_tv.setText(String.format("%02d", second));


                } else if ("TIMER_FINISHED".equals(action)) {
                    // 타이머 종료시 UI 초기화
                    resetUI();
                    static_tv.setText("소독이 완료 되었습니다!");

                    // 알림채널 생성
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(
                                "TIMER_FINISHED_CHANNEL_ID", // 채널 ID
                                "Timer Finished Channel", // 채널 이름
                                NotificationManager.IMPORTANCE_LOW // 중요도 낮게 설정
                        );
                        channel.setSound(null, null); // 소리 제거
                        notificationManager.createNotificationChannel(channel);
                    }

                    // 소독 완료 알림 생성
                    Notification notification = new NotificationCompat.Builder(context, "TIMER_FINISHED_CHANNEL_ID")
                            .setContentTitle("소독 종료")
                            .setContentText("소독이 완료되었습니다.")
                            .setSmallIcon(R.drawable.uv_icon) // 적절한 아이콘 리소스를 설정
                            .setSound(null) // 소리 제거
                            .setVibrate(new long[0]) // 진동 제거
                            .setAutoCancel(true) // 알림 클릭 시 자동 제거
                            .build();

                    // 알림 표시
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        };

        // 시작 버튼
        start.setOnClickListener(v -> {
            String shour = hour_et.getText().toString();
            String sminute = minute_et.getText().toString();

            // 사용자가 시간 값을 입력하지 않았을 경우
            if (shour.isEmpty() || sminute.isEmpty()) {
                Toast.makeText(this, "소독 시간을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            } // if

            nhour = Integer.parseInt(shour);
            nminute = Integer.parseInt(sminute);
            nsecond = 0; // 초는 0으로 초기화


            if (nminute > 60) {
                Toast.makeText(this, "올바른 시간을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (nhour > 2) {
                Toast.makeText(this, "램프는 한번에 최대 2시간까지 작동 가능합니다.", Toast.LENGTH_LONG).show();
                return;
            }

            // 타이머 서비스 시작
            Intent serviceIntent = new Intent(this, TimerService.class);
            serviceIntent.putExtra("hour", nhour);
            serviceIntent.putExtra("minute", nminute);
            serviceIntent.putExtra("second", nsecond);
            ContextCompat.startForegroundService(this, serviceIntent);

            setUI();

            // 버튼 이미지 변경
            start.setVisibility(View.GONE);
            started.setVisibility(View.VISIBLE);
            uv_on.setVisibility(View.VISIBLE);

            // 입력이 들어왔을 경우 정수로 변환 후 계산
            hour_tv.setText(String.format("%02d", nhour));
            minute_tv.setText(String.format("%02d", nminute));
            second_tv.setText(String.format("%02d", nsecond));

            int totalSeconds = (nhour * 3600) + (nminute * 60) + nsecond;
            uvState.setValue(ON);
            uvTimer.setValue(totalSeconds);

        }); // start

        // 램프가 작동 중일 때 전원 끄기
        uv_on.setOnClickListener(v1 -> {
            Toast.makeText(this, "소독을 종료합니다.", Toast.LENGTH_SHORT).show();

            // 서비스 종료 전에 타이머를 중지
            stopService(new Intent(this, TimerService.class));

            resetUI();

            hour_et.setText(null);
            minute_et.setText(null);

            uvState.setValue(OFF);
            uvTimer.setValue(0);

            // 서비스 중지 브로드캐스트 전송
            Intent timerFinishedIntent = new Intent("TIMER_FINISHED");
            sendBroadcast(timerFinishedIntent);

            // 알림채널 생성
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        "TIMER_CHANNEL_ID", // 채널 ID
                        "Timer Channel", // 채널 이름
                        NotificationManager.IMPORTANCE_LOW // 중요도 낮게 설정
                );

                channel.setSound(null, null); // 소리 제거
                notificationManager.createNotificationChannel(channel);
            }

            // 램프 종료 알림 생성
            Notification notification = new NotificationCompat.Builder(this, "TIMER_CHANNEL_ID")
                    .setContentTitle("소독 종료")
                    .setContentText("소독이 완료되었습니다.")
                    .setSmallIcon(R.drawable.uv_icon)
                    .setSound(null) // 소리 제거
                    .setVibrate(new long[0]) // 진동 제거
                    .setAutoCancel(true) // 알림 클릭 시 자동 제거
                    .build();

            // 알림 표시
            notificationManager.notify(NOTIFICATION_ID, notification);
        });

        // 타이머가 작동 중일 때
        started.setOnClickListener(v1 -> {
            Toast.makeText(this, "램프가 작동 중입니다. 잠시만 기다려주세요!", Toast.LENGTH_LONG).show();
        });

    } // onCreate()

    // 타이머 UI 초기화
    private void resetUI() {
        uv_off.setVisibility(View.VISIBLE);
        uv_on.setVisibility(View.GONE); // 전원 off

        hour_et.setVisibility(View.VISIBLE);
        minute_et.setVisibility(View.VISIBLE);
        colons_et.setVisibility(View.VISIBLE);

        hour_tv.setVisibility(View.GONE);
        minute_tv.setVisibility(View.GONE);
        second_tv.setVisibility(View.GONE);
        colons_tv1.setVisibility(View.GONE);
        colons_tv2.setVisibility(View.GONE);

        start.setVisibility(View.VISIBLE);
        start.setEnabled(true);
        started.setVisibility(View.GONE);

        static_tv.setText("작동 시간을 입력해 주세요.");

        String shour = hour_et.getText().toString();
        String sminute = minute_et.getText().toString();

        if (nhour == 0 && nminute == 0 && nsecond == 0 && !shour.isEmpty() && !sminute.isEmpty()) {
            static_tv.setText("소독이 완료 되었습니다!");
        }

    }

    // 타이머 UI 세팅
    private void setUI() {
        // 타이머 작동시, EditText 요소를 숨기고 TextView 요소를 보이게 설정
        uv_off.setVisibility(View.GONE);
        uv_on.setVisibility(View.VISIBLE); // 전원 on

        hour_et.setVisibility(View.GONE);
        minute_et.setVisibility(View.GONE);
        colons_et.setVisibility(View.GONE);

        hour_tv.setVisibility(View.VISIBLE);
        colons_tv1.setVisibility(View.VISIBLE);
        minute_tv.setVisibility(View.VISIBLE);
        colons_tv2.setVisibility(View.VISIBLE);
        second_tv.setVisibility(View.VISIBLE);

        start.setVisibility(View.GONE);
        started.setVisibility(View.VISIBLE);

        static_tv.setText("소독 중입니다!");
    }

    // 타이머 상태 복원
    private void restoreTimerState() {

        uvTimer.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 데이터 존재 여부
                if (snapshot.exists()) {
                    // 데이터 읽기
                    Integer state = snapshot.getValue(Integer.class);

                    // UI 업데이트
                    // 타이머가 작동 중일 때
                    if (state != null && state == ON) {
                        hour_tv.setText(String.format("%02d", hour));
                        minute_tv.setText(String.format("%02d", minute));
                        second_tv.setText(String.format("%02d", second));

                        setUI();

                        Log.d("TimerState", "Timer is running");


                    } else {
                        // 타이머가 종료 되거나 정지된 경우
                        resetUI();

                        Log.d("TimerState", "Timer is not running");
                    }
                } else { // 데이터가 존재하지 않은 경우
                    resetUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                    // 데이터 읽기 실패 시 처리
                    Log.e("TimerState", "Failed to read timer state", error.toException());
            }
        });
    } // restoreTimerState()

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("TIMER_UPDATED");   // 타이머 실행
        filter.addAction("TIMER_FINISHED");  // 타이머 종료
//        registerReceiver(timerReceiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(timerReceiver, filter);
        }
        // 타이머 상태 복원
//        restoreTimerState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(timerReceiver);
    }

    // 사용자 위치 가져옴
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // 권한 없으면 종료
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper()); // 위치 업데이트 요청
    }

    // 위도, 경도를 상세 주소로 변환
    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.KOREA);

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // 주소를 구성하는 여러 요소 가져오기
                StringBuilder addressString = new StringBuilder();
                if (address.getSubLocality() != null) {
                    addressString.append(address.getSubLocality()).append(" "); // 동
                }
                if (address.getLocality() != null) {
                    addressString.append(address.getLocality()).append(" "); // 시, 구
                }
//                if (address.getAdminArea() != null) {
//                    addressString.append(address.getAdminArea()); // 도, 주
//                }

                place.setText(addressString.toString()); // 주소를 UI에 표시
            } else {
                place.setText("주소를 가져올 수 없습니다.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            place.setText("서비스 이용 불가");
        }
    } // getAddressFromLocation()

    private void fetchWeatherData(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.WEATHER_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        WeatherService weatherService = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = weatherService.getDailyForecast(latitude,longitude, AppConfig.WEATHER_API_KEY, 1);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse weatherResponse = response.body();

                    if (weatherResponse != null && !weatherResponse.data.isEmpty()) {
                        double maxTemp = weatherResponse.data.get(0).max_temp; // 최고 기온
                        double minTemp = weatherResponse.data.get(0).min_temp; // 최저 기온
                        String iconCode = weatherResponse.data.get(0).weather.icon; // 아이콘 코드

                        int max_r = (int) Math.round(maxTemp);
                        int min_r = (int) Math.round(minTemp);

                        String max = max_r + "°";
                        String min = min_r + "°";

                        String iconUrl = "https://www.weatherbit.io/static/img/icons/" + iconCode + ".png";

                        // Glide를 사용하여 아이콘 로드 및 표시
                        Glide.with(MainActivity.this)
                                .load(iconUrl)
                                .into(weather_icon);

                        maxTem.setText(max);
                        minTem.setText(min);

                        // Firebase에 날씨 데이터 저장
                        FirebaseWeatherSaver saver = new FirebaseWeatherSaver();
                        saver.saveWeatherData(max_r, min_r, iconCode); // 최저, 최고 기온, 아이콘 코드를 넘김
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                // 실패 원인 로그 출력
                t.printStackTrace();
                Log.e(TAG, "Failed to get weather data", t);

                // 사용자에게 알림 표시
                runOnUiThread(() -> {
                    place.setText("Unable to retrieve weather data");
                    maxTem.setText("");
                    minTem.setText("");
                    // TV_weatherDescription.setText("");
                    weather_icon.setImageResource(R.drawable.error); // 실패 아이콘 또는 기본 아이콘 설정
                });
            }
        });
    } // fetchWeatherData()

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates(); // 권한 허용되면 위치 업데이트 시작
            } else {
                // 권한이 거부된 경우 처리
                Toast.makeText(this, "권한이 거부 되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 위치를 가져오는 데 실패한 경우
    private void handleLocationError() {
        place.setText("위치 로딩 실패");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback); // 위치 업데이트 중지
    }
} // class