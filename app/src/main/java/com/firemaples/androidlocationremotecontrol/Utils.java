package com.firemaples.androidlocationremotecontrol;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Louis on 2015/6/29.
 */
public class Utils {
    static String className;
    static String methodName;
    static int lineNumber;

    private static String createLog(String log) {

        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        buffer.append(className);
        buffer.append("=>");
        buffer.append(methodName);
        buffer.append(":");
        buffer.append(lineNumber);
        buffer.append("]");
        buffer.append(log);

        return buffer.toString();
    }

    private static void getMethodNames(StackTraceElement[] sElements) {
        className = sElements[1].getFileName();
        methodName = sElements[1].getMethodName();
        lineNumber = sElements[1].getLineNumber();
    }

    public static void makeTestLog(Context context,String message){
        getMethodNames(new Throwable().getStackTrace());
        Log.i("myLog", createLog(message));
    }

    public static void makeTestToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
