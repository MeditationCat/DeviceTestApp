package com.zhi_tech.taipp.devicetestapp.sensor;

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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhi_tech.taipp.devicetestapp.AppDefine;
import com.zhi_tech.taipp.devicetestapp.DeviceTestAppService;
import com.zhi_tech.taipp.devicetestapp.OnDataChangedListener;
import com.zhi_tech.taipp.devicetestapp.R;
import com.zhi_tech.taipp.devicetestapp.SensorPackageObject;
import com.zhi_tech.taipp.devicetestapp.Utils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by taipp on 5/20/2016.
 */
public class GSensor extends Activity implements View.OnClickListener {

    private Button mBtCalibrate;
    private TextView tvdata;
    private ImageView ivimg;
    private Button mBtOk;
    private Button mBtFailed;
    //private Button mSmtTest;

    private float mX;
    private float mY;
    private float mZ;

    SharedPreferences mSp;
    boolean mCheckDataSuccess;
    private final static int OFFSET = 2;
    private final static int Accl_Sensitivity = 16384; // LSB/g
    private final static float Gravity = (float) 9.8; // m/s²
    private final static int FullScale_Range = 3; // g
    private byte okFlag = 0x00;
    private Timer mTimer;
    private TimerTask mTimerTask;

    private final String TAG = "GSensor";

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
                float[] values = new float[3];
                values[0] = object.accelerometerSensor.getX() / 1.0f;
                values[1] = object.accelerometerSensor.getY() / 1.0f;
                values[2] = object.accelerometerSensor.getZ() / 1.0f;

                float x = values[0] * Gravity / Accl_Sensitivity;
                float y = values[1] * Gravity  / Accl_Sensitivity;
                float z = values[2] * Gravity  / Accl_Sensitivity;

                mX = x;
                mY = y;
                mZ = z;
                tvdata.setText(String.format("X: %+f%nY: %+f%nZ: %+f%n",mX,mY,mZ));

                if (Math.abs(mX) < FullScale_Range && Math.abs(mY) < FullScale_Range
                        && Math.abs(Math.abs(mZ) - Gravity) < FullScale_Range) {
                    okFlag |= 0x01;
                }
                if (Math.abs(mX) < FullScale_Range && Math.abs(mZ) < FullScale_Range
                        && Math.abs(Math.abs(mY) - Gravity) < FullScale_Range) {
                    okFlag |= 0x02;
                }
                if (Math.abs(mZ) < FullScale_Range && Math.abs(mY) < FullScale_Range
                        && Math.abs(Math.abs(mX) - Gravity) < FullScale_Range) {
                    okFlag |= 0x04;
                }

                if (okFlag == 0x07) {
                    tvdata.setTextColor(Color.GREEN);
                    mBtFailed.setBackgroundColor(Color.GRAY);
                    mBtFailed.setClickable(false);
                    mBtOk.setBackgroundColor(Color.GREEN);
                    mCheckDataSuccess = true;
                    //
                }
                /*
                if (Math.abs(mX) > Gravity + FullScale_Range
                        || Math.abs(mY) > Gravity + FullScale_Range || Math.abs(mZ) > Gravity + FullScale_Range) {
                    //Log.d(TAG,String.format("X: %+f Y: %+f Z: %+f ",values[0],values[1],values[2]));
                    Log.d(TAG,String.format("X: %+f Y: %+f Z: %+f%n",mX,mY,mZ));
                    tvdata.setTextColor(Color.RED);
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setClickable(false);
                    mBtOk.setBackgroundColor(Color.GRAY);
                    mCheckDataSuccess = false;
                    okFlag |= 0x80;
                }
                */

                if (((okFlag & 0x01) != 0 && (okFlag & 0x02) != 0 && (okFlag & 0x04) != 0)
                        || (okFlag & 0x80) != 0) {
                    if (mTimer == null) {
                        mTimer = new Timer();
                        mTimer.schedule(mTimerTask, 3 * 1000);
                    }
                }

                if (Math.abs(x) > Math.abs(y) && Math.abs(x) - OFFSET > Math.abs(z)) {
                    ivimg.setBackgroundResource(x > 0? R.drawable.gsensor_x : R.drawable.gsensor_x_2);
                } else if (Math.abs(y) - OFFSET > Math.abs(x) && Math.abs(y) - OFFSET > Math.abs(z)) {
                    ivimg.setBackgroundResource(y > 0? R.drawable.gsensor_y : R.drawable.gsensor_2y);
                } else if (Math.abs(z) > Math.abs(x) && Math.abs(z) > Math.abs(y)) {
                    ivimg.setBackgroundResource(R.drawable.gsensor_z);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.gsensor);

        Intent intent = new Intent(GSensor.this,DeviceTestAppService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        mBtCalibrate = (Button) findViewById(R.id.gsensor_calibrate);
        mBtCalibrate.setOnClickListener(this);
        tvdata = (TextView) findViewById(R.id.gsensor_tv_data);
        ivimg = (ImageView) findViewById(R.id.gsensor_iv_img);
        mBtOk = (Button) findViewById(R.id.gsensor_bt_ok);
        //mSmtTest = (Button)findViewById(R.id.smt_test);
        //mSmtTest.setOnClickListener(this);
        //mSmtTest.setVisibility(View.INVISIBLE);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.gsensor_bt_failed);
        mBtFailed.setOnClickListener(this);
        mTimer = null;
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                SaveToReport();
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        unbindService(conn);
        mCheckDataSuccess = false;
    }

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
        /*
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
        */
        Utils.SetPreferences(this, mSp, R.string.gsensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

    public void SaveToReport() {
        Utils.SetPreferences(this, mSp, R.string.gsensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }
}
