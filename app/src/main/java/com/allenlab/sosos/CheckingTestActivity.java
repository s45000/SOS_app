package com.allenlab.sosos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class CheckingTestActivity extends Activity {

    String password;
    int cc;

    private String filename = "dataNote.json";
    private File file;
    private Note note;

    private boolean isEnd = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_checking_test);

        note = loadNote();

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
                if(!isEnd){
                    sendMessage("위험감지 SOS 설정 테스트 중입니다.");
                }
                isEnd = true;
                finish();
            }
        }.start();
    }
    private boolean isPhoneNumber(String pn){
        boolean regex1 = Pattern.matches("^11(?:2|4|9)$", pn);
        boolean regex2 = Pattern.matches("^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", pn);
        boolean regex3 = Pattern.matches("^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", pn);

        return regex1 || regex2 || regex3;
    }
    private void sendMessage(String s){
        try {
            //전송
            String[] pns = {note.getFirstPhone(), note.getSecondPhone(), note.getThirdPhone()};
            for(String pn : pns) {
                if(isPhoneNumber(pn)){
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(pn, null, s, null, null);
                    Log.d("danger","true : "+pn);
                }
            }
            Toast.makeText(getApplicationContext(), "테스트 메시지를 전달했습니다.", Toast.LENGTH_LONG).show();
        } catch (Exception err) {
            Toast.makeText(getApplicationContext(), "SMS faild, please try again later!", Toast.LENGTH_LONG).show();
            Log.e("err",err.toString());
        }
    }
    public Note loadNote(){
        Note note = new Note();
        file = new File(getApplication().getFilesDir(), filename);
        try {
            String data = getStringFromFile(file.getPath());
            JSONObject jObj = new JSONObject(data);

            int s = jObj.getInt("sensitive");
            String p = jObj.getString("password");
            String ps = jObj.getString("phone1");
            String pss = jObj.getString("phone2");
            String psss = jObj.getString("phone3");
            boolean t = jObj.getBoolean("tut");
            //Log.d("danger"," \ns : "+s+"\np : "+p+"\nps : "+ps+", "+pss+", "+psss+", "+"\nt : "+t);
            // loadNote
            note.setSensitive(s);
            note.setPassword(p);
            note.setFirstPhone(ps);
            note.setSecondPhone(pss);
            note.setThirdPhone(psss);
            note.setTutorial(t);
        }catch (Exception e){
            e.printStackTrace();
        }
        return note;
    }
    public static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
    //확인 버튼 클릭
    public void mOnClick(View v){
        TextView pwdText = (TextView) findViewById(R.id.pwdText);
        if (pwdText.getText().toString().equals(password)) {
            Toast toast = Toast.makeText(getApplicationContext(), "비밀번호가 맞았습니다.", Toast.LENGTH_LONG);
            toast.show();
        } else {
            sendMessage("위험감지 SOS 설정 테스트 중입니다.");
            Toast toast = Toast.makeText(getApplicationContext(), "비밀번호가 틀렸습니다.", Toast.LENGTH_LONG);
            toast.show();
        }
        isEnd = true;
        finish();
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
        sendMessage("위험감지 SOS 설정 테스트 중입니다.");
        Toast toast = Toast.makeText(getApplicationContext(), "대답하지 않고 강제종료 하셨습니다.", Toast.LENGTH_LONG);
        toast.show();
        isEnd = true;
        finish();
    }
}