package com.zhi_tech.devicetestapp;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Locale;

public class DeviceTestApp extends AppCompatActivity implements OnItemClickListener {
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
    private Button mBtCheckSN;
    private Button mBleCy7c63813;
    private Button mBtSetting;

    private TextView textViewDeviceInfo, textViewPacket, textViewVersion, textViewSN;

    public static byte result[] = new byte[AppDefine.DVT_NV_ARRAR_LEN]; //0 default; 1,success; 2,fail; 3,notest
    private boolean mCheckDataSuccess;
    private byte okFlag = 0x00;
    private boolean mBleCy7c63813IsConnected = false;

    public static int ShowItemTestResultTimeout = 1; // s

    public static boolean IsFactoryMode = false;
    public static boolean AutoTestMode = true;
    public static int ItemTestTimeout = 10;
    public static int Accel_FullScale_Range = 3; // g
    public static float Gyro_FullScale_Range = 300.0f; // º/s
    public static int Proximity_Threshold_Approach = 820; //
    public static int Light_Threshold_Approach = 100; //
    public static int Temperature_Range_Min = -40; //ºC
    public static int Temperature_Range_Max = 85; //ºC
    public static int Magnetic_Yaw_Offset = 90; // º

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
                                case 0xA5: // iap upgrade return result value
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

                                case 0x2C: //G sensor calibration result send 0x2B feedback
                                    int calibrationTip = 0;
                                    if (buffer[2] == 0) {
                                        calibrationTip = R.string.calibration_complete;
                                        mBtCalibration.setTextColor(Color.GREEN);
                                    } else {
                                        calibrationTip = R.string.calibration_failed;
                                        mBtCalibration.setTextColor(Color.RED);
                                    }
                                    toast=Toast.makeText(getApplicationContext(), getString(calibrationTip), Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();

                                    if (IsFactoryMode) {
                                        if (dtaService != null) {
                                            dtaService.StartSensorSwitch();
                                        }
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent intent = new Intent();
                                                intent.setClassName("com.zhi_tech.devicetestapp", "com.zhi_tech.devicetestapp.AutoTest");
                                                startActivityForResult(intent, AppDefine.DT_AUTOTESTID);
                                            }
                                        }, 2 * 1000);
                                    }
                                    break;

                                case 0xB2: //check version result send 0xB1 feedback
                                    if (buffer[2] == 0 || buffer[3] == 0 || buffer[4] == 0) {
                                        mCheckDataSuccess = false;
                                        textViewVersion.setTextColor(Color.RED);
                                    } else {
                                        mCheckDataSuccess = true;
                                        textViewVersion.setTextColor(Color.GREEN);
                                        mBtCheckVersion.setTextColor(Color.GREEN);
                                    }
                                    textViewVersion.setText(String.format(Locale.US,"%s: %d%n%s: %d%n%s: %d%n%s: %d",
                                            getString(R.string.device_version_ble), buffer[2],
                                            getString(R.string.device_version_63813), buffer[3],
                                            getString(R.string.device_version_stm32), buffer[4],
                                            getString(R.string.device_ble_state), buffer[5]));

                                    //CheckVersionSaveToReport();
                                    //mGrid.setAdapter(mAdapter);
                                    if (dtaService != null) {
                                        dtaService.StartToReadSerialNumber();
                                    }
                                    break;

                                case 0xB6: //write ble mac return value
                                    int bleWriteMacTip = 0;
                                    if (buffer[2] == 1) {
                                        bleWriteMacTip = R.string.ble_write_mac_failed;
                                    } else {
                                        bleWriteMacTip = R.string.ble_write_mac_complete;
                                    }
                                    toast=Toast.makeText(getApplicationContext(), getString(bleWriteMacTip), Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                    break;

                                case 0xB8: //read ble mac return value
                                    textViewVersion.append(String.format(Locale.US, "%n%s: %02x:%02x:%02x:%02x:%02x:%02x",
                                            getString(R.string.device_ble_mac_addr),
                                            buffer[2], buffer[3], buffer[4],buffer[5], buffer[6], buffer[7]));
                                    break;

                                case 0xBB: //result for serial number writing
                                    int writeSNTip = 0;
                                    if (buffer[2] == 1) {
                                        writeSNTip = R.string.device_write_sn_failed;
                                    } else {
                                        writeSNTip = R.string.device_write_sn_complete;
                                    }
                                    toast=Toast.makeText(getApplicationContext(), getString(writeSNTip), Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                    break;

                                case 0xBD: //read serial number return value
                                    String stringSn = null;
                                    try {
                                        stringSn = new String(buffer, 2, buffer[1], "US-ASCII");
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    textViewSN.setText(String.format(Locale.US, "%s:%s",
                                            getString(R.string.device_serial_number), stringSn));

                                    if ((buffer[1] & 0x0F) == 0x0F) {
                                        textViewSN.setTextColor(Color.GREEN);
                                        mBtCheckSN.setTextColor(Color.GREEN);
                                    } else {
                                        textViewSN.setTextColor(Color.RED);
                                        mBtCheckSN.setTextColor(Color.RED);
                                    }

                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (AutoTestMode && !IsFactoryMode) {
                                                Intent intent = new Intent();
                                                int reqId = -1;
                                                intent.setClassName("com.zhi_tech.devicetestapp", "com.zhi_tech.devicetestapp.AutoTest");
                                                reqId = AppDefine.DT_AUTOTESTID;
                                                startActivityForResult(intent, reqId);
                                            }
                                        }
                                    }, 2 *1000);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                }
            });

            dtaService.connectToDevice();
            dtaService.StartToCheckVersion();
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
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);

        initDefaultSetting();

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
        mBtCheckSN = (Button) findViewById(R.id.main_bt_checksn);
        mBtCheckSN.setOnClickListener(cl);
        mBleCy7c63813 = (Button) findViewById(R.id.main_bt_ble_cy7c63813);
        mBtSetting = (Button) findViewById(R.id.main_bt_setting);
        mBtSetting.setOnClickListener(cl);
        mBtSetting.setVisibility(View.GONE);

        textViewDeviceInfo = (TextView) findViewById(R.id.textViewDeviceInfo);
        textViewPacket = (TextView) findViewById(R.id.textViewPacket);
        textViewVersion = (TextView) findViewById(R.id.textViewVersion);
        textViewSN = (TextView) findViewById(R.id.textViewSN);

        if (IsFactoryMode) {
            mBtAuto.setVisibility(View.GONE);
            mBtUpgrade.setVisibility(View.GONE);
        } else {
            mBtCalibration.setVisibility(View.GONE);
            mBtUpgrade.setVisibility(View.GONE);
            mBleCy7c63813.setVisibility(View.GONE);
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onNewIntent(intent);
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

    public void initDefaultSetting() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //factory mode
        IsFactoryMode = sharedPreferences.getBoolean("factory_mode_switch", false);
        //auto test mode
        AutoTestMode = sharedPreferences.getBoolean("auto_test_mode_switch", true);
        //item test timeout
        String string = sharedPreferences.getString("item_test_timeout_list", "10");
        ItemTestTimeout = Integer.valueOf(string);
        // accel full scale range
        string = sharedPreferences.getString("accel_full_scale_select", "3");
        Accel_FullScale_Range = Integer.valueOf(string);
        // gyro full scale range
        string = sharedPreferences.getString("gyro_full_scale_select", "300");
        Gyro_FullScale_Range = Float.valueOf(string);
        // proximity sensor threshold approach
        string = sharedPreferences.getString("proximity_threshold_approach", "820");
        Proximity_Threshold_Approach = Integer.valueOf(string);
        // light sensor threshold approach
        string = sharedPreferences.getString("light_threshold_approach", "100");
        Light_Threshold_Approach = Integer.valueOf(string);
        // temp range min, max
        string = sharedPreferences.getString("temp_range_min", "-40");
        Temperature_Range_Min = Integer.valueOf(string);
        string = sharedPreferences.getString("temp_range_max", "85");
        Temperature_Range_Max = Integer.valueOf(string);
        // magnetic yaw offset
        string = sharedPreferences.getString("magnetic_yaw_offset", "90");
        Magnetic_Yaw_Offset = Integer.valueOf(string);
        //
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() +
                String.format(Locale.US,"->%n" +
                        "IsFactoryMode=%b%n" +
                        "AutoTestMode=%b%n" +
                        "ItemTestTimeout=%ds%n" +
                        "Accel_FullScale_Range=%dg%n" +
                        "Gyro_FullScale_Range=%fdps%n" +
                        "Proximity_Threshold_Approach=%d%n" +
                        "Light_Threshold_Approach=%d%n" +
                        "Temperature_Range_Min=%d℃%n" +
                        "Temperature_Range_Max=%d℃%n" +
                        "Magnetic_Yaw_Offset=%dº%n",
                        IsFactoryMode,
                        AutoTestMode,
                        ItemTestTimeout,
                        Accel_FullScale_Range,
                        Gyro_FullScale_Range,
                        Proximity_Threshold_Approach,
                        Light_Threshold_Approach,
                        Temperature_Range_Min,
                        Temperature_Range_Max,
                        Magnetic_Yaw_Offset
                        ));
    }

    private void initTestItems() {
        //add test items
        itemIds = new ArrayList<Integer>();
        itemIds.clear();
        if (!IsFactoryMode) {
            itemIds.add(R.string.KeyCode_name);
            itemIds.add(R.string.lsensor_name);
            itemIds.add(R.string.psensor_name);
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
        textViewVersion.setText(String.format("%s: %s%n%s: %s%n%s: %s%n%s: %s",
                getString(R.string.device_version_ble), getString(R.string.device_unknown),
                getString(R.string.device_version_63813), getString(R.string.device_unknown),
                getString(R.string.device_version_stm32), getString(R.string.device_unknown),
                getString(R.string.device_ble_state), getString(R.string.device_unknown)));
        textViewSN.setText(String.format(Locale.US, "%s: %s",
                getString(R.string.device_serial_number), getString(R.string.device_unknown)));
    }

    public View.OnClickListener cl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent();
            int reqId = -1;
            if (v.getId() == mBtAuto.getId()) {
                intent.setClassName("com.zhi_tech.devicetestapp", "com.zhi_tech.devicetestapp.AutoTest");
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
            }else if (v.getId() == mBtCheckSN.getId()) {
                //check version request
                if (dtaService != null) {
                    dtaService.StartToReadSerialNumber();
                }
            } else if (v.getId() == mBtSetting.getId()) {
                intent.setClassName(DeviceTestApp.this, "com.zhi_tech.devicetestapp.SettingsActivity");
                startActivity(intent);
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
            //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            String name = mListData.get(position);
            String classname = null;
            if (name.equals(getString(R.string.bluetooth_name))) {
                classname = "com.zhi_tech.devicetestapp.bluetooth.Bluetooth";
            } else if (name.equals(getString(R.string.gsensor_name))) {
                classname = "com.zhi_tech.devicetestapp.sensor.GSensor";
            } else if (name.equals(getString(R.string.msensor_name))) {
                classname = "com.zhi_tech.devicetestapp.sensor.MSensor";
            } else if (name.equals(getString(R.string.lsensor_name))) {
                classname = "com.zhi_tech.devicetestapp.sensor.LSensor";
            } else if (name.equals(getString(R.string.psensor_name))) {
                classname = "com.zhi_tech.devicetestapp.sensor.PSensor";
            } else if (name.equals(getString(R.string.KeyCode_name))) {
                classname = "com.zhi_tech.devicetestapp.KeyCode";
            }else if(name.equals(getString(R.string.gyroscopesensor_name))) {
                classname = "com.zhi_tech.devicetestapp.sensor.GyRoscopeSensor";
            }else if(name.equals(getString(R.string.tsensor_name))) {
                classname = "com.zhi_tech.devicetestapp.sensor.TSensor";
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
        //System.gc();
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
        //close app when usb detached
        android.os. Process.killProcess(android.os.Process.myPid());
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
        return true; //super.dispatchKeyEvent(event);
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

        if (IsFactoryMode) {
            if (okFlag != 0 && !mBleCy7c63813IsConnected) {
                mBleCy7c63813IsConnected = true;
                mBleCy7c63813.setTextColor(Color.GREEN);
                Toast toast=Toast.makeText(getApplicationContext(), getString(R.string.ble_test_tip), Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                if (IsFactoryMode) {
                    if (dtaService != null) {
                        dtaService.connectToDevice();
                        dtaService.StartToCalibration();
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(DeviceTestApp.this, SettingsActivity.class);
                item.setIntent(intent);
                break;
            case R.id.action_about_info:
                Toast toast=Toast.makeText(getApplicationContext(),
                        String.format(Locale.US, "%s%n%s: %s",
                                getString(R.string.app_name), getString(R.string.about_us_info), getVersionName(this)),
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static String getVersionName(Context context) {
        return getPackageInfo(context).versionName;
    }

    public static int getVersionCode(Context context) {
        return getPackageInfo(context).versionCode;
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo pi = null;

        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_CONFIGURATIONS);

            return pi;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pi;
    }
}

