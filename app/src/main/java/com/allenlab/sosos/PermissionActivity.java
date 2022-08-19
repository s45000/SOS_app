package com.allenlab.sosos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class PermissionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        // 퍼미션 요청
        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.VIBRATE,
                android.Manifest.permission.FOREGROUND_SERVICE
        }, 1);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED
                        && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    long start = System.currentTimeMillis();
                    while(System.currentTimeMillis()-start < 2000){}
                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.SEND_SMS,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.VIBRATE,
                            android.Manifest.permission.FOREGROUND_SERVICE
                    }, 1);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}