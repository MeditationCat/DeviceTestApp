package com.zhi_tech.taipp.devicetestapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.List;

/**
 * Created by taipp on 5/20/2016.
 */
public class AllTest extends Activity {

    boolean mBlueResult = false;
    SharedPreferences mSp;
    private BluetoothAdapter mAdapter = null;
    boolean isregisterReceiver = false;
    HandlerThread mBlueThread = new HandlerThread("blueThread");
    BlueHandler mBlueHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alltest);
        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);

        mBlueThread.start();
        mBlueHandler = new BlueHandler(mBlueThread.getLooper());
        mBlueHandler.post(bluerunnable);

        Intent intent = new Intent();
        intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.KeyCode");
        this.startActivityForResult(intent, AppDefine.DT_KEYCODEID);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        int requestid = -1;
        if (requestCode == AppDefine.DT_KEYCODEID) {
            if (resultCode == RESULT_FIRST_USER) {
                finish();
                return;
            }
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.sensor.GSensor");
            if (isMsensorItemVisible()) {
                requestid = AppDefine.DT_GSENSORID;
            } else {
                requestid = AppDefine.DT_MSENSORID;
            }
        }
        if (requestCode == AppDefine.DT_GSENSORID) {
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.sensor.MSensor");
            requestid = AppDefine.DT_MSENSORID;
        }
        if (requestCode == AppDefine.DT_MSENSORID) {
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.sensor.PSensor");
            if (isLightsensorEnable()) {
                requestid = AppDefine.DT_PSENSORID;
            } else {
                requestid = AppDefine.DT_LSENSORID;
            }
        }
        if (requestCode == AppDefine.DT_PSENSORID) {
            intent.setClassName(this, "com.zhi_tech.taipp.devicetestapp.sensor.LSensor");
            requestid = AppDefine.DT_LSENSORID;
        }
        if (requestCode == AppDefine.DT_LSENSORID) {
            OnFinish();
            return;
        }

        this.startActivityForResult(intent, requestid);
    }

    public void onDestroy() {
        super.onDestroy();
        BackstageDestroy();
    }

    public void BackstageDestroy() {
        mBlueHandler.removeCallbacks(bluerunnable);
        if (isregisterReceiver == true) {
            unregisterReceiver(mReceiver);
        }
        mAdapter.disable();
    }

    public void BlueInit() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mAdapter.enable();
        if (mAdapter.isEnabled() == true) {
            StartReciver();
            while (mAdapter.startDiscovery() == false) {
                mAdapter.startDiscovery();
            }
        } else {
            mBlueHandler.postDelayed(bluerunnable, 3000);
        }
    }

    public void StartReciver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        isregisterReceiver = true;
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mBlueResult = true;
                    if (isregisterReceiver == true) {
                        unregisterReceiver(mReceiver);
                        isregisterReceiver = false;
                    }
                    mAdapter.disable();
                }
            }
        }
    };

    Runnable bluerunnable = new Runnable() {
        @Override
        public void run() {
            BlueInit();
        }
    };

    class BlueHandler extends Handler {
        public BlueHandler() {
        }

        public BlueHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    public void OnFinish() {
        Utils.SetPreferences(this, mSp, R.string.bluetooth_name,
                (mBlueResult == true) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }
    private boolean isMsensorItemVisible() {
        if (mSp.getString(getString(R.string.msensor_name), null) != null) {
            return true;
        }
        return false;
    }
    private boolean isLightsensorEnable() {
        if (mSp.getString(getString(R.string.lsensor_name), null) != null) {
            return true;
        }
        return false;
    }
    private boolean isProximityEnable() {
        if (mSp.getString(getString(R.string.psensor_name), null) != null) {
            return true;
        }
        return false;
    }
}
