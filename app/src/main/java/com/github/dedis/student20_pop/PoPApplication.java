package com.github.dedis.student20_pop;

import android.app.Application;
import android.content.Context;

public class PoPApplication extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return appContext;
    }

    private static void setAppContext(Context context) {
        appContext = context;
    }
}
