package com.zhi_tech.taipp.devicetestapp.sensor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhi_tech.taipp.devicetestapp.AppDefine;
import com.zhi_tech.taipp.devicetestapp.R;
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
    SensorManager mSm = null;
    Sensor mGyRoscopeSensor;




    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;

    private float[] angle= new float[3];

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gyroscopesensor);

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);

        tvdata = (TextView) findViewById(R.id.gyroscopesensor);

        mBtOk = (Button) findViewById(R.id.gyroscopesensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.gyroscopesensor_bt_failed);
        mBtFailed.setOnClickListener(this);

        mSm = (SensorManager) getSystemService(SENSOR_SERVICE);
        mGyRoscopeSensor = mSm
                .getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE);
        mSm.registerListener(lsn, mGyRoscopeSensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onDestroy() {
        mSm.unregisterListener(lsn);
        super.onDestroy();
    }

    SensorEventListener lsn = new SensorEventListener() {
        public void onAccuracyChanged(android.hardware.Sensor sensor,
                                      int accuracy) {
        }

        public void onSensorChanged(SensorEvent e) {
            if (e.sensor == mGyRoscopeSensor) {

                // tvdata.setText(String.format("X:%+f%nY:%+f%nZ:%+f%n",e.values[SensorManager.DATA_X],e.values[SensorManager.DATA_Y],e.values[SensorManager.DATA_Z]));

                if (timestamp != 0) {
                    final float dT = (e.timestamp - timestamp) * NS2S;
                    angle[0] += e.values[0] * dT;
                    angle[1] += e.values[1] * dT;
                    angle[2] += e.values[2] * dT;

                    tvdata.setText(String.format("X:%+f%nY:%+f%nZ:%+f%n",
                            angle[0], angle[1], angle[2]));
                }
                timestamp = e.timestamp;

            }
        }
    };

    @Override
    public void onClick(View v) {

        Utils.SetPreferences(this, mSp, R.string.gyroscopesensor_name, (v
                .getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS
                : AppDefine.DT_FAILED);
        finish();
    }
}
