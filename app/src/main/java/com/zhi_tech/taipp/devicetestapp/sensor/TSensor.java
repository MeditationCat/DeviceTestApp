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
import com.zhi_tech.taipp.devicetestapp.DeviceTestApp;
import com.zhi_tech.taipp.devicetestapp.DeviceTestAppService;
import com.zhi_tech.taipp.devicetestapp.OnDataChangedListener;
import com.zhi_tech.taipp.devicetestapp.R;
import com.zhi_tech.taipp.devicetestapp.SensorPackageObject;
import com.zhi_tech.taipp.devicetestapp.Utils;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Tiger on 6/5/2016.
 */
public class TSensor extends Activity implements View.OnClickListener {
    private TextView tvdata;
    private Button mBtOk;
    private Button mBtFailed;
    SharedPreferences mSp;
    boolean mCheckDataSuccess;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private byte okFlag = 0x00;

    private final String TAG = "TSensor";
    public static final float Temp_Sensitivity = (float) 326.8; //LSB/ºC
    public static final int RoomTemp_Offset = 25; //ºC
    public static int Temperature_Range_Min = -40; //ºC
    public static int Temperature_Range_Max = 85; //ºC
    /*
    TEMP_degC = (TEMP_OUT[15:0]/Temp_Sensitivity)
            + RoomTemp_Offset
    where Temp_Sensitivity = 326.8
    LSB/ºC and RoomTemp_Offset = 25ºC
    */
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
                float TEMP_degC = (object.temperatureSensor.getTemperature() / Temp_Sensitivity) + RoomTemp_Offset;
                tvdata.setText(String.format(Locale.US, "%.02f",TEMP_degC));
                //Log.d(TAG, String.format("TEMP_degC = %.02f",TEMP_degC));
                if (TEMP_degC < Temperature_Range_Min || TEMP_degC > Temperature_Range_Max) {
                    tvdata.setTextColor(Color.RED);
                    mBtFailed.setBackgroundColor(Color.RED);
                    mBtOk.setClickable(false);
                    mBtOk.setBackgroundColor(Color.GRAY);
                    okFlag |= 0x80;
                } else {
                    okFlag |= 0x01;
                }

                if (mTimer == null) {
                    mTimer = new Timer();
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if ((okFlag & 0x80) != 0) {
                                mCheckDataSuccess = false;
                            } else {
                                mCheckDataSuccess = true;
                            }
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mCheckDataSuccess) {
                                        tvdata.setTextColor(Color.GREEN);
                                        mBtFailed.setBackgroundColor(Color.GRAY);
                                        mBtFailed.setClickable(false);
                                        mBtOk.setBackgroundColor(Color.GREEN);
                                    } else {
                                        tvdata.setTextColor(Color.RED);
                                        mBtFailed.setBackgroundColor(Color.RED);
                                        mBtOk.setClickable(false);
                                        mBtOk.setBackgroundColor(Color.GRAY);
                                    }
                                }
                            });

                            Timer timer = new Timer();
                            timer.schedule(mTimerTask, DeviceTestApp.ShowItemTestResultTimeout * 1000);
                        }
                    };
                    mTimer.schedule(timerTask, 5 * 1000);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.tsensor);

        Intent intent = new Intent(TSensor.this,DeviceTestAppService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        tvdata = (TextView) findViewById(R.id.textView_tsensor_data);
        mBtOk = (Button) findViewById(R.id.tsensor_bt_ok);
        mBtOk.setOnClickListener(this);
        mBtFailed = (Button) findViewById(R.id.tsensor_bt_failed);
        mBtFailed.setOnClickListener(this);
        mBtOk.setClickable(false);
        mBtFailed.setClickable(false);

        Temperature_Range_Min = DeviceTestApp.Temperature_Range_Min;
        Temperature_Range_Max = DeviceTestApp.Temperature_Range_Max;

        mCheckDataSuccess = false;
        mTimer = null;
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
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        unbindService(conn);
    }

    @Override
    public void onClick(View v) {

        Utils.SetPreferences(this, mSp, R.string.tsensor_name,
                (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }

    public void SaveToReport() {
        Utils.SetPreferences(this, mSp, R.string.tsensor_name,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
        finish();
    }
}
