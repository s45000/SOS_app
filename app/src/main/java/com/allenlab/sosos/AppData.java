package com.allenlab.sosos;

import android.app.Application;

public class AppData extends Application {
    private boolean bool = false;

    public void setTrue(){
        bool = true;
    }
    public void setFalse(){
        bool = false;
    }
    public boolean isChecking(){
        return bool;
    }
}
