package com.mhci.gripandtipforce.application;

import android.app.Application;
import android.content.Context;

import com.mhci.gripandtipforce.model.ProjectConfig;
import com.mhci.gripandtipforce.model.utils.BluetoothManager;
import com.mhci.gripandtipforce.model.utils.CustomizedExceptionHandler;
import com.nullwire.trace.ExceptionHandler;
import com.parse.Parse;

/**
 * Created by lab430 on 16/1/23.
 */
public class GTFApp extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        Context appContext = getApplicationContext();
        ProjectConfig.initSomeVars(appContext);
        Parse.enableLocalDatastore(this);
        Parse.initialize(this);
        BluetoothManager.init(appContext);
//        ExceptionHandler.register(appContext, new CustomizedExceptionHandler(), false);

    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }

}
