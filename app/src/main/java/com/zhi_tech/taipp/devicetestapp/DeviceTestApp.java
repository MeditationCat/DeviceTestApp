package com.zhi_tech.taipp.devicetestapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
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
            R.string.bluetooth_name
    };
    public static ArrayList<Integer> excludeIds = new ArrayList<Integer>();
    private Button mBtAll;
    private Button mBtAuto;
    //liuyang add begin@20121219
    private Button mAllResult;
    public static byte result[] = new byte[AppDefine.DVT_NV_ARRAR_LEN]; //[2048];  //0 default; 1,success; 2,fail; 3,notest
    public static final int AP_CFG_CUSTOM_FILE_CUSTOM_LID = 44;
    //liuyang add end

    private final String TAG = "DeviceTestApp";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        init();
        mBtAuto = (Button) findViewById(R.id.main_bt_autotest);

        mBtAuto.setOnClickListener(cl);
       // mBtAuto.setVisibility(View.GONE);//modify by yuwei@20151020
        mBtAll = (Button) findViewById(R.id.main_bt_alltest);
        mBtAll.setOnClickListener(cl);
        //mBtAll.setVisibility(View.GONE);//modify by yuwei@20151020
        mGrid = (GridView) findViewById(R.id.main_grid);
        mListData = getData();
        mAdapter = new MyAdapter(this);

    }

    @Override
    protected void onResume() {
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
            }

            if (v.getId() == mBtAuto.getId()) {
                intent.setClassName("com.zhi_tech.taipp.devicetestapp", "com.zhi_tech.taipp.devicetestapp.AllTest");
                reqId = AppDefine.DT_ALLTESTID;
            }

            startActivityForResult(intent, reqId);
        }
    };

    public class MyAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public MyAdapter(DeviceTestApp deviceTestApp, int deviceButton) {
        }

        public int getCount() {
            if (mListData == null) {
                return 0;
            }
            return mListData.size();
        }

        public Object getItem(int position) {
            return mListData.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.main_grid, null);
            TextView textview = (TextView) convertView.findViewById(R.id.factor_button);
            textview.setText(mListData.get(position));
            SetColor(textview);
            return convertView;
        }
    }

    private void init() {
        //first exclude some features
        excludeItems();

        if (isShowBackupAllreport()) {
            for (int i = 0; i < result.length; i++){
                result[i] = 0x00;
            }
            ReadData();
        }
        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSp.edit();
        for (int i = 0; i < itemString.length; i++) {
        	/*
        	if(!isGpsEnable() && (itemString[i] == R.string.gps_name)){
        		editor.putString(getString(itemString[i]), null);
        	}else if(!isMsensorEnable() && (itemString[i] == R.string.msensor_name)){
        		editor.putString(getString(itemString[i]), null);
        	}else{
        		editor.putString(getString(itemString[i]), AppDefine.FT_DEFAULT);
        	}
        	*/
            boolean flag = true;
            for (int j = 0; j < excludeIds.size(); j++) {
                if(itemString[i] == excludeIds.get(j).intValue()){
                    editor.putString(getString(itemString[i]), null);
                    flag = false;
                    break;
                }
            }
            if(flag)
                editor.putString(getString(itemString[i]), AppDefine.DT_DEFAULT);

        }

        editor.commit();
    }

    private void SetColor(TextView s) {
        //mSp = getSharedPreferences("FactoryMode", Context.MODE_PRIVATE);
        if (isShowBackupAllreport()) {
            for (int i = 0; i < itemString.length; i++) {
                if (getResources().getString(itemString[i]).equals(s.getText().toString())) {
                    if (result[i] == AppDefine.DVT_OK) {
                        s.setTextColor(getApplicationContext().getResources().getColor(R.color.blue));
                    } else if (result[i] == AppDefine.DVT_FAIL) {
                        s.setTextColor(getApplicationContext().getResources().getColor(R.color.red));
                    } else { //if (name.equals(AppDefine.FT_FAILED)) {
                        s.setTextColor(getApplicationContext().getResources().getColor(R.color.black));
                    }
                }
            }
        } else {
            for (int i = 0; i < itemString.length; i++) {
                if (getResources().getString(itemString[i]).equals(s.getText().toString())) {
                    String name = mSp.getString(getString(itemString[i]), null);
                    if (name.equals(AppDefine.DT_SUCCESS)) {
                        s.setTextColor(getApplicationContext().getResources().getColor(R.color.blue));
                    } else if (name.equals(AppDefine.DT_DEFAULT)) {
                        s.setTextColor(getApplicationContext().getResources().getColor(R.color.black));
                    } else if (name.equals(AppDefine.DT_FAILED)) {
                        s.setTextColor(getApplicationContext().getResources().getColor(R.color.red));
                    }
                }
            }
        }
    }

    private List<String> getData() {
        List<String> items = new ArrayList<String>();
        SharedPreferences pre = PreferenceManager.getDefaultSharedPreferences(this);
        for (int i = 0; i < itemString.length; i++) {
            if (mSp.getString(getString(itemString[i]), null) != null) {
                items.add(getString(itemString[i]));
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
            if (getString(R.string.bluetooth_name) == name) {
                classname = "com.mediatek.factorymode.bluetooth.Bluetooth";
            } else if (getString(R.string.gsensor_name) == name) {
                classname = "com.mediatek.factorymode.sensor.GSensor";
            } else if (getString(R.string.msensor_name) == name) {
                classname = "com.mediatek.factorymode.sensor.MSensor";
            } else if (getString(R.string.lsensor_name) == name) {
                classname = "com.mediatek.factorymode.sensor.LSensor";
            } else if (getString(R.string.psensor_name) == name) {
                classname = "com.mediatek.factorymode.sensor.PSensor";
            } else if (getString(R.string.KeyCode_name) == name) {
                classname = "com.mediatek.factorymode.KeyCode";
            }else if(getString(R.string.gyroscopesensor_name) == name){
                classname = "com.mediatek.factorymode.sensor.GyRoscopeSensor";
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
        boolean result = this.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_SENSOR_COMPASS);
        if(result)
        {
            result = getResources().getBoolean(R.bool.config_Msensor_available);
        }
        return result;
    }
    private boolean isGsensorEnable(){
        boolean result = this.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        return result;
    }
    private boolean isLightsensorEnable(){
        boolean result = this.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_SENSOR_LIGHT);
        if(result)
        {
            result = getResources().getBoolean(R.bool.config_LSensor_available);
        }
        return result;
    }
    private boolean isProximityEnable(){
        boolean result = this.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_SENSOR_PROXIMITY);
        if(result)
        {
            result = getResources().getBoolean(R.bool.config_PSensor_available);
        }
        return result;
    }
    private boolean isGyroscopesensorEnable() {
        boolean result = getResources().getBoolean(R.bool.config_gyroscopesensor_available);
        return result;
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

    private  boolean isShowBackupAllreport() {
        boolean result = getResources().getBoolean(R.bool.config_backup_show_allreport);
        return result;
    }
    //Read NvRAM
    public void ReadData() {
        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);

        //Log.v(TAG,"ReadData");
        byte buff[] = new byte[AppDefine.DVT_NV_ARRAR_LEN];
        try {
            buff = agent.readFile(AP_CFG_CUSTOM_FILE_CUSTOM_LID);// read buffer from nvram
            //Log.v(TAG,"read success");
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            //Log.v(TAG,"read failed");
            e.printStackTrace();
        }

        for(int i = 0; i < buff.length; i ++){
            Log.v(TAG,"read_buff["+i+"]="+buff[i]);
        }
        System.arraycopy(buff, 0, result, 0, buff.length);
    }

    @Override
    protected void onStop() {
        if (getResources().getBoolean(R.bool.config_notest_warning)){
            Utils.WriteIsAllTestOK();
        }
        if (isShowBackupAllreport()) {
            Utils.NvRAMBackupData();
        }
        super.onStop();
    }

    @Override
    public void finish() {
        Log.v(TAG,"dt-finish");
        if (getResources().getBoolean(R.bool.config_notest_warning)){
            Utils.WriteIsAllTestOK();
        }
        if (isShowBackupAllreport()) {
            Utils.NvRAMBackupData();
        }
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}

