package com.zhi_tech.taipp.devicetestapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class BootCompletedReceiver extends BroadcastReceiver {

    private final String TAG = "BootCompletedReceiver";

    public BootCompletedReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + intent.getAction());

        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            context.startService(new Intent(context, DeviceTestAppService.class));
        }
    }
}
