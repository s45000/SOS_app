package com.allenlab.sosos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Random;

import jxl.Sheet;
import jxl.Workbook;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private long backKeyPressedTime = 0;
    private Toast toast;

    private FragmentManager fragmentManager;
    private MapFragment mapFragment;

    private GoogleMap gmap;
    FusedLocationProviderClient fusedLocationProviderClient;
    private boolean onMap = false;

    private ClusterManager<MyItem> clusterManager;
    private String[] headMessage = {"?????? ?????? ?????????","?????? ?????? ?????????","?????????????????? ????????? ??????!","???????????? ????????????..","??????????????? ??? ????????????","??????????????? ???????????? ??????!"};
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu:
                Intent intent = new Intent(this, TutorialActivity.class);
                startActivity(intent);
                return true;
            case R.id.quest:
                Intent setIntent = new Intent(this,SettingActivity.class);
                startActivity(setIntent);
                overridePendingTransition(R.anim.slide_in_right,android.R.anim.fade_out);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            fragmentManager = getFragmentManager();
            mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }catch(Exception e){
            e.printStackTrace();
        }

        /*ActionBar actionBar = getSupportActionBar();
        actionBar.hide();*/
        // Action Bar ????????? ??????
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0xFFffb366));
        // Action Bar ????????? ??????
        int headLength = headMessage.length;
        Random random = new Random();
        String randomMessage = headMessage[random.nextInt(headLength)];
        getSupportActionBar().setTitle(Html.fromHtml("<font color='#000000'><b>"+randomMessage+"</b></font>"));

        String filename = "dataNote.json";
        File file = new File(getApplication().getFilesDir(), filename);
        if(!file.exists()){
            Note note = new Note();
            FileOutputStream outputStream;
            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(JsonUtil.toJSon(note).getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (!isMyServiceRunning(DangerDetectingService.class)) {
                Intent startIntent = new Intent(getApplicationContext(), DangerDetectingService.class);
                startIntent.setAction("start");
                startService(startIntent);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ????????? ??????
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.VIBRATE,
                    android.Manifest.permission.FOREGROUND_SERVICE,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
        }else{
            googleMap.setMyLocationEnabled(true);
        }
        gmap = googleMap;
        gmap.setMinZoomPreference(15);
        gmap.setMaxZoomPreference(18);
        switchLocation();
        onMap = true;
        clusterManager = new ClusterManager<>(this,gmap);
        clusterManager.setRenderer(new BellIconRendered(this, gmap, clusterManager));
        clusterManager.setAnimation(false);
        // ???????????? ????????? ?????????
        clusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MyItem>() {
            @Override
            public boolean onClusterClick(Cluster<MyItem> cluster) {
                LatLngBounds.Builder builder_c = LatLngBounds.builder();
                String text = "";
                int count = 0;
                for (ClusterItem item : cluster.getItems()) {
                    builder_c.include(item.getPosition());
                    text += item.getTitle() + "\n";
                    count += 1;
                }
                if (count <= 5){
                    toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
                    toast.show();
                }
                LatLngBounds bounds_c = builder_c.build();
                gmap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds_c, 1));
                float zoom = gmap.getCameraPosition().zoom - 0.5f;
                gmap.animateCamera(CameraUpdateFactory.zoomTo(zoom));
                return true;
            }
        });
        gmap.setOnCameraIdleListener(clusterManager);
        gmap.setOnMarkerClickListener(clusterManager);
        /*
        for(int i = 1; i<=16; i++){
            String fname = "belldata"+i+".xls";
            excelToMarker(fname,"bell");
        }*/ //?????? ????????? ????????? ?????? ??????
        //excelToMarker("belldata8.xls","bell"); // Line193 ????????????
        excelToMarker("firedata.xls","fire");
        excelToMarker("policedata.xls","police");
    }
    public class BellIconRendered extends DefaultClusterRenderer<MyItem> {
        public BellIconRendered(Context context, GoogleMap map, ClusterManager<MyItem> clusterManager) {
            super(context, map, clusterManager);
        }
        @Override
        protected void onBeforeClusterItemRendered(MyItem item,
                                                   MarkerOptions markerOptions) {
            BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.bell_icon);
            Bitmap b = bitmapdraw.getBitmap();
            Bitmap bellmarker = Bitmap.createScaledBitmap(b, 150, 150, false);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bellmarker));
            markerOptions.title(item.getTitle());
        }
    }

    public void excelToMarker(String filename, String place){
        try {
            InputStream is = getBaseContext().getResources().getAssets().open(filename);
            Workbook wb = Workbook.getWorkbook(is);
            if(wb != null){
                Sheet sheet = wb.getSheet(0);
                if(sheet != null){
                    int colTotal = sheet.getColumns();
                    int rowIndexStart = 1;
                    int rowTotal = sheet.getColumn(colTotal - 1).length;

                    if(place == "bell") {
                        /*BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.bell_icon);
                        Bitmap b = bitmapdraw.getBitmap();
                        Bitmap bellmarker = Bitmap.createScaledBitmap(b, 150, 150, false);*/
                        for (int row = rowIndexStart; row < rowTotal; row++) {
                        //for (int row = 3397; row < 4475; row++) {
                            Double lat = Double.parseDouble(sheet.getCell(7, row).getContents());
                            Double lng = Double.parseDouble(sheet.getCell(8, row).getContents());
                            clusterManager.addItem(new MyItem(lat,lng,sheet.getCell(4, row).getContents()));

                            /*LatLng location = new LatLng(lat, lng);
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.title(sheet.getCell(4, row).getContents());
                            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bellmarker));
                            markerOptions.position(location);
                            gmap.addMarker(markerOptions);
                            Log.d("place",lat+" "+lng);*/
                        }
                    }else{
                        BitmapDrawable bitmapdraw = new BitmapDrawable();
                        if(place == "fire"){
                            bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.firefighter_icon);

                        }else if(place == "police"){
                            bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.police_icon);
                        }
                        Bitmap b = bitmapdraw.getBitmap();
                        Bitmap marker = Bitmap.createScaledBitmap(b, 150, 150, false);
                        for (int row = rowIndexStart; row < rowTotal; row++) {
                            if(!(sheet.getCell(2, row).getContents() == "" || sheet.getCell(3, row).getContents() == "")){
                                String name = sheet.getCell(5, row).getContents();
                                Double lat = Double.parseDouble(sheet.getCell(2, row).getContents());
                                Double lng = Double.parseDouble(sheet.getCell(3, row).getContents());

                                LatLng location = new LatLng(lat, lng);
                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.title(name);
                                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(marker));
                                markerOptions.position(location);
                                gmap.addMarker(markerOptions);
                            }
                        }
                    }
                }
            }
        }catch (Exception e){

        }
    }
    private void switchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ????????? ??????
            return;
        } else {
            Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();

            locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        LatLng lastLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        //Log.d("danger", lastLocation.toString());
                        gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 18));
                    }
                }
            });
        }
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
    public void onBackPressed() {
        // ?????? ?????? ?????? ????????? ????????? ?????? ?????? ?????? ??????
        //super.onBackPressed();
        if(((AppData)getApplication()).isChecking()){
            toast = Toast.makeText(this, "????????? ?????????????????????.", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        // ??????????????? ?????? ?????? ????????? ????????? ????????? 2.5?????? ?????? ?????? ????????? ?????? ???
        // ??????????????? ?????? ?????? ????????? ????????? ????????? 2.5?????? ???????????? Toast ??????
        // 2500 milliseconds = 2.5 seconds
        if (System.currentTimeMillis() > backKeyPressedTime + 2500) {
            backKeyPressedTime = System.currentTimeMillis();
            toast = Toast.makeText(this, "?????? ?????? ????????? ??? ??? ??? ???????????? ???????????????.", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        // ??????????????? ?????? ?????? ????????? ????????? ????????? 2.5?????? ?????? ?????? ????????? ?????? ???
        // ??????????????? ?????? ?????? ????????? ????????? ????????? 2.5?????? ????????? ???????????? ??????
        if (System.currentTimeMillis() <= backKeyPressedTime + 2500) {
            finish();
            toast.cancel();
            toast = Toast.makeText(this, "????????? ????????? ???????????????.", Toast.LENGTH_LONG);
            toast.show();
        }
    }
    @Override
    protected void onRestart(){
        super.onRestart();
        //Log.d("danger","Restart");
    }
    @Override
    protected void onStop(){
        super.onStop();
        //Log.d("danger","stop");
    }
    @Override
    protected void onUserLeaveHint() {        // ??? ??????
        super.onUserLeaveHint();
        //Log.d("danger","mainHomeButton");
    }
    @Override
    protected void onDestroy() {
        ActivityCompat.finishAffinity(this);
        if (isMyServiceRunning(DangerDetectingService.class)){
            Intent stopIntent = new Intent(getApplicationContext(), DangerDetectingService.class);
            stopIntent.setAction("stop");
            startService(stopIntent);
        }
        super.onDestroy();
    }
}