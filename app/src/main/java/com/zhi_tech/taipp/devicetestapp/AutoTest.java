package com.zhi_tech.taipp.devicetestapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by taipp on 5/23/2016.
 */
public class AutoTest extends Activity {

    private final String TAG = "AutoTest";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.autotest);

        Intent intent = new Intent();
        intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.KeyCode");
        this.startActivityForResult(intent, AppDefine.DT_KEYCODEID);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " requestCode:" + requestCode);
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int requestid = -1;
        if (requestCode == AppDefine.DT_KEYCODEID) {
            if (resultCode == RESULT_FIRST_USER) {
                finish();
                return;
            }
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.sensor.GSensor");
            requestid = AppDefine.DT_GSENSORID;
        }
        if (requestCode == AppDefine.DT_GSENSORID) {
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.sensor.MSensor");
            requestid = AppDefine.DT_MSENSORID;
        }
        if (requestCode == AppDefine.DT_MSENSORID) {
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.sensor.LSensor");
            requestid = AppDefine.DT_LSENSORID;
        }
        if (requestCode == AppDefine.DT_LSENSORID) {
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.sensor.PSensor");
            requestid = AppDefine.DT_PSENSORID;
        }
        if (requestCode == AppDefine.DT_PSENSORID) {
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.sensor.GyRoscopeSensor");
            requestid = AppDefine.DT_GYROSCOPESENSORID;
        }
        if (requestCode == AppDefine.DT_GYROSCOPESENSORID) {
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.bluetooth.Bluetooth");
            requestid = AppDefine.DT_BLUETOOTHID;
        }

        if (requestCode == AppDefine.DT_BLUETOOTHID) {
            finish();
            return;
        }

        this.startActivityForResult(intent, requestid);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    private boolean isMsensorEnable(){
        return getResources().getBoolean(R.bool.config_Msensor_available);
    }
    private boolean isLightsensorEnable(){
        return getResources().getBoolean(R.bool.config_LSensor_available);
    }
}
