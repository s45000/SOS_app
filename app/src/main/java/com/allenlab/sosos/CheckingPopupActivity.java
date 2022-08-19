package com.allenlab.sosos;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class CheckingPopupActivity extends Activity {

    String password;
    int cc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_checking_popup);


        Intent intent = getIntent();
        String pwd = intent.getExtras().getString("pwd");
        cc = intent.getExtras().getInt("cc");
        password = pwd;

        Button b = (Button) findViewById(R.id.button);

        long checkStart = System.currentTimeMillis();
        CountDownTimer countDownTimer = new CountDownTimer(cc*1000,1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                cc--;
                b.setText("확인 (남은시간 : "+cc+")");
            }
            @Override
            public void onFinish() {
                /*try {
                    stopMyService();
                    Intent startIntent = new Intent(getApplicationContext(), DangerDetectingService.class);
                    startIntent.putExtra("result", "timeOut");
                    startIntent.setAction("timeOut");
                    startService(startIntent);
                }catch(Exception e){
                    e.printStackTrace();
                }*/
                finish();
            }
        }.start();
    }

    //확인 버튼 클릭
    public void mOnClick(View v){
        TextView pwdText = (TextView) findViewById(R.id.pwdText);
        try {
            if (isMyServiceRunning(DangerDetectingService.class)) {
                Intent intent = new Intent(getApplicationContext(), DangerDetectingService.class);
                if (pwdText.getText().toString().equals(password)) {
                    intent.setAction("pwdPass");
                    intent.putExtra("result", "pwdPass");
                } else {
                    intent.setAction("pwdFail");
                    intent.putExtra("result", "pwdFail");
                }
                stopMyService();
                startService(intent);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        finish();
    }
    private void stopMyService(){
        Intent stop = new Intent(getApplicationContext(), DangerDetectingService.class);
        stop.setAction("stop");
        startService(stop);
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //바깥레이어 클릭시 안닫히게
        if(event.getAction()==MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        //안드로이드 백버튼 막기
        return;
    }
    @Override
    protected void onUserLeaveHint() {		// 홈 버튼
        super.onUserLeaveHint();
        try {
            stopMyService();
            Intent startIntent = new Intent(getApplicationContext(), DangerDetectingService.class);
            startIntent.putExtra("result", "pwdFail");
            startIntent.setAction("pwdFail");
            startService(startIntent);
        }catch(Exception e){
            e.printStackTrace();
        }
        finish();
    }
}