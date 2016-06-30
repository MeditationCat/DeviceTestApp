package com.zhi_tech.taipp.devicetestapp.bluetooth;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.zhi_tech.taipp.devicetestapp.AppDefine;
import com.zhi_tech.taipp.devicetestapp.R;
import com.zhi_tech.taipp.devicetestapp.Utils;

/**
 * Created by taipp on 5/23/2016.
 */
public class Bluetooth extends Activity implements OnClickListener {

    private BluetoothAdapter mAdapter = null;
    private TextView mTvInfo = null;
    private TextView mTvResult = null;
    private TextView mTvCon = null;
    private Button mBtOk;
    private Button mBtFailed;
    private String mNameList = "";
    private boolean mBlueFlag = false;
    private SharedPreferences mSp;
    HandlerThread mBlueThread = new HandlerThread("blueThread");
    BlueHandler mBlueHandler;
    Message msg = null;
    public final String TAG = "Bluetooth";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.ble_test);
        mTvInfo = (TextView) findViewById(R.id.ble_state_id);
        mTvResult = (TextView) findViewById(R.id.ble_result_id);
        mTvCon = (TextView) findViewById(R.id.ble_con_id);
        mBtOk = (Button) findViewById(R.id.ble_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.ble_bt_failed);
        mBtFailed.setOnClickListener(this);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mBlueThread.start();
        mBlueHandler = new BlueHandler(mBlueThread.getLooper());
        mBlueHandler.post(bluerunnable);
        mTvInfo.setText(R.string.Bluetooth_opening);
        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mTvInfo.setText(R.string.Bluetooth_open);
            mTvResult.setText(R.string.Bluetooth_scaning);
            mBlueHandler.removeCallbacks(bluerunnable);
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
            IntentFilter filter_finished = new IntentFilter(
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter_finished);
            mBlueFlag = true;
            while (mAdapter.startDiscovery() == false) {
                mAdapter.startDiscovery();
            }
        }
    };

    Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            if (mAdapter.enable()) {
                mHandler.removeCallbacks(myRunnable);
                init();
            } else {
                mHandler.post(myRunnable);
            }
        }
    };

    private void init() {
        mAdapter.enable();
        if (mAdapter.isEnabled() == true) {
            msg = mHandler.obtainMessage();
            msg.sendToTarget();
        } else {
            mBlueHandler.postDelayed(bluerunnable, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        if(mBlueFlag == true){
            unregisterReceiver(mReceiver);
        }
        mAdapter.disable();
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNameList += device.getName() + "--" + getString(R.string.Bluetooth_mac)
                            + device.getAddress() + "\n";
                    mTvResult.setText(mNameList);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mTvCon.setText(R.string.Bluetooth_scan_success);
            }
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

    Runnable bluerunnable = new Runnable() {
        @Override
        public void run() {
            init();
        }
    };

    @Override
    public void onClick(View v) {/*
        Utils.SetPreferences(this, mSp, R.string.bluetooth_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    */}
}
