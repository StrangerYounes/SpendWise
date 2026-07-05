package com.corner.takecontrol;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class TakeControlApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Firebase is automatically initialized by the google-services plugin,
        // but we can place other global initializations here.
    }
}
