package com.mhci.gripandtipforce.model.utils;

import android.util.Log;

import com.nullwire.trace.StackInfo;
import com.nullwire.trace.StackInfoSender;
import com.parse.ParseObject;

import java.util.Collection;

/**
 * Created by lab430 on 16/1/23.
 */
public class CustomizedExceptionHandler implements StackInfoSender{

    public final static String debugTag = "stackTracer";

    public CustomizedExceptionHandler() {

    }

    @Override
    public void submitStackInfos(Collection<StackInfo> collection, String s) {
        Log.d(debugTag,"submit stack info");

        StringBuffer strBuff = new StringBuffer();
        try {
            for (StackInfo stackInfo : collection) {
                ParseObject msgObj = new ParseObject("ExceptionMsg");
                msgObj.put("Exception", stackInfo.getExceptionType());
                strBuff.setLength(0);
                for (String traceInfo : stackInfo.getStacktrace()) {
                    strBuff.append(traceInfo);
                    strBuff.append(",");
                }
                String msg = strBuff.toString();
                msgObj.put("Trace", msg);
                msgObj.saveEventually();
            }
        }
        catch(Exception e) {
            Log.d(debugTag, e.getLocalizedMessage());
        }
    }
}
