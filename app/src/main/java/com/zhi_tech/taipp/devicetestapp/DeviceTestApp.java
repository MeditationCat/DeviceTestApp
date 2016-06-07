package com.zhi_tech.taipp.devicetestapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DeviceTestApp extends Activity implements OnItemClickListener {

    private List<String> mListData;
    private SharedPreferences mSp = null;
    private GridView mGrid;
    private MyAdapter mAdapter;
    final static int itemString[] = {
            R.string.KeyCode_name,
            R.string.gsensor_name,
            R.string.msensor_name,
            R.string.lsensor_name,
            R.string.psensor_name,
            R.string.gyroscopesensor_name,
            R.string.tsensor_name,
            //R.string.bluetooth_name,
    };
    public static ArrayList<Integer> excludeIds = new ArrayList<Integer>();
    private Button mBtAuto;
    private Button mBtStart;
    private TextView textView;

    public static byte result[] = new byte[AppDefine.DVT_NV_ARRAR_LEN]; //0 default; 1,success; 2,fail; 3,notest

    private final String TAG = "DeviceTestApp";

    //service connection
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
                textView.setText(String.valueOf(object.getHeader()) + ":" + object.getTimestamp());
                }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(DeviceTestApp.this,DeviceTestAppService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        init();
        mBtAuto = (Button) findViewById(R.id.main_bt_autotest);
        mBtAuto.setOnClickListener(cl);
        mBtStart = (Button) findViewById(R.id.main_bt_start);
        mBtStart.setOnClickListener(cl);
        textView = (TextView) findViewById(R.id.textView);
        //textView.setVisibility(View.GONE);
        mGrid = (GridView) findViewById(R.id.main_grid);
        mListData = getData();
        mAdapter = new MyAdapter(this);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onResume();
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(this);
    }

    public View.OnClickListener cl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            int reqId = -1;
            if (v.getId() == mBtAuto.getId()) {
                intent.setClassName("com.zhi_tech.taipp.devicetestapp", "com.zhi_tech.taipp.devicetestapp.AutoTest");
                reqId = AppDefine.DT_AUTOTESTID;
                startActivityForResult(intent, reqId);
            } else if (v.getId() == mBtStart.getId()) {
                if (dtaService != null) {
                    dtaService.startToConnectDevice();
                }
            }
        }
    };

    public class MyAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public MyAdapter(DeviceTestApp deviceTestApp, int deviceButton) {
        }

        @Override
        public int getCount() {
            if (mListData == null) {
                return 0;
            }
            return mListData.size();
        }

        @Override
        public Object getItem(int position) {
            return mListData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.main_grid, null);
            }
            TextView textview = (TextView) convertView.findViewById(R.id.factor_button);
            textview.setText(mListData.get(position));
            SetColor(textview);
            return convertView;
        }
    }

    private void init() {
        //first exclude some features
        excludeItems();

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSp.edit();
        for (int item:itemString) {
            editor.putString(getString(item), AppDefine.DT_DEFAULT);
        }
        editor.apply();
    }

    private void SetColor(TextView s) {
        try {
            for (int item:itemString) {
                if (getResources().getString(item).equals(s.getText().toString())) {
                    String name = mSp.getString(getString(item), null);
                    if (name.equals(AppDefine.DT_SUCCESS)) {
                        s.setTextColor(getApplicationContext().getResources().getColor(R.color.blue));
                    } else if (name.equals(AppDefine.DT_DEFAULT)) {
                        s.setTextColor(getApplicationContext().getResources().getColor(R.color.black));
                    } else if (name.equals(AppDefine.DT_FAILED)) {
                        s.setTextColor(getApplicationContext().getResources().getColor(R.color.red));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"SetColor ExException");
        }

    }

    private List<String> getData() {
        List<String> items = new ArrayList<String>();
        for (int item:itemString) {
            if (mSp.getString(getString(item), null) != null) {
                items.add(getString(item));
            }
        }
        return items;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            String name = mListData.get(position);
            String classname = null;
            if (name.equals(getString(R.string.bluetooth_name))) {
                classname = "com.zhi_tech.taipp.devicetestapp.bluetooth.Bluetooth";
            } else if (name.equals(getString(R.string.gsensor_name))) {
                classname = "com.zhi_tech.taipp.devicetestapp.sensor.GSensor";
            } else if (name.equals(getString(R.string.msensor_name))) {
                classname = "com.zhi_tech.taipp.devicetestapp.sensor.MSensor";
            } else if (name.equals(getString(R.string.lsensor_name))) {
                classname = "com.zhi_tech.taipp.devicetestapp.sensor.LSensor";
            } else if (name.equals(getString(R.string.psensor_name))) {
                classname = "com.zhi_tech.taipp.devicetestapp.sensor.PSensor";
            } else if (name.equals(getString(R.string.KeyCode_name))) {
                classname = "com.zhi_tech.taipp.devicetestapp.KeyCode";
            }else if(name.equals(getString(R.string.gyroscopesensor_name))) {
                classname = "com.zhi_tech.taipp.devicetestapp.sensor.GyRoscopeSensor";
            }else if(name.equals(getString(R.string.tsensor_name))) {
                classname = "com.zhi_tech.taipp.devicetestapp.sensor.TSensor";
            }
            intent.setClassName(this, classname);
            this.startActivity(intent);
        } catch (Exception e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.PackageIerror);
            builder.setMessage(R.string.Packageerror);
            builder.setPositiveButton("OK", null);
            builder.create().show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.gc();
        Intent intent = new Intent(DeviceTestApp.this, Report.class);
        startActivity(intent);

    }

    private boolean isMsensorEnable(){
        return getResources().getBoolean(R.bool.config_Msensor_available);
    }
    private boolean isGsensorEnable(){
        return getResources().getBoolean(R.bool.config_Gsensor_available);
    }
    private boolean isLightsensorEnable(){
        return getResources().getBoolean(R.bool.config_LSensor_available);
    }
    private boolean isProximityEnable(){
        return getResources().getBoolean(R.bool.config_PSensor_available);
    }
    private boolean isGyroscopesensorEnable() {
        return getResources().getBoolean(R.bool.config_gyroscopesensor_available);
    }

    private void excludeItems(){
        excludeIds.clear();
        if(!isMsensorEnable()){
            excludeIds.add(new Integer(R.string.msensor_name));
        }
        if(!isGsensorEnable()){
            excludeIds.add(new Integer(R.string.gsensor_name));
        }
        if(!isProximityEnable()){
            excludeIds.add(new Integer(R.string.psensor_name));
        }
        if(!isLightsensorEnable()){
            excludeIds.add(new Integer(R.string.lsensor_name));
        }
        if (!isGyroscopesensorEnable()) {
            excludeIds.add(new Integer(R.string.gyroscopesensor_name));
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onStop();
    }

    @Override
    public void finish() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.finish();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        unbindService(conn);
        android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " keyCode: " + keyCode);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " keyCode: " + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + " keyCode: " + keyCode);
        return super.onKeyLongPress(keyCode, event);
    }
}

