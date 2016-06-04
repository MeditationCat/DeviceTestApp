package com.zhi_tech.taipp.devicetestapp.sensor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhi_tech.taipp.devicetestapp.AppDefine;
import com.zhi_tech.taipp.devicetestapp.DeviceTestAppService;
import com.zhi_tech.taipp.devicetestapp.OnDataChangedListener;
import com.zhi_tech.taipp.devicetestapp.R;
import com.zhi_tech.taipp.devicetestapp.SensorPackageObject;
import com.zhi_tech.taipp.devicetestapp.Utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by taipp on 5/20/2016.
 */
public class PSensor extends Activity implements View.OnClickListener {

    private Button mBtOk;
    private Button mFailed;
    private TextView mPsensor;
    public final String TAG = "PSensor";

    CountDownTimer mCountDownTimer;
    SharedPreferences mSp;
    private Button mBtCalibrate;

    private DeviceTestAppService dtaService = null;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            dtaService = ((DeviceTestAppService.DtaBinder)service).getService();
            dtaService.setOnDataChangedListener(new OnDataChangedListener() {
                @Override
                public void sensorDataChanged(SensorPackageObject object) {
                    //to get the data from the object.
                    postUpdateHandlerMsg(object);
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            dtaService = null;
        }
    };

    private Handler handler = new Handler();

    private void postUpdateHandlerMsg(final SensorPackageObject object) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                mPsensor.setText(getResources().getString(R.string.proximity) + " " + object.proximitySensor.getProximitySensorValue());
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.psensor);

        Intent intent = new Intent(PSensor.this,DeviceTestAppService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        mBtOk = (Button) findViewById(R.id.psensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mFailed = (Button) findViewById(R.id.psensor_bt_failed);
        mFailed.setOnClickListener(this);
        mPsensor = (TextView) findViewById(R.id.proximity);
        mBtCalibrate = (Button) findViewById(R.id.psensor_calibrate);
        mBtCalibrate.setOnClickListener(this);
        if(mSp.getString(getString(R.string.psensor_name), null) == null){
            finish();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
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
            }catch(Exception e){
                e.printStackTrace();
            }
            return;
        }
        Utils.SetPreferences(this, mSp, R.string.psensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }
}
