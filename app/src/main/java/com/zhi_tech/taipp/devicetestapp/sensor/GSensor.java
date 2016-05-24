package com.zhi_tech.taipp.devicetestapp.sensor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhi_tech.taipp.devicetestapp.AppDefine;
import com.zhi_tech.taipp.devicetestapp.R;
import com.zhi_tech.taipp.devicetestapp.Utils;

/**
 * Created by taipp on 5/20/2016.
 */
public class GSensor extends Activity implements View.OnClickListener {

    private Button mBtCalibrate;
    private TextView tvdata;
    private ImageView ivimg;
    private Button mBtOk;
    private Button mBtFailed;
    private Button mSmtTest;

    private int mX;
    private int mY;
    private int mZ;

    SharedPreferences mSp;
    SensorManager mSm = null;
    Sensor mGravitySensor;
    boolean mCheckDataSuccess;
    private final static int OFFSET = 2;
    private final String TAG = "GSensor";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.gsensor);
        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        mBtCalibrate = (Button) findViewById(R.id.gsensor_calibrate);
        mBtCalibrate.setOnClickListener(this);
        tvdata = (TextView) findViewById(R.id.gsensor_tv_data);
        ivimg = (ImageView) findViewById(R.id.gsensor_iv_img);
        mBtOk = (Button) findViewById(R.id.gsensor_bt_ok);
        mSmtTest = (Button)findViewById(R.id.smt_test);
        mSmtTest.setOnClickListener(this);
        mSmtTest.setVisibility(View.INVISIBLE);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.gsensor_bt_failed);
        mBtFailed.setOnClickListener(this);
        mSm = (SensorManager) getSystemService(SENSOR_SERVICE);
        mGravitySensor = mSm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
        mSm.registerListener(lsn, mGravitySensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        mSm.unregisterListener(lsn);
        mCheckDataSuccess = false;
    }

    SensorEventListener lsn = new SensorEventListener() {
        public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent e) {
            if (e.sensor == mGravitySensor) {
                tvdata.setText(String.format("X:%+f%nY:%+f%nZ:%+f%n",e.values[SensorManager.DATA_X],e.values[SensorManager.DATA_Y],e.values[SensorManager.DATA_Z]));

                int x = (int) e.values[SensorManager.DATA_X];
                int y = (int) e.values[SensorManager.DATA_Y];
                int z = (int) e.values[SensorManager.DATA_Z];

                mX = x;
                mY = y;
                mZ = z;
                if (Math.abs(x) > Math.abs(y) && Math.abs(x) - OFFSET > Math.abs(z)) {
                    ivimg.setBackgroundResource(x > 0? R.drawable.gsensor_x : R.drawable.gsensor_x_2);
                } else if (Math.abs(y) - OFFSET > Math.abs(x) && Math.abs(y) - OFFSET > Math.abs(z)) {
                    ivimg.setBackgroundResource(y > 0? R.drawable.gsensor_y : R.drawable.gsensor_2y);
                } else if (Math.abs(z) > Math.abs(x) && Math.abs(z) > Math.abs(y)) {
                    ivimg.setBackgroundResource(R.drawable.gsensor_z);
                }
            }
        }
    };

    private boolean IsCheckDataCorrect(){

        if( ((mX<= 4.8) && (mX>= -4.8)) && ((mY<= 4.8) && (mY>= -4.8))&&((mZ<= 14.6) && (mZ>=5)))
            return true;

        return false;
    }
    @Override
    public void onClick(View v) {
        if(v.getId() == mBtCalibrate.getId()){
            try{
                Intent intent = new Intent("android.intent.action.GSENSOR_CALIBRATE");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.putExtra("fromWhere", "DeviceTestApp");
                startActivity(intent);
            }catch(ActivityNotFoundException e){
            }catch(SecurityException e){
            }
            return;
        }
        if(v.getId()== mSmtTest.getId()){
            boolean flag = IsCheckDataCorrect();
            if(mCheckDataSuccess)
                return;
            if(flag){
                mCheckDataSuccess = true;
                mSmtTest.setTextColor(Color.GREEN);
                mSmtTest.setText("Pass");
            }else{
                mCheckDataSuccess = true;
                mSmtTest.setTextColor(Color.RED);
                mSmtTest.setText("Fail");
            }
            return;
        }
        Utils.SetPreferences(this, mSp, R.string.gsensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }
}
