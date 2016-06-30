package com.zhi_tech.taipp.devicetestapp.sensor;

import android.app.Activity;
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

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by taipp on 5/20/2016.
 */
public class LSensor extends Activity {
    /** Called when the activity is first created. */
    TextView mAccuracyView = null;
    TextView mValueX = null;
    Button mBtOk;
    Button mBtFailed;
    SharedPreferences mSp;
    private final String TAG = "LSensor";
    private byte okFlag = 0x00;
    private Timer mTimer;
    private TimerTask mTimerTask;
    boolean mCheckDataSuccess;

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
                mAccuracyView.setText(getString(R.string.LSensor_accuracy));
                mValueX.setText(getString(R.string.LSensor_value) + object.lightSensor.getLightSensorValue());
                if (object.lightSensor.getLightSensorValue() < 10) {
                    okFlag |= 0x01;
                } else if (object.lightSensor.getLightSensorValue() < 100) {
                    okFlag |= 0x02;
                } else if (object.lightSensor.getLightSensorValue() < 1000) {
                    okFlag |= 0x04;
                } else if (object.lightSensor.getLightSensorValue() < 2000) {
                    okFlag |= 0x08;
                }
                if ((okFlag & 0xFF) == 0x0F) {
                    mCheckDataSuccess = true;
                    if (mTimer == null) {
                        mTimer = new Timer();
                        mTimer.schedule(mTimerTask, 3 * 1000);

                        mValueX.setTextColor(Color.GREEN);
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
        setContentView(R.layout.lsensor);
        Intent intent = new Intent(LSensor.this,DeviceTestAppService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        mAccuracyView = (TextView) findViewById(R.id.lsensor_accuracy);
        mValueX = (TextView) findViewById(R.id.lsensor_value);
        mBtOk = (Button) findViewById(R.id.lsensor_bt_ok);
        mBtOk.setOnClickListener(cl);
        mBtFailed = (Button) findViewById(R.id.lsensor_bt_failed);
        mBtFailed.setOnClickListener(cl);
        mTimer = null;
        mCheckDataSuccess = false;
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                SaveToReport();
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }

    public View.OnClickListener cl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {/*
            Utils.SetPreferences(getApplicationContext(), mSp, R.string.lsensor_name,
                    (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
            finish();
        */}
    };

    public void SaveToReport() {
        Utils.SetPreferences(this, mSp, R.string.lsensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

}
