package com.allenlab.sosos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StartActivity extends AppCompatActivity {
    private String filename = "dataNote.json";
    private File file;
    private Note note;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        note = loadNote();
        if(note.getTutorial()){
            note.setTutorial(false);
            saveNote(note);
            Intent intent = new Intent(this, TutorialActivity.class);
            startActivity(intent);
            finish();
        }else{
            Intent intent = new Intent(this, PermissionActivity.class);
            startActivity(intent);
            finish();
        }
    }
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
}