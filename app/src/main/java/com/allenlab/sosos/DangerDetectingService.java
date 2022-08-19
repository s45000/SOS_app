package com.allenlab.sosos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.os.HandlerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;
import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class DangerDetectingService extends Service implements SensorEventListener {
    public DangerDetectingService() {
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private Handler h;
    private Runnable r;

    private static final String MODEL_FILE = "yamnet.tflite";

    private AudioClassifier mAudioClassifier;
    private AudioRecord mAudioRecord;
    private long classficationInterval = 500;       // 500 = 0.5 sec (샘플링 주기)
    private Handler mHandler;

    private long lastTime;
    private float speed;
    private float lastX;
    private float lastY;
    private float lastZ;
    private float x, y, z;

    private static final int SHAKE_THRESHOLD = 10000000;
    private static final int DATA_X = SensorManager.DATA_X;
    private static final int DATA_Y = SensorManager.DATA_Y;
    private static final int DATA_Z = SensorManager.DATA_Z;

    private SensorManager sensorManager;
    private Sensor accelerormeterSensor;

    private int isStop;
    private int stopLimitTime = 5; // 몇분동안 움직임 없을시 쓰러짐 감지
    private long LastDangerTime;
    private String password;
    private int sensitive;
    private boolean Checking = false;

    private String danger = "";
    private int defaultCount = 20;
    private int checkCount = defaultCount;

    private String filename = "dataNote.json";
    private File file;
    private Note note;

    FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng myLocation;
    private String myAddress;
    private Timer locationUpdateTimer;
    @Override
    public void onCreate(){
        super.onCreate();

        isStop = 0;
        note = loadNote();
        password = note.getPassword();
        sensitive = note.getSensitive();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerormeterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        HandlerThread handlerThread = new HandlerThread("backgroundThread");
        handlerThread.start();
        mHandler = HandlerCompat.createAsync(handlerThread.getLooper());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        startAudioClassification();

        locationUpdateTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                setMyLoacation();
                //Log.d("danger","address : "+myAddress+" latlng : "+myLocation);
            }
        };
        locationUpdateTimer.schedule(timerTask, 0, 10000);
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
    private boolean isPhoneNumber(String pn){
        boolean regex1 = Pattern.matches("^11(?:2|4|9)$", pn);
        boolean regex2 = Pattern.matches("^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", pn);
        boolean regex3 = Pattern.matches("^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", pn);

        return regex1 || regex2 || regex3;
    }
    private void setMyLoacation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
            locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng lastLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        myLocation = lastLocation;
                        Geocoder g = new Geocoder(getApplicationContext());
                        try {
                            if(myLocation!=null){
                                List<Address> fromLocation = g.getFromLocation(myLocation.latitude, myLocation.longitude, 5);
                                myAddress =  fromLocation.get(0).getAddressLine(0);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }
    private void sendMessage(String s){
        try {
            String[] pns = {note.getFirstPhone(), note.getSecondPhone(), note.getThirdPhone()};
            for(String pn : pns) {
                if(isPhoneNumber(pn)){
                    SmsManager smsManager = SmsManager.getDefault();
                    String msg = s+"\n위치는 "+myAddress+" 입니다.";
                    smsManager.sendTextMessage(pn, null, msg, null, null);
                    //Log.d("danger",pn+"\n"+msg);
                }
            }
            detectedEndSign();
            Toast.makeText(getApplicationContext(), "위험을 전달했습니다.", Toast.LENGTH_LONG).show();
        } catch (Exception err) {
            Toast.makeText(getApplicationContext(), "SMS faild, please try again later!", Toast.LENGTH_LONG).show();
        }
    }
    private void startAudioClassification(){
        if(mAudioClassifier != null) return;

        try {
            AudioClassifier classifier = AudioClassifier.createFromFile(this, MODEL_FILE);
            TensorAudio audioTensor = classifier.createInputTensorAudio();

            AudioRecord record = classifier.createAudioRecord();
            record.startRecording();

            Runnable run = new Runnable() {
                @Override
                public void run() {
                    Boolean isSpeach = false;
                    Boolean isScream = false;
                    audioTensor.load(record);
                    List<Classifications> output = classifier.classify(audioTensor);
                    List<Category> filterModelOutput = output.get(0).getCategories();
                    float screamPoints;
                    if(sensitive == 0) {
                        screamPoints = (float) 0.04;
                    }else if(sensitive == 1) {
                        screamPoints = (float) 0.004;
                    }else{
                        screamPoints = (float) 0.0004;
                    }
                    String test = "";
                    boolean stest = false;
                    for(Category c : filterModelOutput) {
                        if(c.getLabel().equals("Speech") || c.getLabel().equals("Squeal") || c.getLabel().equals("Screaming"))
                            test += ".\n"+c.getLabel()+" "+c.getScore();
                        if((c.getLabel().equals("Squeal") && c.getScore() > 0.001) || (c.getLabel().equals("Screaming")&& c.getScore() > 0.001)){
                            stest = true;
                        }
                        if(c.getLabel().equals("Speech") && c.getScore() > 0.02)
                            isSpeach = true;
                        if(isSpeach){
                            if(c.getLabel().equals("Screaming") && c.getScore() > screamPoints) {
                                isScream = true;
                            }
                            if(c.getLabel().equals("Squeal") && c.getScore() > screamPoints){
                                isScream = true;
                            }
                        }//어느정도 사람목소리이자 비명이거나,
                        //완전 비명이면 (0.05이상)
                        if((c.getLabel().equals("Screaming") && c.getScore() > 0.05) || (c.getLabel().equals("Squeal") && c.getScore() > 0.05))
                            isScream = true;
                    }
                    mHandler.postDelayed(this,classficationInterval);
                    if(stest)
                        //Log.d("soundtest",test);
                    if(isScream) {
                        DetectDanger("비명이 감지되었습니다.");
                    }
                }
            };

            mHandler.post(run);
            mAudioClassifier = classifier;
            mAudioRecord = record;
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void DetectDanger(String detected){
        if (!Checking && System.currentTimeMillis() - LastDangerTime > 5000) {
            ((AppData)getApplication()).setTrue(); //메인액티비티 백버튼 비활성화
            Checking = true;
            danger = detected;
            checkCount = defaultCount;
            detectedStartSign();
        }
    }
    private void ansSafe(String result){
        //result : Start, pwdPass, pwdFail, timeOut
        if(result.equals("pwdPass")){
            Toast toast = Toast.makeText(getApplicationContext(), "괜찮으시군요 다행입니다.\n5초지난후 다시 위험감지를 시작합니다.", Toast.LENGTH_LONG);
            toast.show();
        }else if(result.equals("pwdFail") || result.equals("timeOut")){
            //GPS 현재위치 함께 보내기
            sendMessage(danger);
        }
        LastDangerTime = System.currentTimeMillis();
        Checking = false;
        ((AppData)getApplication()).setFalse(); //메인액티비티 백버튼 활성화
    }

    private void stopAudioClassfication(){
        mHandler.removeCallbacksAndMessages(null);
        mAudioRecord.stop();
        mAudioRecord = null;
        mAudioClassifier = null;
    }
    private void detectedStartSign(){
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(500);
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen.startTone(ToneGenerator.TONE_SUP_RADIO_ACK,400);
    }
    private void detectedEndSign(){
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        long[] pattern = {0,500,300,500}; //대기,진동,대기,진동(milisec)
        vibrator.vibrate(pattern,-1);
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGen.startTone(ToneGenerator.TONE_DTMF_C, 400);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long gabOfTime = (currentTime - lastTime);

            if (gabOfTime > 100) {
                lastTime = currentTime;
                x = event.values[SensorManager.DATA_X];
                y = event.values[SensorManager.DATA_Y];
                z = event.values[SensorManager.DATA_Z];

                speed = (Math.abs((float) Math.pow(x - lastX, 3)) + Math.abs((float) Math.pow(y - lastY, 3)) + Math.abs((float) Math.pow(z - lastZ, 3)))
                        / gabOfTime * 10000;

                if (speed > SHAKE_THRESHOLD) { // SHAKE_THRESHOLD = 10000000
                    // 충격감지
                    DetectDanger("저에게 강한 충격이 감지되었습니다.");
                }

                if (speed < 10000) {
                    isStop += gabOfTime;
                    if(isStop>1000000000){
                        isStop = 0;
                    }
                    if (isStop > stopLimitTime * 60 * 1000) {
                        //stopLimitTime(분단위)동안 움직임 없을시 쓰러짐감지
                        DetectDanger("제가 움직임이 멈춘지 "+stopLimitTime+"분 되었습니다.");
                        isStop = 0;
                    }
                } else {
                    isStop = 0;
                }

                lastX = event.values[DATA_X];
                lastY = event.values[DATA_Y];
                lastZ = event.values[DATA_Z];
            }

        }

    }
    private Notification updateNotification() {

        Context context = getApplicationContext();
        PendingIntent action;
        action = PendingIntent.getActivity(context,
                0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_CANCEL_CURRENT); // Flag indicating that if the described PendingIntent already exists, the current one should be canceled before generating a new one.

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            String CHANNEL_ID = "test_channel";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "TestChannel",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Test channel description");
            manager.createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        }
        else
        {
            builder = new NotificationCompat.Builder(context);
        }

        return builder.setContentIntent(action)
                .setContentTitle("위험 감지중..")
                .setSmallIcon(R.drawable.sensor_icon)
                .setOngoing(true)
                .build();
    }
    private Notification checkingNotification() {

        Context context = getApplicationContext();
        PendingIntent action;
        Intent intent = new Intent(context, CheckingPopupActivity.class);
        intent.putExtra("pwd",password);
        intent.putExtra("cc",checkCount);
        action = PendingIntent.getActivity(context,
                0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT); // Flag indicating that if the described PendingIntent already exists, the current one should be canceled before generating a new one.

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            String CHANNEL_ID = "1checking_channel";

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "CheckChannel",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Check channel description");
            manager.createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        }
        else
        {
            builder = new NotificationCompat.Builder(context);
        }

        return builder.setContentIntent(action)
                .setContentTitle("위험 감지됨 ("+checkCount+")")
                .setContentText("괜찮으시다면 클릭해주세요.")
                .setSmallIcon(R.drawable.danger_icon)
                .setOngoing(true)
                .build();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (accelerormeterSensor != null)
            sensorManager.registerListener(this, accelerormeterSensor,
                    SensorManager.SENSOR_DELAY_GAME);

        if (intent.getAction().contains("stop")) { //메인 액티비티 종료시
            if (sensorManager != null)
                sensorManager.unregisterListener(this);
            locationUpdateTimer.cancel();
            h.removeCallbacks(r);
            stopAudioClassfication();
            stopForeground(true);
            stopSelf();
        } else if (intent.getAction().contains("start")) {
            LastDangerTime = System.currentTimeMillis();
            h = new Handler();
            r = new Runnable() {
                @Override
                public void run() {
                    //Log.d("danger","Checking : "+Checking+" msg : "+danger+" cc : "+checkCount);
                    if(Checking){
                        if(checkCount<=0){
                            ansSafe("timeOut");
                            Checking = false;
                            ((AppData)getApplication()).setFalse(); //메인액티비티 백버튼 활성화
                            checkCount = defaultCount;
                        }else {
                            startForeground(1, checkingNotification());
                            checkCount--;
                        }
                    }else {
                        startForeground(2, updateNotification());
                    }
                    h.postDelayed(this, 1000);
                }
            };
            h.post(r);
        } else { // pwdPass, pwdFail, timeOut, Home 액션시
            String res = intent.getStringExtra("result");
            ansSafe(res);
            //Log.d("danger",res);

            Intent startIntent = new Intent(getApplicationContext(), DangerDetectingService.class);
            startIntent.setAction("start");
            startService(startIntent);
        }
        return Service.START_STICKY;
    }
}