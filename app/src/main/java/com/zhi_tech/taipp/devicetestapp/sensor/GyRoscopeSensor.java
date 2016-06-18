package com.zhi_tech.taipp.devicetestapp.sensor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
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

/*
 *
 * 陀螺仪的XYZ分别代表设备围绕XYZ三个轴旋转的角速度：radians/second。至于XYZ使用的坐标系与gsensor相同。
 * 逆时针方向旋转时，XYZ的值是正的
 *
 * */
public class GyRoscopeSensor extends Activity implements View.OnClickListener {

    private TextView tvdata;
    private Button mBtOk;
    private Button mBtFailed;

    SharedPreferences mSp;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private static final int Gyro_Sensitivity = 131; // LSB/(º/s)
    private final static int FullScale_Range = 300; // º/s

    private float[] angle= new float[3];
    private final String TAG = "GyRoscopeSensor";

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

                @Override
                public void sendsorCommandReturnValue(int cmd, int value) {

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
                /*
                if (timestamp != 0) {
                    final float dT = (object.getTimestamp() - timestamp) * NS2S;
                    angle[0] += object.gyroscopeSensor.getX() * dT;
                    angle[1] += object.gyroscopeSensor.getY() * dT;
                    angle[2] += object.gyroscopeSensor.getZ() * dT;

                    tvdata.setText(String.format("X:%+f%nY:%+f%nZ:%+f%n", angle[0], angle[1], angle[2]));
                }
                timestamp = object.getTimestamp();*/
                angle[0] = (float) object.gyroscopeSensor.getX() / Gyro_Sensitivity;
                angle[1] = (float) object.gyroscopeSensor.getY() / Gyro_Sensitivity;
                angle[2] = (float) object.gyroscopeSensor.getZ() / Gyro_Sensitivity;

                //Log.d(TAG, String.format("Gyro: %d, %d, %d", object.gyroscopeSensor.getX(), object.gyroscopeSensor.getY(), object.gyroscopeSensor.getZ()));
                if (Math.abs(angle[0]) > FullScale_Range
                        || Math.abs(angle[1]) > FullScale_Range || Math.abs(angle[2]) > FullScale_Range) {
                    //Log.d(TAG,String.format("X: %+f Y: %+f Z: %+f ",angle[0],angle[1],angle[2]));
                    tvdata.setTextColor(Color.RED);
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setClickable(false);
                    mBtOk.setBackgroundColor(Color.GRAY);
                }

                tvdata.setText(String.format("Gyroscope Sensor Data:%nX: %+f%nY: %+f%nZ: %+f%n", angle[0], angle[1], angle[2]));
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.gyroscopesensor);

        Intent intent = new Intent(GyRoscopeSensor.this,DeviceTestAppService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        tvdata = (TextView) findViewById(R.id.gyroscopesensor);
        tvdata.setText(String.format("Gyroscope Sensor Data:%nX: %+f%nY: %+f%nZ: %+f%n", angle[0], angle[1], angle[2]));
        mBtOk = (Button) findViewById(R.id.gyroscopesensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.gyroscopesensor_bt_failed);
        mBtFailed.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        unbindService(conn);
    }

    @Override
    public void onClick(View v) {

        Utils.SetPreferences(this, mSp, R.string.gyroscopesensor_name, (v
                .getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS
                : AppDefine.DT_FAILED);
        finish();
    }
}
