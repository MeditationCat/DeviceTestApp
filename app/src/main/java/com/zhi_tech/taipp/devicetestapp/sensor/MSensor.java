package com.zhi_tech.taipp.devicetestapp.sensor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
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

public class MSensor extends Activity {

    private ImageView mImgCompass = null;
    private TextView mOrientText = null;
    private TextView mOrientValue = null;
    private RotateAnimation mMyAni = null;
    private float mDegressQuondam = 0.0f;
    private SharedPreferences mSp;
    private Button mBtOk;
    private Button mBtFailed;
    private final String TAG = "MSensor";

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
                public void sendsorCommandReturnValue(int cmd, byte[] buffer) {

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
                synchronized (this) {
                    if (null == mOrientText || null == mOrientValue || null == mImgCompass) {
                        return;
                    }
                    float Mx = object.magneticSensor.getX() / 100.0f * 100.0f;
                    float My = object.magneticSensor.getY() / 100.0f * 100.0f;
                    float Mz = object.magneticSensor.getZ() / 100.0f * 100.0f;
                    mOrientValue.setText(String.format("Magnetic Sensor Data:%nX: %+f%nY: %+f%nZ: %+f%n", Mx, My, Mz));
                    //Log.d(TAG,String.format(" Magnetic Sensor Data:X: %+f Y: %+f Z: %+f ", Mx, My, Mz));
                    float azimuth = (float) (Math.atan2(Mx, My) * (180 / Math.PI));
                    if (azimuth < 0) {
                        azimuth = 360 - Math.abs(azimuth);
                    }
                    /*
                    float azimuth = (float) Math.atan2(My, Mx);
                    if (azimuth < 0) {
                        azimuth = (float) (360 - Math.abs(azimuth * 180 / Math.PI));
                    } else {
                        azimuth = (float) (azimuth * 180 / Math.PI);
                    }
                    azimuth = 360 - azimuth;
                    */
                    float pitch = (float) (Math.atan2(My, Mz) * 180 / Math.PI);
                    float roll = (float) (Math.atan2(Mx, Mz) * 180 / Math.PI);
                    if (roll > 90) {
                        roll = -(180 - roll);
                    } else if (roll < -90) {
                        roll = 180 + roll;
                    }
                    float[] values = new float[3];
                    values[0] = azimuth;
                    values[1] = pitch;
                    values[2] = roll;

                    if (Math.abs(values[0] - mDegressQuondam) < 1) {
                        return;
                    }

                    switch ((int) values[0]) {
                        case 0: // North
                            mOrientText.setText(R.string.MSensor_North);
                            break;
                        case 90: // East
                            mOrientText.setText(R.string.MSensor_East);
                            break;
                        case 180: // South
                            mOrientText.setText(R.string.MSensor_South);
                            break;
                        case 270: // West
                            mOrientText.setText(R.string.MSensor_West);
                            break;
                        default: {
                            int v = (int) values[0];
                            if (v > 0 && v < 90) {
                                mOrientText.setText(getString(R.string.MSensor_north_east) + String.format(" %02d °", v));
                            }

                            if (v > 90 && v < 180) {
                                v = 180 - v;
                                mOrientText.setText(getString(R.string.MSensor_south_east) + String.format(" %02d °", v));
                            }

                            if (v > 180 && v < 270) {
                                v = v - 180;
                                mOrientText.setText(getString(R.string.MSensor_south_west) + String.format(" %02d °", v));
                            }
                            if (v > 270 && v < 360) {
                                v = 360 - v;
                                mOrientText.setText(getString(R.string.MSensor_north_west) + String.format(" %02d °", v));
                            }
                        }
                    }

                    if (mDegressQuondam != -values[0])
                        AniRotateImage(-values[0]);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setContentView(R.layout.msensor);

        Intent intent = new Intent(MSensor.this,DeviceTestAppService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        mOrientText = (TextView) findViewById(R.id.OrientText);
        mImgCompass = (ImageView) findViewById(R.id.ivCompass);
        mOrientValue = (TextView) findViewById(R.id.OrientValue);
        mOrientValue.setText(String.format("Magnetic Sensor Data:%nX: %+f%nY: %+f%nZ: %+f%n", 0.0f, 0.0f, 0.0f));
        mBtOk = (Button) findViewById(R.id.msensor_bt_ok);
        mBtOk.setOnClickListener(cl);
        mBtFailed = (Button) findViewById(R.id.msensor_bt_failed);
        mBtFailed.setOnClickListener(cl);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onStart() {
        super.onStart();
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        unbindService(conn);
    }

    private void AniRotateImage(float fDegress) {
        if (Math.abs(fDegress - mDegressQuondam) < 1) {
            return;
        }
        mMyAni = new RotateAnimation(mDegressQuondam, fDegress, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mMyAni.setDuration(200);
        mMyAni.setFillAfter(true);

        mImgCompass.startAnimation(mMyAni);
        mDegressQuondam = fDegress;
    }

    private View.OnClickListener cl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Utils.SetPreferences(getApplicationContext(), mSp, R.string.msensor_name,
                    (v.getId() == mBtOk.getId()) ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
            finish();
        }
    };

}
