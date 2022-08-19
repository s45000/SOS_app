package com.allenlab.sosos;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SettingActivity extends AppCompatActivity {

    private EditText pwd;
    private EditText pn1;
    private EditText pn2;
    private EditText pn3;
    private RadioGroup sensGroup;

    private String filename = "dataNote.json";
    private File file;
    private Note note;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Action Bar 배경색 변경
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFffb366));
        getSupportActionBar().setTitle(Html.fromHtml("<font color='#000000'><b>설정 화면</b></font>"));

        note = loadNote();
        // view default value set
        pwd = (EditText) findViewById(R.id.pwd);
        pn1 = (EditText) findViewById(R.id.pn1);
        pn2 = (EditText) findViewById(R.id.pn2);
        pn3 = (EditText) findViewById(R.id.pn3);
        sensGroup = (RadioGroup) findViewById(R.id.sensGroup);
        sensGroup.setOnCheckedChangeListener(radioGroupButtonChangeListener);

        Button pb = (Button) findViewById(R.id.pwdButton);
        Button pnb1 = (Button) findViewById(R.id.pn1Button);
        Button pnb2 = (Button) findViewById(R.id.pn2Button);
        Button pnb3 = (Button) findViewById(R.id.pn3Button);
        Button tb = (Button) findViewById(R.id.testButton);
        Button mb = (Button) findViewById(R.id.mapButton);

        pb.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                note.setPassword(pwd.getText().toString());
                saveNote(note);
            }
        });
        pnb1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                note.setFirstPhone(pn1.getText().toString());
                saveNote(note);
            }
        });
        pnb2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                note.setSecondPhone(pn2.getText().toString());
                saveNote(note);
            }
        });
        pnb3.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                note.setThirdPhone(pn3.getText().toString());
                saveNote(note);
            }
        });
        tb.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), CheckingTestActivity.class);
                intent.putExtra("pwd",note.getPassword());
                intent.putExtra("cc",10);
                startActivity(intent);
            }
        });
        mb.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        if(note.getSensitive() == 0)
            sensGroup.check(R.id.sens1);
        else if(note.getSensitive() == 2)
            sensGroup.check(R.id.sens3);
        else
            sensGroup.check(R.id.sens2);
        pwd.setText(note.getPassword());
        pn1.setText(note.getFirstPhone());
        pn2.setText(note.getSecondPhone());
        pn3.setText(note.getThirdPhone());
    }
    RadioGroup.OnCheckedChangeListener radioGroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                if (i == R.id.sens1) {
                    Toast.makeText(getApplicationContext(), "중요한 순간에 감지되지 않을 수 있습니다.\n모든 변경사항은 어플리케이션 재실행시 적용됩니다.", Toast.LENGTH_SHORT).show();
                    note.setSensitive(0);
                } else if (i == R.id.sens2) {
                    Toast.makeText(getApplicationContext(), "모든 변경사항은 어플리케이션 재실행시 적용됩니다.",Toast.LENGTH_SHORT).show();
                    note.setSensitive(1);
                } else {
                    Toast.makeText(getApplicationContext(), "작은 위협도 감지되어 불편할 수 있습니다.\n모든 변경사항은 어플리케이션 재실행시 적용됩니다.", Toast.LENGTH_SHORT).show();
                    note.setSensitive(2);
                }
                saveNote(note);
        }
    };
    public void saveNote(Note note){
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(JsonUtil.toJSon(note).getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("danger", e.toString());
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
    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in,R.anim.slide_out_right);
    }
}