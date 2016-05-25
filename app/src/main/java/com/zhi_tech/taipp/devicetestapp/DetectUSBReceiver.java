package com.zhi_tech.taipp.devicetestapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class DetectUSBReceiver extends BroadcastReceiver {

    private final String TAG = "DetectUSBReceiver";

    public DetectUSBReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + intent.getAction());

        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)
                || intent.getAction().equalsIgnoreCase(Intent.ACTION_UMS_CONNECTED)) {

            context.startService(new Intent(context, DeviceTestAppService.class));
        }

        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_UMS_DISCONNECTED)) {

        }
    }
}
