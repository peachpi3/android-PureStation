package com.example.purestation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nullable;

public class TimerService extends Service {

    private static final int NOTIFICATION_ID = 1; // 알림 ID
    private NotificationManager notificationManager;
    private Timer timer;
    private TimerTask timerTask;
    private int hour, minute, second;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference uvStateRef = database.getReference("RemoteControl/uvState");
    DatabaseReference uvTimerRef = database.getReference("RemoteControl/uvTimer");

    // Timer와 TimerTask가 계속 중첩되면 안 되기 때문에,
    // 새로운 타이머를 시작하기 전에 기존 타이머를 명시적으로 취소하고 null로 설정
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0 (Oreo) 이상에서 Notification Channel 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "TIMER_CHANNEL_ID", // 채널 ID
                    "Timer Channel", // 채널 이름
                    NotificationManager.IMPORTANCE_LOW // 중요도 낮게 설정
            );

            channel.setSound(null, null); // 소리 제거
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 타이머가 이미 존재하면 취소
        stopTimer();

        // 인텐트를 통해 타이머 값 가져옴
        hour = intent.getIntExtra("hour", 0);
        minute = intent.getIntExtra("minute", 0);
        second = intent.getIntExtra("second", 0);

        // 타이머 시작
        startTimer();

        //  Foreground Notification 생성 및 시작
        Notification notification = getNotification();
        startForeground(NOTIFICATION_ID, notification);

        return START_NOT_STICKY; // 서비스가 종료될 때 재시작하지 않음
    }

    @Override
    public void onDestroy() {
        // 서비스 종료 시 타이머도 취소
        stopTimer();
        super.onDestroy();
    }

    private void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // 타이머 로직
                if (second > 0) {
                    second--;
                } else if (minute > 0) {
                    second = 59;
                    minute--;
                } else if (hour > 0) {
                    second = 59;
                    minute = 59;
                    hour--;
                }

                // UI 업데이트를 위한 브로드캐스트 전송
                Intent intent = new Intent("TIMER_UPDATED");
                intent.putExtra("hour", hour);
                intent.putExtra("minute", minute);
                intent.putExtra("second", second);
                sendBroadcast(intent);

                // Notification 업데이트
                Notification notification = getNotification();
                notificationManager.notify(NOTIFICATION_ID, notification);


                // 타이머 종료시 서비스 중지
                if (hour == 0 && minute == 0 && second == 0) {
                    timer.cancel();

                    // Firebase에 타이머 종료 상태 저장
                    uvStateRef.setValue(0); // uvState 값을 OFF로 설정
                    uvTimerRef.setValue(0); // uvTimer 값을 0으로 설정

                    // 타이머 종료 알림 생성
                    Notification finishedNotification = new NotificationCompat.Builder(TimerService.this, "TIMER_CHANNEL_ID")
                            .setContentTitle("소독 종료")
                            .setContentText("소독이 완료되었습니다.")
                            .setSmallIcon(R.drawable.uv_icon)
                            .setSound(null) // 소리 제거
                            .setVibrate(new long[0]) // 진동 제거
                            .setAutoCancel(true) // 알림 클릭 시 자동 제거
                            .build();

                    // 알림 표시
                    notificationManager.notify(NOTIFICATION_ID, finishedNotification);

                    // 타이머 종료를 알리는 브로드캐스트 전송
                    Intent timerFinishedIntent = new Intent("TIMER_FINISHED");
                    sendBroadcast(timerFinishedIntent);

                    stopForeground(true); // 포그라운드 상태 해제
                    stopSelf(); // 서비스 종료
                }

            }
        };

        timer.schedule(timerTask, 0, 1000); // 1초마다 타이머 실행
    }

    private Notification getNotification() {
        // Foreground Service를 위한 Notification 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "TIMER_CHANNEL_ID")
                .setContentTitle("램프 가동 중")
                .setContentText("남은 시간: " + String.format("%02d:%02d:%02d", hour, minute, second))
                .setSmallIcon(R.drawable.uv_icon)
                .setSound(null) // 소리 제거
                .setVibrate(new long[0]); // 진동 제거


        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // bind 기능 X
    }
}
