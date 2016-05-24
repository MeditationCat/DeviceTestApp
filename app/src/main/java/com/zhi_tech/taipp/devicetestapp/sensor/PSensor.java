package com.zhi_tech.taipp.devicetestapp.sensor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhi_tech.taipp.devicetestapp.AppDefine;
import com.zhi_tech.taipp.devicetestapp.R;
import com.zhi_tech.taipp.devicetestapp.Utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by taipp on 5/20/2016.
 */
public class PSensor extends Activity implements SensorEventListener, View.OnClickListener {

    private SensorManager sensorManager;
    private Button mBtOk;
    private Button mFailed;
    private TextView mPsensor;
    public final String TAG = "PSensor";
    private int[] mAllPsensor = new int[1000];
    private static int mCount = 0;
    private int mPrePsensor = 0;
    private int mAverage = 0;
    private char[] mWrint = new char[1];
    private int mSumPsensor = 0;
    private Handler myHandler;
    CountDownTimer mCountDownTimer;
    SharedPreferences mSp;
    private Button mBtCalibrate;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        setContentView(R.layout.psensor);
        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.psensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mFailed = (Button) findViewById(R.id.psensor_bt_failed);
        mFailed.setOnClickListener(this);
        mPsensor = (TextView) findViewById(R.id.proximity);
        myHandler = new Handler();
        mBtCalibrate = (Button) findViewById(R.id.psensor_calibrate);
        mBtCalibrate.setOnClickListener(this);
        //myHandler.post(myRunnable);
        if(mSp.getString(getString(R.string.psensor_name), null) == null){
            finish();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        //myHandler.removeCallbacks(myRunnable);
    }

    public Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            File file = new File("/sys/bus/platform/drivers/als_ps/ps");
            if (file.exists()) {
                String pSensorValues2 = readFile(file);
                mPrePsensor = Integer.parseInt(pSensorValues2.trim());
                mAllPsensor[mCount] = mPrePsensor;
                mCount++;
                mPsensor.setText(getResources().getString(R.string.proximity) + " " + mPrePsensor);
            }
            for (int i = 0; i < mCount; i++) {
                mSumPsensor = mSumPsensor + mAllPsensor[i];
                mAllPsensor[i] = 0;
            }
            if (mCount > 0) {
                mAverage = mSumPsensor / mCount + 1;
                mWrint[0] = (char) mAverage;
            }
            mCount = 0;
            mSumPsensor = 0;
            myHandler.post(myRunnable);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : sensors) {
            sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private static String readFile(File fn) {
        FileReader f;
        int len;

        f = null;
        try {
            f = new FileReader(fn);
            String s = "";
            char[] cbuf = new char[200];
            while ((len = f.read(cbuf, 0, cbuf.length)) >= 0) {
                s += String.valueOf(cbuf, 0, len);
            }
            return s;
        } catch (IOException ex) {
            return "0";
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ex) {
                    return "0";
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            mPsensor.setText(getResources().getString(R.string.proximity) + " " + distance);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == mBtCalibrate.getId()){
            try{
                //Intent intent = new Intent("android.intent.action.PSENSOR_CALIBRATE");
                //intent.addCategory(Intent.CATEGORY_DEFAULT);
                //intent.putExtra("fromWhere", "DeviceTestApp");
                //startActivity(intent);
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setComponent(new ComponentName(
                        "com.zhi_tech.taipp.devicetestapp.sensor",
                        "com.zhi_tech.taipp.devicetestapp.sensor.PSensorCalibration"));
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.putExtra("fromWhere", "DeviceTestApp");
                startActivity(i);
            }catch(ActivityNotFoundException e){
            }catch(SecurityException e){
            }
            return;
        }
        Utils.SetPreferences(this, mSp, R.string.psensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }
}
