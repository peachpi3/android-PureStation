package com.example.purestation;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.security.Provider;
import java.util.Timer;
import java.util.TimerTask;

public class CountDownTimer {
    // UV 램프 타이머 클래스
    int hour, minute, second;
    private Timer timer;
    private TimerTask timerTask;
    private TextView hourTv, minuteTv, secondTv, staticTv;
    private TimerEndListener listener;

    public interface TimerEndListener {
        void onTimerEnd();
    }

    public CountDownTimer(int hour, int minute, int second, TextView hourTv, TextView minuteTv,
                          TextView secondTv, TextView staticTv, TimerEndListener listener) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.hourTv = hourTv;
        this.minuteTv = minuteTv;
        this.secondTv = secondTv;
        this.staticTv = staticTv;
        this.listener = listener;
    }

    public void start() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // 0초 이상일 때
                if (second != 0) {
                    second --;
                } else if (minute != 0) {
                    second = 59;
                    minute --;
                } else if (hour != 0) {
                    second = 59;
                    minute = 59;
                    hour --;
                } // if ~ else

                // 숫자 앞에 0을 붙인다 ( 8 -> 08 )
                final String formattedSecond = (second <= 9) ? "0" + second : Integer.toString(second);
                final String formattedMinute = (minute <= 9) ? "0" + minute : Integer.toString(minute);
                final String formattedHour = (hour <= 9) ? "0" + hour : Integer.toString(hour);


                // 포멧 형태로 setText
                hourTv.post(() -> {
                    hourTv.setText(formattedHour);
                    minuteTv.setText(formattedMinute);
                    secondTv.setText(formattedSecond);
                });

                // 타이머가 끝났을 때
                if (hour == 0 && minute == 0 && second == 0) {
                    timer.cancel(); // 타이머 종료
//                    staticTv.post(() -> staticTv.setText("소독 완료!"));
//                    if (listener != null) {
//                        listener.onTimerEnd();
//                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        staticTv.setText("소독 완료!");
                        if (listener!= null) {
                            listener.onTimerEnd();
                        }
                    });
                } // if
            } // run()
        }; // TimerTask()

        timer.schedule(timerTask, 0, 1000); // timer 실행
    } // start()

}
