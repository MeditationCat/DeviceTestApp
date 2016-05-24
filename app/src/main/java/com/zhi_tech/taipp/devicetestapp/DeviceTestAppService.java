package com.zhi_tech.taipp.devicetestapp;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class DeviceTestAppService extends Service {

    private static final String TAG = "DeviceTestAppService";
    private static byte result[] = new byte[AppDefine.DVT_NV_ARRAR_LEN];
    private static final long delaytime = 5000;

    private Handler mHandler = new Handler();

    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.d(TAG, "mHandler---mRunnable!---");
            //readDataFromNvram();
            checkDVTResult();
            mHandler.postDelayed(this, delaytime);
        }
    };

    public DeviceTestAppService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
        //Log.d(TAG, "Service---onBind!---");
        //return null;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Log.d(TAG, "Service---onCreate!---");
        if (!getResources().getBoolean(R.bool.config_notest_warning)){
            stopSelf();
            return;
        }

        mHandler.postDelayed(mRunnable, delaytime);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);
        Log.d(TAG, "Service---onStart!---");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service-----onDestroy");
        mHandler.removeCallbacks(mRunnable);
    }

    private void checkDVTResult() {
        if (result[result.length -1] != AppDefine.DVT_OK){
            Toast.makeText(getApplicationContext(), getString(R.string.complate_fmtest), Toast.LENGTH_LONG).show();
            mHandler.postDelayed(mRunnable, delaytime);
        }else{
            mHandler.removeCallbacks(mRunnable);
            stopSelf();
        }

    }
}
