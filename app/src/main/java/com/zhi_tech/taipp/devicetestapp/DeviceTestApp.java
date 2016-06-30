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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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

import java.util.ArrayList;
import java.util.List;

public class DeviceTestApp extends Activity implements OnItemClickListener {
    public enum State {
        FACTORY_MODE,
        AUTO_TEST_MODE,
    }
    public static State TEST_MODE = State.FACTORY_MODE;
    //public static State TEST_MODE = State.AUTO_TEST_MODE;

    private SharedPreferences mSp = null;
    private GridView mGrid;
    private MyAdapter mAdapter;
    public static ArrayList<Integer> itemIds;
    private ArrayList<String> mListData;
    private Button mBtAuto;
    private Button mBtStart;
    private Button mBtUpgrade;
    private Button mBtCalibration;
    private Button mBtCheckVersion;
    private Button mBleCy7c63813;

    private TextView textViewDeviceInfo, textViewPacket, textViewVersion;

    public static byte result[] = new byte[AppDefine.DVT_NV_ARRAR_LEN]; //0 default; 1,success; 2,fail; 3,notest
    private boolean mCheckDataSuccess;
    private byte okFlag = 0x00;
    private boolean mBleCy7c63813IsConnected = false;

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
                                    int calibrationTip = 0;
                                    if (buffer[2] == 0x0A) {
                                        mBtCalibration.setTextColor(Color.BLACK);
                                    } else {
                                        if (buffer[2] == 0) {
                                            calibrationTip = R.string.calibration_complete;
                                            mBtCalibration.setTextColor(Color.GREEN);
                                            //mBtCalibration.setClickable(false);
                                        } else {
                                            calibrationTip = R.string.calibration_failed;
                                            mBtCalibration.setTextColor(Color.RED);
                                        }
                                        Toast toast=Toast.makeText(getApplicationContext(), getString(calibrationTip), Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();
                                    }
                                    break;

                                case 0xB2: //check version result send 0xB1 feedback
                                    if (buffer[1] == 0x00) {
                                        textViewVersion.setText(String.format("%s: %s",
                                                getString(R.string.device_version), getString(R.string.device_unknown)));
                                        mCheckDataSuccess = false;
                                    } else if (buffer[2] == 0 ||buffer[2] == 1 || buffer[3] == 0) {
                                        if (buffer[2] == 1) {
                                            textViewVersion.setText(String.format("%s: %#04x/%#04x",
                                                    getString(R.string.device_version), 0x00, buffer[3]));
                                        } else {
                                            textViewVersion.setText(String.format("%s: %#04x/%#04x",
                                                    getString(R.string.device_version), buffer[2], buffer[3]));
                                        }
                                        mCheckDataSuccess = false;
                                        textViewVersion.setTextColor(Color.RED);
                                    } else {
                                        textViewVersion.setText(String.format("%s: %#04x/%#04x",
                                                getString(R.string.device_version), buffer[2], buffer[3]));
                                        mCheckDataSuccess = true;
                                        textViewVersion.setTextColor(Color.GREEN);
                                    }
                                    //CheckVersionSaveToReport();
                                    //mGrid.setAdapter(mAdapter);
                                    break;

                                case 0xA5:
                                    int upgradeTip = 0;
                                    if (buffer[2] == 1) {
                                        upgradeTip = R.string.upgrade_failed;
                                    } else {
                                        upgradeTip = R.string.upgrade_complete;
                                    }
                                    Toast toast=Toast.makeText(getApplicationContext(), getString(upgradeTip), Toast.LENGTH_LONG);
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
                textViewPacket.setText(String.format("%s: %s%n%s: %s",
                        getString(R.string.packetdata_header), String.valueOf(object.getHeader()),
                        getString(R.string.packetdata_timestamp),String.valueOf(object.getTimestamp())));
                }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

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
        mBleCy7c63813 = (Button) findViewById(R.id.main_bt_ble_cy7c63813);

        textViewDeviceInfo = (TextView) findViewById(R.id.textViewDeviceInfo);
        textViewPacket = (TextView) findViewById(R.id.textViewPacket);
        textViewVersion = (TextView) findViewById(R.id.textViewVersion);

        if (TEST_MODE == State.FACTORY_MODE) {
            //mBtAuto.setVisibility(View.GONE);
            mBtUpgrade.setVisibility(View.GONE);
            //mBtCheckVersion.setVisibility(View.GONE);
        } else if (TEST_MODE == State.AUTO_TEST_MODE) {
            mBtCalibration.setVisibility(View.GONE);
            mBtUpgrade.setVisibility(View.GONE);
            //mBtAuto.setVisibility(View.GONE);
        }
        // init grid view data
        initTestItems();
        setDefaultValues();

        mGrid = (GridView) findViewById(R.id.main_grid);
        mAdapter = new MyAdapter(this, mListData);
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(this);

        mCheckDataSuccess = false;

        Intent intent = new Intent(DeviceTestApp.this,DeviceTestAppService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        //post a runnable to handler
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dtaService != null)
                    dtaService.connectToDevice();
            }
        }, 3 * 1000);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        setDefaultValues();

        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onResume();

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

    private void initTestItems() {
        //add test items
        itemIds = new ArrayList<Integer>();
        itemIds.clear();
        if (TEST_MODE == State.AUTO_TEST_MODE) {
            itemIds.add(R.string.lsensor_name);
            itemIds.add(R.string.psensor_name);
            itemIds.add(R.string.KeyCode_name);
        }

        itemIds.add(R.string.gsensor_name);
        itemIds.add(R.string.msensor_name);
        itemIds.add(R.string.gyroscopesensor_name);
        itemIds.add(R.string.tsensor_name);
        //set default value
        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSp.edit();
        for (int item:itemIds) {
            editor.putString(getString(item), AppDefine.DT_DEFAULT);
        }
        editor.apply();
        //add test items string to mListData
        mListData = new ArrayList<String>();
        for (int item:itemIds) {
            mListData.add(getString(item));
        }
    }

    private void setDefaultValues() {
        textViewDeviceInfo.setText(String.format("%s: %s%n%s: %s", getString(R.string.device_manufacturer), getString(R.string.device_unknown),
                getString(R.string.device_productname), getString(R.string.device_unknown)));
        textViewPacket.setText(String.format("%s: %s%n%s: %s",
                getString(R.string.packetdata_header), getString(R.string.device_unknown),
                getString(R.string.packetdata_timestamp),getString(R.string.device_unknown)));
        textViewVersion.setText(String.format("%s: %s", getString(R.string.device_version), getString(R.string.device_unknown)));
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
        private Context context;
        private ArrayList<String> mDataList;

        public MyAdapter(Context context, ArrayList<String> mDataList) {
            this.context = context;
            this.mDataList = mDataList;
        }

        @Override
        public int getCount() {
            return (mDataList == null) ? 0 : mDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return (mDataList == null) ? null : mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(this.context).inflate(R.layout.main_grid,parent, false);
            }
            TextView textView = Utils.ViewHolder.get(convertView, R.id.factor_button);
            textView.setText(mDataList.get(position));
            try {
                String name = mSp.getString(mDataList.get(position), null);
                if (name != null) {
                    if (name.equals(AppDefine.DT_SUCCESS)) {
                        textView.setTextColor(Color.BLUE);
                    } else if (name.equals(AppDefine.DT_DEFAULT)) {
                        textView.setTextColor(Color.BLACK);
                    } else if (name.equals(AppDefine.DT_FAILED)) {
                        textView.setTextColor(Color.RED);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG,"SetColor ExException");
            }

            return convertView;
        }
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
        //unregisterReceiver(mUsbReceiver);
        //android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }

    public void CheckVersionSaveToReport() {
        Utils.SetPreferences(this, mSp, R.string.CheckVersion,
                mCheckDataSuccess ? AppDefine.DT_SUCCESS : AppDefine.DT_FAILED);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + TAG + "->" + action);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                SharedPreferences.Editor editor = mSp.edit();
                for (int item:itemIds) {
                    editor.putString(getString(item), AppDefine.DT_DEFAULT);
                }
                editor.apply();
                //
                for (int i = 0; i < itemIds.size(); i++) {
                    DeviceTestApp.result[i] = AppDefine.DVT_DEFAULT;
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mGrid.setAdapter(mAdapter);
                        textViewVersion.setTextColor(Color.BLACK);
                        mBleCy7c63813.setTextColor(Color.BLACK);


                    }
                });
            }
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent keyCode->" + event.getKeyCode() + "-->" + event.getSource());
        MyAdapter myAdapter = (MyAdapter) mGrid.getAdapter();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                //joystick
                case KeyEvent.KEYCODE_BUTTON_L2:
                case KeyEvent.KEYCODE_BUTTON_L1:
                case KeyEvent.KEYCODE_BUTTON_Y:
                    okFlag |= 0x80;
                    checkDataSuccess();
                    break;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if ((ev.getDevice().getSources() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            Log.d(TAG, String.format("dispatchGenericMotionEvent ev->(%f, %f)", ev.getX(), ev.getY()));

            if (ev.getX() > 0.5) {
                okFlag |= 0x01;
            }
            if (ev.getX() < -0.5) {
                okFlag |= 0x02;
            }
            if (ev.getY() > 0.5) {
                okFlag |= 0x04;
            }
            if (ev.getY() < -0.5) {
                okFlag |= 0x08;
            }
            checkDataSuccess();
        }
        return true; //super.dispatchGenericMotionEvent(ev);
    }

    public void checkDataSuccess() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("->okFlag = %#x", okFlag));

        if (DeviceTestApp.TEST_MODE == DeviceTestApp.State.FACTORY_MODE) {
            if (okFlag != 0 && !mBleCy7c63813IsConnected) {
                mBleCy7c63813IsConnected = true;
                mBleCy7c63813.setTextColor(Color.GREEN);
                Toast toast=Toast.makeText(getApplicationContext(), getString(R.string.ble_test_tip), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }

}

