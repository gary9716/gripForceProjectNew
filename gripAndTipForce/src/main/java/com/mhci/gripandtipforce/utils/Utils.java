package com.mhci.gripandtipforce.utils;

import android.util.DisplayMetrics;

import com.mhci.gripandtipforce.model.ProjectConfig;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

/**
 * Created by lab430 on 16/1/22.
 */
public class Utils {

    public static int inchToPixels(DisplayMetrics metrics,float inches) {
        return Math.round(inches * metrics.xdpi);
    }

    public static void runAsRoot(String commandStr) {
        try {
            Command command = new Command(0, commandStr);
            RootTools.getShell(true).add(command);
        } catch (Exception e) {
            // something went wrong, deal with it here
            ProjectConfig.useSystemBarHideAndShow = false; //disable this feature
        }
    }

}
