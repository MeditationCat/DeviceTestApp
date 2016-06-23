package com.zhi_tech.taipp.devicetestapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DeviceTestApp extends Activity implements OnItemClickListener {
    public enum State {
        FACTORY_MODE,
        AUTO_TEST_MODE,
    }
    //public State TEST_MODE = State.FACTORY_MODE;
    public State TEST_MODE = State.AUTO_TEST_MODE;

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
    };
    public static ArrayList<Integer> itemIds = new ArrayList<Integer>();
    private Button mBtAuto;
    private Button mBtStart;
    private Button mBtUpgrade;
    private Button mBtCalibration;
    private Button mBtCheckVersion;

    private TextView textViewDeviceInfo, textViewPacket, textViewVersion;

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
            dtaService.setOnDeviceStatusListener(new OnDeviceStatusListener() {
                @Override
                public void deviceStatusChanged(final Object object) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            textViewDeviceInfo.setText((String) object);
                        }
                    });
                }
            });
            dtaService.setOnCommandResultListener(new OnCommandResultListener() {
                @Override
                public void commandResultChanged(final int cmd, final byte[] buffer) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            switch (cmd) {
                                case 0x2C: //G sensor calibration result send 0x2B feedback
                                    if (buffer[2] == 0x0A) {
                                        mBtCalibration.setTextColor(Color.BLACK);
                                    } else {
                                        String str = "Calibration is OK!";
                                        if (buffer[2] == 0) {
                                            str = "Calibration failed!";
                                        } else {
                                            mBtCalibration.setTextColor(Color.GREEN);
                                            //mBtCalibration.setClickable(false);
                                        }
                                        Toast toast=Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();
                                    }
                                    break;

                                case 0xB2: //check version result send 0xB1 feedback
                                    textViewVersion.setText(String.format("Version: %#04x / %#04x", buffer[2], buffer[3]));
                                    break;

                                case 0xA5:
                                    String str = "DFU upgrade is OK!";
                                    if (buffer[2] == 1) {
                                        str = "DFU upgrade failed!";
                                    }
                                    Toast toast=Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                    break;

                                default:
                                    break;
                            }
                        }
                    });
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
                textViewPacket.setText(String.format("PacketHeader: %s%nTimestamp: %s",
                        String.valueOf(object.getHeader()), String.valueOf(object.getTimestamp())));
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
        mBtUpgrade = (Button) findViewById(R.id.main_bt_upgrade);
        mBtUpgrade.setOnClickListener(cl);
        mBtCalibration = (Button) findViewById(R.id.main_bt_calibration);
        mBtCalibration.setOnClickListener(cl);
        mBtCheckVersion = (Button) findViewById(R.id.main_bt_checkversion);
        mBtCheckVersion.setOnClickListener(cl);

        textViewDeviceInfo = (TextView) findViewById(R.id.textViewDeviceInfo);
        textViewDeviceInfo.setText(String.format("Manufacturer: %s%nProductName: %s", "Unknown", "Unknown"));
        textViewPacket = (TextView) findViewById(R.id.textViewPacket);
        textViewPacket.setText(String.format("PacketHeader: %s%nTimestamp: %s", "xx", String.valueOf(0)));
        textViewVersion = (TextView) findViewById(R.id.textViewVersion);
        textViewVersion.setText(String.format("Version: %#04x / %#04x", 0x00, 0x00));
        if (TEST_MODE == State.FACTORY_MODE) {
            mBtAuto.setVisibility(View.GONE);
            mBtUpgrade.setVisibility(View.GONE);
        } else if (TEST_MODE == State.AUTO_TEST_MODE) {
            mBtCalibration.setVisibility(View.GONE);
            mBtUpgrade.setVisibility(View.GONE);
        }
        mGrid = (GridView) findViewById(R.id.main_grid);
        mListData = getData();
        mAdapter = new MyAdapter(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onResume();
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(this);

        if (dtaService != null) {
            dtaService.setOnDataChangedListener(new OnDataChangedListener() {
                @Override
                public void sensorDataChanged(SensorPackageObject object) {
                    //to get the data from the object.
                    postUpdateHandlerMsg(object);
                }
            });
        }
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
                    dtaService.StartSensorSwitch();
                }
            } else if (v.getId() == mBtUpgrade.getId()) {
                //start upgrade request
                if (dtaService != null) {
                    dtaService.StartToUpgrade();
                }
            } else if (v.getId() == mBtCalibration.getId()) {
                //start calibration request
                if (dtaService != null) {
                    dtaService.StartToCalibration();
                }
            } else if (v.getId() == mBtCheckVersion.getId()) {
                //check version request
                if (dtaService != null) {
                    dtaService.StartToCheckVersion();
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
                convertView = mInflater.inflate(R.layout.main_grid, parent, false);
            }
            TextView textView = Utils.ViewHolder.get(convertView, R.id.factor_button);
            textView.setText(mListData.get(position));
            SetColor(textView);
            return convertView;
        }
    }

    private void init() {
        itemIds.clear();
        if (TEST_MODE == State.AUTO_TEST_MODE) {
            itemIds.add(R.string.KeyCode_name);
            itemIds.add(R.string.lsensor_name);
            itemIds.add(R.string.psensor_name);
        }

        itemIds.add(R.string.gsensor_name);
        itemIds.add(R.string.msensor_name);
        itemIds.add(R.string.gyroscopesensor_name);
        itemIds.add(R.string.tsensor_name);

        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSp.edit();
        for (int item:itemIds) {
            editor.putString(getString(item), AppDefine.DT_DEFAULT);
        }
        editor.apply();
    }

    private void SetColor(TextView s) {
        try {
            for (int item:itemIds) {
                if (getResources().getString(item).equals(s.getText().toString())) {
                    String name = mSp.getString(getString(item), null);
                    if (name != null) {
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
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG,"SetColor ExException");
        }

    }

    private List<String> getData() {
        List<String> items = new ArrayList<String>();
        for (int item:itemIds) {
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
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onActivityResult(requestCode, resultCode, data);
        System.gc();
        Intent intent = new Intent(DeviceTestApp.this, Report.class);
        startActivity(intent);

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

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                SharedPreferences.Editor editor = mSp.edit();
                for (int item:itemIds) {
                    editor.putString(getString(item), AppDefine.DT_DEFAULT);
                }
                editor.apply();
                //
                for (int i = 0; i < result.length; i++) {
                    DeviceTestApp.result[i] = AppDefine.DVT_DEFAULT;
                }
                mGrid.setAdapter(mGrid.getAdapter());
            }
        }
    };
}

