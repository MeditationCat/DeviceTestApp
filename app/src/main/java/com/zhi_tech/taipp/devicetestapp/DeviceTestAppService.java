package com.zhi_tech.taipp.devicetestapp;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.nio.ByteBuffer;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DeviceTestAppService extends Service {

    private static final String TAG = "DeviceTestAppService";

    private Handler mHandler = new Handler();

    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.d(TAG, "mHandler---mRunnable!---");
            //readDataFromNvram();
            //checkDVTResult();
            //mHandler.postDelayed(this, delaytime);
        }
    };

    private OnDataChangedListener onDataChangedListener;

    private DtaBinder dtaBinder = null;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private List<UsbInterface> usbInterfaceList;
    private UsbInterface usbInterface;
    private UsbEndpoint epControl;
    private UsbEndpoint epBulkOut,epBulkOut2, epBulkIn, epBulkIn2;
    private UsbEndpoint epIntOut, epIntOut2, epIntIn, epIntIn2;
    private UsbDeviceConnection usbDeviceConnection;

    private PendingIntent pendingIntent = null;

    @Override
    public void onCreate() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onCreate();
        dtaBinder = new DtaBinder();
        usbInterfaceList = new ArrayList<>();
        pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + intent.getAction());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + intent.getAction());
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + intent.getAction());
        return dtaBinder;
    }

    public class DtaBinder extends Binder {
        public DeviceTestAppService getService() {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
            return DeviceTestAppService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + intent.getAction());
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    public void setOnDataChangedListener(OnDataChangedListener onDataChangedListener) {
        this.onDataChangedListener = onDataChangedListener;
    }

    /* usb device operation methods */
    private  void initConnection() {
        usbManager = null;
        usbDevice = null;
        usbInterfaceList.clear();
        usbInterface = null;

        epControl = null;

        epBulkOut = null;
        epBulkIn = null;
        epIntOut = null;
        epIntIn = null;

        epBulkOut2 = null;
        epBulkIn2 = null;
        epIntOut2 = null;
        epIntIn2 = null;

        usbDeviceConnection = null;
    }

    public void startToConnectDevice() {
        try {
            initConnection();
            usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            enumerateDevice();
            getDeviceInterface();

            if (!usbInterfaceList.isEmpty()) {
                setDeviceInterface(usbInterfaceList.get(0));
                if (usbInterfaceList.size() > 1) {
                    // custom to select one interface;
                    Log.d(TAG, "usbInterfaceList.size() = " + usbInterfaceList.size());
                }
            }

            assignEndpoint(usbInterface);

            if (checkDevicePermission(usbDevice)) {
                connectToDevice(usbInterface);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enumerateDevice() {

        if (usbManager != null) {
            try {
                HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

                if (!(deviceList.isEmpty())) {
                    final int vendorID = 48899;
                    final int productID = 48898;

                    Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

                    while (deviceIterator.hasNext()) {
                        UsbDevice device = deviceIterator.next();
                        Log.d(TAG, "DeviceInfo: " + device.getVendorId() + " , " + device.getProductId());
                        if (device.getVendorId() == vendorID && device.getProductId() == productID) {
                            usbDevice = device;
                        }
                    }
                } else {
                    Log.d(TAG, "deviceList.isEmpty():" + deviceList.isEmpty());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            Log.d(TAG, "usbManager = null!");
        }
    }

    private void getDeviceInterface() {
        if (usbDevice != null) {
            Log.d(TAG, "usbDevice.getInterfaceCount() : " + usbDevice.getInterfaceCount());

            try {
                for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                    UsbInterface intf = usbDevice.getInterface(i);
                    usbInterfaceList.add(intf);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "usbDevice = null!");
        }
    }

    private void setDeviceInterface(UsbInterface mInterface) {
        if (usbInterface != null) {
            usbInterface = mInterface;
        }
    }

    private void assignEndpoint(UsbInterface mInterface) {

        if (mInterface != null) {
            try {
                for (int i = 0; i < mInterface.getEndpointCount(); i++) {
                    UsbEndpoint ep = mInterface.getEndpoint(i);
                    Log.d(TAG,"ep[" + i + "] = " + ep.toString());
                    // look for bulk endpoint
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                            epBulkOut = ep;
                            Log.d(TAG,"epBulkOut info->addr:" + ep.getAddress() + " epNumber:" + ep.getEndpointNumber());
                        } else {
                            if (ep.getAddress() == 0x82) {
                                epBulkIn = ep;
                            } else {
                                epBulkIn2 = ep;
                            }

                            Log.d(TAG,"epBulkIn info->addr:" + ep.getAddress() + " epNumber:" + ep.getEndpointNumber());
                        }
                    }
                    // look for contorl endpoint
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_CONTROL) {
                        epControl = ep;
                        Log.d(TAG,"epControl info->addr:" + ep.getAddress() + " epNumber:" + ep.getEndpointNumber());
                    }
                    // look for interrupt endpoint
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                        if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                            epIntOut = ep;
                            Log.d(TAG,"epIntOut info->addr:" + ep.getAddress() + " epNumber:" + ep.getEndpointNumber());
                        }
                        if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                            epIntIn = ep;
                            Log.d(TAG,"epIntIn info->addr:" + ep.getAddress() + " epNumber:" + ep.getEndpointNumber());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (epBulkOut == null && epBulkIn == null && epControl == null
                && epIntOut == null && epIntIn == null) {
            Log.d(TAG,"No endpoint is available!");
            throw new IllegalArgumentException("No endpoint is available!");
        }
    }

    private boolean checkDevicePermission(UsbDevice mUsbDevice) {

        if (mUsbDevice != null) {
            try {
                if (usbManager.hasPermission(mUsbDevice)) {
                    return true;
                } else {
                    usbManager.requestPermission(usbDevice, pendingIntent);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private boolean connectToDevice(UsbInterface mInterface) {

        boolean openResult = false;
        if (mInterface != null) {
            try {
                if (usbManager.hasPermission(usbDevice)) {
                    usbDeviceConnection = usbManager.openDevice(usbDevice);
                } else {
                    checkDevicePermission(usbDevice);
                    return false;
                }

                if (usbDeviceConnection != null) {
                    openResult = usbDeviceConnection.claimInterface(mInterface, true);
                    Log.d(TAG, "usbDeviceConnection.getSerial(): " + usbDeviceConnection.getSerial());
                } else {
                    Log.d(TAG, "usbDeviceConnection = null!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return openResult;
    }

    private boolean deviceIsConnected() {
        if (usbDeviceConnection != null && usbDeviceConnection.getSerial() != null) {
            return true;
        }
        return false;
    }

    public class PackageDataParseThread implements Runnable {

        private String name;
        public final static int TIME_OUT = 4;

        private char[] packageHeader = new char[2];
        private short[] packageDataGyroscope = new short[3];
        private short[] packageDataAccelerometer = new short[3];
        private short[] packageDataMagnetic = new short[3];
        private short[] packageDataTemperature = new short[1];
        private short[] packageDataLight = new short[1];
        private short[] packageDataProximity = new short[1];
        private int[] packageDataTimestamp = new int[1];

        PackageDataParseThread(String name) {
            this.name = name;
        }

        @Override
        public void run() {

            if (deviceIsConnected()) {
                // receive data
                int receiveBufferLength = epBulkIn.getMaxPacketSize();
                ByteBuffer receiveBuffer = ByteBuffer.allocate(receiveBufferLength);
                int receivedLength = -1;
                SensorPackageObject sensorPackageObject = new SensorPackageObject();

                while (true) {
                    try {
                        receivedLength = usbDeviceConnection.bulkTransfer(epBulkIn, receiveBuffer.array(), receiveBuffer.array().length, TIME_OUT);
                        //Log.d(TAG, Thread.currentThread().getName() + "->" + System.currentTimeMillis());
                        if (receivedLength > 0) {
                            int dstOffset = 0;
                            //package header: char: 'M','5'
                            receiveBuffer.asCharBuffer().get(packageHeader, dstOffset, packageHeader.length);
                            dstOffset += packageHeader.length;
                            //package data: gryo: short: x, y, z
                            receiveBuffer.asShortBuffer().get(packageDataGyroscope,dstOffset, packageDataGyroscope.length);
                            dstOffset += packageDataGyroscope.length;
                            //package data: accelerometer: short: x, y, z
                            receiveBuffer.asShortBuffer().get(packageDataAccelerometer,dstOffset, packageDataAccelerometer.length);
                            dstOffset += packageDataAccelerometer.length;
                            //package data: magnetic: short: x, y, z
                            receiveBuffer.asShortBuffer().get(packageDataMagnetic,dstOffset, packageDataMagnetic.length);
                            dstOffset += packageDataMagnetic.length;
                            //package data: temperature: short
                            receiveBuffer.asShortBuffer().get(packageDataTemperature,dstOffset, packageDataTemperature.length);
                            dstOffset += packageDataTemperature.length;
                            //package data: light: short
                            receiveBuffer.asShortBuffer().get(packageDataLight,dstOffset, packageDataLight.length);
                            dstOffset += packageDataLight.length;
                            //package data: proximity: short
                            receiveBuffer.asShortBuffer().get(packageDataProximity,dstOffset, packageDataProximity.length);
                            dstOffset += packageDataProximity.length;

                            //package data: timestamp: int
                            receiveBuffer.asIntBuffer().get(packageDataTimestamp, dstOffset, packageDataTimestamp.length);
                            dstOffset += packageDataTimestamp.length;

                            sensorPackageObject.setHeader(packageHeader);
                            sensorPackageObject.gyroscopeSensor.setValues(packageDataGyroscope[0], packageDataGyroscope[1], packageDataGyroscope[2]);
                            sensorPackageObject.accelerometerSensor.setValues(packageDataAccelerometer[0], packageDataAccelerometer[1], packageDataAccelerometer[2]);
                            sensorPackageObject.magneticSensor.setValues(packageDataMagnetic[0], packageDataMagnetic[1], packageDataMagnetic[2]);
                            sensorPackageObject.temperatureSensor.setTemperature(packageDataTemperature[0]);
                            sensorPackageObject.lightSensor.setLightSensorValue(packageDataLight[0]);
                            sensorPackageObject.proximitySensor.setProximitySensorValue(packageDataProximity[0]);
                            sensorPackageObject.setTimestampValue(packageDataTimestamp[0]);

                            onDataChangedListener.packageDataUpdate(sensorPackageObject);
                        }

                        Thread.sleep(4);

                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
    }

    //private
    public void startCommunication() {
        //set up to send cmd to the device or receive data from the device.
        Log.d(TAG, "start to communicate with the device!");
        // test
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        onDataChangedListener.dataUpdate(Calendar.getInstance().getTime().toString());
                        Thread.sleep(4);

                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }).start();

        // start new thread to parse package
        PackageDataParseThread dataParseThread = new PackageDataParseThread("dataParseThread");
        Thread thread1 = new Thread(dataParseThread, "dataParseThread-01");
        Thread thread2 = new Thread(dataParseThread, "dataParseThread-02");

        thread1.start();
        //thread2.start();


    }
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + action);

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "extra permission is granted for device" + device);
                        if(device != null){
                            //call method to set up device communication
                            try {
                                connectToDevice(usbInterface);
                                if (deviceIsConnected()) {
                                    Log.d(TAG, "startCommunication()!");
                                    startCommunication();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                if (!deviceIsConnected()) {
                    Log.d(TAG, "startToConnectDevice()!");
                    startToConnectDevice();
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (device != null) {
                    Log.d(TAG, "close the device connection!");
                    // call your method that cleans up and closes communication with the device
                    synchronized (this) {
                        try {
                            if (usbDeviceConnection != null) {
                                usbDeviceConnection.releaseInterface(usbInterface);
                                usbDeviceConnection.close();
                            }
                            initConnection();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };


    /*
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

    } */
}