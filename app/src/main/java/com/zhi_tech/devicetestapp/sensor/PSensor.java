package com.zhi_tech.devicetestapp.sensor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
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

import com.zhi_tech.devicetestapp.AppDefine;
import com.zhi_tech.devicetestapp.DeviceTestApp;
import com.zhi_tech.devicetestapp.DeviceTestAppService;
import com.zhi_tech.devicetestapp.OnDataChangedListener;
import com.zhi_tech.devicetestapp.R;
import com.zhi_tech.devicetestapp.SensorPackageObject;
import com.zhi_tech.devicetestapp.Utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by taipp on 5/20/2016.
 */
public class PSensor extends Activity implements View.OnClickListener {

    private Button mBtOk;
    private Button mBtFailed;
    private TextView mPsensor;
    public final String TAG = "PSensor";
    private byte okFlag = 0x00;
    private static int Proximity_Threshold_Leave = 200;
    private static int Proximity_Threshold_Approach = 820;
    private Timer mTimer;
    private TimerTask mTimerTask;
    boolean mCheckDataSuccess;

    CountDownTimer mCountDownTimer;
    SharedPreferences mSp;

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
                mPsensor.setText(String.format(Locale.US, "%s:%n%s %d", getString(R.string.psensor_hello), getString(R.string.proximity),
                        object.proximitySensor.getProximitySensorValue()));
                if (object.proximitySensor.getProximitySensorValue() < Proximity_Threshold_Leave) {
                    //leave away
                    okFlag |= 0x01;
                }
                if (object.proximitySensor.getProximitySensorValue() > Proximity_Threshold_Approach) {
                    //approach
                    okFlag |= 0x02;
                }

                if ((okFlag & 0xFF) == 0x03) {
                    mCheckDataSuccess = true;
                    if (mTimer == null) {
                        mTimer = new Timer();
                        mTimer.schedule(mTimerTask, DeviceTestApp.ShowItemTestResultTimeout * 1000);

                        mPsensor.setTextColor(Color.GREEN);
                        mBtFailed.setBackgroundColor(Color.GRAY);
                        mBtFailed.setClickable(false);
                        mBtOk.setBackgroundColor(Color.GREEN);
                    }
                }
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
        mBtFailed = (Button) findViewById(R.id.psensor_bt_failed);
        mBtFailed.setOnClickListener(this);
        mBtOk.setClickable(false);
        mBtFailed.setClickable(false);

        mPsensor = (TextView) findViewById(R.id.proximity);
        mPsensor.setText(String.format(Locale.US, "%s:%n%s %d", getString(R.string.psensor_hello), getString(R.string.proximity), 0));

        Proximity_Threshold_Approach = DeviceTestApp.Proximity_Threshold_Approach;

        mTimer = null;
        mCheckDataSuccess = false;
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                SaveToReport();
            }
        };

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SaveToReport();
            }
        }, DeviceTestApp.ItemTestTimeout * 1000);
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
        Utils.SetPreferences(this, mSp, R.string.psensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

    public void SaveToReport() {
        Utils.SetPreferences(this, mSp, R.string.psensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }
}
