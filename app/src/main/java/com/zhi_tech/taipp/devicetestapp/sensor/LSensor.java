package com.zhi_tech.taipp.devicetestapp.sensor;

import android.app.Activity;
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

/**
 * Created by taipp on 5/20/2016.
 */
public class LSensor extends Activity implements SensorEventListener {
    /** Called when the activity is first created. */
    //SensorManager mSensorManager = null;
    //Sensor mLightSensor = null;
    TextView mAccuracyView = null;
    TextView mValueX = null;
    Button mBtOk;
    Button mBtFailed;
    SharedPreferences mSp;
    private final String TAG = "LSensor";

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
                mAccuracyView.setText(getString(R.string.LSensor_accuracy) + 0);
                mValueX.setText(getString(R.string.LSensor_value) + object.lightSensor.getLightSensorValue());
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
        //mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mAccuracyView = (TextView) findViewById(R.id.lsensor_accuracy);
        mValueX = (TextView) findViewById(R.id.lsensor_value);
        mBtOk = (Button) findViewById(R.id.lsensor_bt_ok);
        mBtOk.setOnClickListener(cl);
        mBtFailed = (Button) findViewById(R.id.lsensor_bt_failed);
        mBtFailed.setOnClickListener(cl);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        //mSensorManager.unregisterListener(this, mLightSensor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        //mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_LIGHT) {
            mAccuracyView.setText(getString(R.string.LSensor_accuracy) + accuracy);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float[] values = event.values;
            mValueX.setText(getString(R.string.LSensor_value) + values[0]);
        }
    }

    public View.OnClickListener cl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Utils.SetPreferences(getApplicationContext(), mSp, R.string.lsensor_name,
                    (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
            finish();
        }
    };
}
