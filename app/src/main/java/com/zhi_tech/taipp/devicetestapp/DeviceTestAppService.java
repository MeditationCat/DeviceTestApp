package com.zhi_tech.taipp.devicetestapp;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DeviceTestAppService extends Service {

    private static final String TAG = "DeviceTestAppService";

    private OnDataChangedListener onDataChangedListener;

    private DtaBinder dtaBinder = null;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private List<UsbInterface> usbInterfaceList;
    private UsbInterface usbInterface, usbInterface2;
    private UsbEndpoint epControl;
    private UsbEndpoint epBulkOut, epBulkIn, epBulkIn2, epBulkIn3;
    private UsbEndpoint epIntOut, epIntIn, epIntIn2, epIntIn3;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbRequest usbRequestEpIntIn2;

    private PendingIntent pendingIntent = null;

    @Override
    public void onCreate() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onCreate();
        dtaBinder = new DtaBinder();
        usbInterfaceList = new ArrayList<>();
        pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
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
        usbInterface2 = null;

        epControl = null;

        epBulkOut = null;
        epBulkIn = null;
        epIntOut = null;
        epIntIn = null;

        epBulkIn2 = null;
        epBulkIn3 = null;
        epIntIn2 = null;
        epIntIn3 = null;

        usbDeviceConnection = null;
        usbRequestEpIntIn2 = null;
    }

    public void startToConnectDevice() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        if (deviceIsConnected()) {
            Log.d(TAG, "device has been already connected!");
            return;
        }
        try {
            initConnection();
            usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            enumerateDevice();
            getDeviceInterface();

            if (!usbInterfaceList.isEmpty()) {
                setDeviceInterface(usbInterfaceList);
            }

            assignEndpoint(usbInterfaceList);

            if (checkDevicePermission(usbDevice)) {
                if (connectToDevice(usbInterface)) {
                    startCommunication();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enumerateDevice() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        if (usbManager != null) {

            ArrayList<UsbDevice> devices;
            try {
                devices = UsbDeviceFilter.getMatchingHostDevices(this, R.xml.device_filter);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "Failed to parse devices.xml: " + e.getMessage());
                return;
            }

            for (UsbDevice device : devices) {
                usbDevice = device;
                Log.d(TAG, "Matched device->{vid, pid} ->{" + device.getVendorId() + ", " + device.getProductId() + "}");
                Log.d(TAG, "Matched device->" + device.getDeviceName());
            }

            /* try {
                HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

                if (!(deviceList.isEmpty())) {
                    final int vendorID =0x2d29; //48899;
                    final int productID = 0x1001; //48898;

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
            } */
        } else {
            Log.d(TAG, "usbManager = null!");
        }
    }

    private void getDeviceInterface() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
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

    private void setDeviceInterface(List<UsbInterface> interfaceList) {
        if (!interfaceList.isEmpty()) {
            for (int i = 0; i < interfaceList.size(); i++) {
                if (interfaceList.get(i).getId() == 0) {
                    usbInterface = interfaceList.get(i);
                } else if (interfaceList.get(i).getId() == 1) {
                    usbInterface2 = interfaceList.get(i);
                } else {
                    //
                    Log.d(TAG, "interface[" + i + "]->" + interfaceList.get(i).getId());
                }

            }
        }
    }

    private void assignEndpoint(List<UsbInterface> interfaceList) {

        for (UsbInterface mInterface : interfaceList) {
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
                                if (ep.getEndpointNumber() == 0x81) {
                                    epBulkIn = ep;
                                } else if (ep.getEndpointNumber() == 0x82) {
                                    epBulkIn2 = ep;
                                } else if (ep.getEndpointNumber() == 0x83) {
                                    epBulkIn3 = ep;
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
                                if (ep.getEndpointNumber() == 0x81) {
                                    epIntIn = ep;
                                } else if (ep.getAddress() == 0x82) {
                                    epIntIn2 = ep;
                                } else if (ep.getAddress() == 0x83) {
                                    epIntIn3 = ep;
                                }
                                Log.d(TAG,"epIntIn info->addr:" + ep.getAddress() + " epNumber:" + ep.getEndpointNumber());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (epBulkOut == null && epBulkIn == null && epBulkIn2 == null && epBulkIn3 == null && epControl == null
                && epIntOut == null && epIntIn == null && epIntIn2 == null && epIntIn3 == null) {
            Log.d(TAG,"No endpoint is available!");
            //throw new IllegalArgumentException("No endpoint is available!");
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
                if (checkDevicePermission(usbDevice)) {
                    usbDeviceConnection = usbManager.openDevice(usbDevice);
                } else {
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
        return (usbDeviceConnection != null && usbDeviceConnection.getSerial() != null);
    }

    public class InterruptTransferThread implements Runnable {

        private String name;

        private char[] packageHeader = new char[2];
        private short[] packageDataGyroscope = new short[3];
        private short[] packageDataAccelerometer = new short[3];
        private short[] packageDataMagnetic = new short[3];
        private short[] packageDataTemperature = new short[1];
        private short[] packageDataLight = new short[1];
        private short[] packageDataProximity = new short[1];
        private int[] packageDataTimestamp = new int[1];

        InterruptTransferThread(String name) {
            this.name = name;
        }

        @Override
        public void run() {

            try {
                if (deviceIsConnected() && epIntIn2 != null) {
                    // receive data
                    int receiveBufferLength = epIntIn2.getMaxPacketSize();
                    ByteBuffer receiveBuffer = ByteBuffer.allocate(receiveBufferLength);

                    usbRequestEpIntIn2 = new UsbRequest(); // create an URB
                    boolean initialized = usbRequestEpIntIn2.initialize(usbDeviceConnection, epIntIn2);
                    if (initialized) {
                        SensorPackageObject sensorPackageObject = new SensorPackageObject();

                        long timeStamp = System.currentTimeMillis();
                        long timeStamp2;
                        Log.d(TAG, Thread.currentThread().getName() + "InterruptTransfer->Start receive package data!");
                        while (true) {
                            try {
                                if (usbRequestEpIntIn2.queue(receiveBuffer, receiveBuffer.array().length)) {
                                    if (usbRequestEpIntIn2.equals(usbDeviceConnection.requestWait())) {
                                        timeStamp2 = System.currentTimeMillis();
                                        if (timeStamp2 - timeStamp > 8) {
                                            Log.d(TAG, Thread.currentThread().getName() + "->" + String.valueOf(timeStamp2 - timeStamp) + "ms");
                                        } else {
                                            //Log.d(TAG, "gap: " + String.valueOf(timeStamp2 - timeStamp) + "ms");
                                        }
                                        timeStamp = timeStamp2;

                                        receiveBuffer.rewind();
                                        //package header: char: 'M','5'
                                        for (int i = 0; i < packageHeader.length; i++) {
                                            packageHeader[i] = (char)receiveBuffer.get(i);
                                        }
                                        //package data: gryo: short: x, y, z
                                        for (int i = 0; i < packageDataGyroscope.length; i++) {
                                            packageDataGyroscope[i] = receiveBuffer.getShort(i);
                                        }
                                        //package data: accelerometer: short: x, y, z
                                        for (int i = 0; i < packageDataAccelerometer.length; i++) {
                                            packageDataAccelerometer[i] = receiveBuffer.getShort(i);
                                        }
                                        //package data: magnetic: short: x, y, z
                                        for (int i = 0; i < packageDataMagnetic.length; i++) {
                                            packageDataMagnetic[i] = receiveBuffer.getShort(i);
                                        }
                                        //package data: temperature: short
                                        packageDataTemperature[0] = receiveBuffer.getShort(0);
                                        //package data: light: short
                                        packageDataLight[0] = receiveBuffer.getShort(0);
                                        //package data: proximity: short
                                        packageDataProximity[0] = receiveBuffer.getShort(0);
                                        //package data: timestamp: int
                                        packageDataTimestamp[0] = receiveBuffer.getInt(0);

                                        sensorPackageObject.setHeader(packageHeader);
                                        sensorPackageObject.gyroscopeSensor.setValues(packageDataGyroscope[0], packageDataGyroscope[1], packageDataGyroscope[2]);
                                        sensorPackageObject.accelerometerSensor.setValues(packageDataAccelerometer[0], packageDataAccelerometer[1], packageDataAccelerometer[2]);
                                        sensorPackageObject.magneticSensor.setValues(packageDataMagnetic[0], packageDataMagnetic[1], packageDataMagnetic[2]);
                                        sensorPackageObject.temperatureSensor.setTemperature(packageDataTemperature[0]);
                                        sensorPackageObject.lightSensor.setLightSensorValue(packageDataLight[0]);
                                        sensorPackageObject.proximitySensor.setProximitySensorValue(packageDataProximity[0]);
                                        sensorPackageObject.setTimestampValue(packageDataTimestamp[0]);
                                        /*
                                        Log.d(TAG, String.format("Header:%c%c", (char)packageHeader[0], (char)packageHeader[1]));
                                        Log.d(TAG, String.format("Gyroscope:%d,%d,%d", packageDataGyroscope[0], packageDataGyroscope[1], packageDataGyroscope[2]));
                                        Log.d(TAG, String.format("Accelerometer:%d,%d,%d", packageDataAccelerometer[0], packageDataAccelerometer[1], packageDataAccelerometer[2]));
                                        Log.d(TAG, String.format("Magnetic:%d,%d,%d", packageDataMagnetic[0], packageDataMagnetic[1], packageDataMagnetic[2]));
                                        Log.d(TAG, String.format("Temperature:%d", packageDataTemperature[0]));
                                        Log.d(TAG, String.format("Light:%d", packageDataLight[0]));
                                        Log.d(TAG, String.format("Proximity:%d", packageDataProximity[0]));
                                        Log.d(TAG, String.format("Timestamp:%d", packageDataTimestamp[0]));
                                        */
                                        onDataChangedListener.sensorDataChanged(sensorPackageObject);
                                    } else {
                                        Log.d(TAG, Thread.currentThread().getName() + "->usbDeviceConnection.requestWait() failed!");
                                    }
                                } else {
                                    Log.d(TAG, Thread.currentThread().getName() + "->request.queue() failed!");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    } else {
                        Log.d(TAG, Thread.currentThread().getName() + "->request.initialize() failed!");
                    }
                } else {
                    Log.d(TAG, Thread.currentThread().getName() + "-> device is not open or epIntIn2 is null!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class BulkTransferThread implements Runnable {

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

        BulkTransferThread(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                if (deviceIsConnected() && epBulkIn != null) {
                    // receive data
                    int receiveBufferLength = epBulkIn.getMaxPacketSize();
                    ByteBuffer receiveBuffer = ByteBuffer.allocate(receiveBufferLength);
                    int receivedLength = -1;
                    SensorPackageObject sensorPackageObject = new SensorPackageObject();

                    long timeStamp = System.currentTimeMillis();
                    long timeStamp2;

                    Log.d(TAG, Thread.currentThread().getName() + "bulkTransfer->Start receive package data!");
                    while (true) {
                        try {
                            receivedLength = usbDeviceConnection.bulkTransfer(epBulkIn, receiveBuffer.array(), receiveBuffer.array().length, TIME_OUT);

                            timeStamp2 = System.currentTimeMillis();
                            if (timeStamp2 - timeStamp > 8) {
                                Log.d(TAG, Thread.currentThread().getName() + "->" + String.valueOf(timeStamp2 - timeStamp) + "ms");
                            } else {
                                //Log.d(TAG, "gap: " + String.valueOf(timeStamp2 - timeStamp) + "ms");
                            }
                            timeStamp = timeStamp2;

                            if (receivedLength > -1) {
                                receiveBuffer.rewind();
                                //package header: char: 'M','5'
                                for (int i = 0; i < packageHeader.length; i++) {
                                    packageHeader[i] = (char)receiveBuffer.get(i);
                                }
                                //package data: gryo: short: x, y, z
                                for (int i = 0; i < packageDataGyroscope.length; i++) {
                                    packageDataGyroscope[i] = receiveBuffer.getShort(i);
                                }
                                //package data: accelerometer: short: x, y, z
                                for (int i = 0; i < packageDataAccelerometer.length; i++) {
                                    packageDataAccelerometer[i] = receiveBuffer.getShort(i);
                                }
                                //package data: magnetic: short: x, y, z
                                for (int i = 0; i < packageDataMagnetic.length; i++) {
                                    packageDataMagnetic[i] = receiveBuffer.getShort(i);
                                }
                                //package data: temperature: short
                                packageDataTemperature[0] = receiveBuffer.getShort(0);
                                //package data: light: short
                                packageDataLight[0] = receiveBuffer.getShort(0);
                                //package data: proximity: short
                                packageDataProximity[0] = receiveBuffer.getShort(0);
                                //package data: timestamp: int
                                packageDataTimestamp[0] = receiveBuffer.getInt(0);

                                sensorPackageObject.setHeader(packageHeader);
                                sensorPackageObject.gyroscopeSensor.setValues(packageDataGyroscope[0], packageDataGyroscope[1], packageDataGyroscope[2]);
                                sensorPackageObject.accelerometerSensor.setValues(packageDataAccelerometer[0], packageDataAccelerometer[1], packageDataAccelerometer[2]);
                                sensorPackageObject.magneticSensor.setValues(packageDataMagnetic[0], packageDataMagnetic[1], packageDataMagnetic[2]);
                                sensorPackageObject.temperatureSensor.setTemperature(packageDataTemperature[0]);
                                sensorPackageObject.lightSensor.setLightSensorValue(packageDataLight[0]);
                                sensorPackageObject.proximitySensor.setProximitySensorValue(packageDataProximity[0]);
                                sensorPackageObject.setTimestampValue(packageDataTimestamp[0]);

                                onDataChangedListener.sensorDataChanged(sensorPackageObject);
                            } else {
                                Log.d(TAG, Thread.currentThread().getName() + "-> bulkTransfer failed!");
                            }
                            Thread.sleep(4);
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                } else {
                    Log.d(TAG, Thread.currentThread().getName() + "-> device is not open or epBulkIn is null!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //encrypt code
    final int L01 = 1;
    final int L02 = 2;
    final int L03 = 3;
    final int L04 = 4;
    final int L05 = 5;
    final int L06 = 6;
    final int L07 = 7;
    final int L08 = 8;
    final int L09 = 9;
    final int L10 = 10;
    final int L11 = 11;
    final int L12 = 12;
    final int L13 = 13;
    final int L14 = 14;
    final int L15 = 15;

    final int B0 = 0;
    final int B1 = 1;
    final int B2 = 2;
    final int B3 = 3;
    final int B4 = 4;
    final int B5 = 5;
    final int B6 = 6;
    final int B7 = 7;

    final int[][] jiami_data_tab = { { L13, B7 }, { L12, B6 }, { L11, B5 },
            { L02, B1 }, { L02, B0 }, { L10, B7 }, { L09, B6 }, { L08, B5 },
            { L13, B6 }, { L12, B5 }, { L11, B4 }, { L03, B1 }, { L03, B0 },
            { L10, B6 }, { L09, B5 }, { L08, B4 }, { L13, B5 }, { L12, B4 },
            { L11, B3 }, { L04, B1 }, { L04, B0 }, { L10, B5 }, { L09, B4 },
            { L08, B3 }, { L13, B4 }, { L12, B3 }, { L11, B2 }, { L05, B1 },
            { L05, B0 }, { L10, B4 }, { L09, B3 }, { L08, B2 }, { L13, B3 },
            { L12, B2 }, { L11, B1 }, { L06, B1 }, { L06, B0 }, { L10, B3 },
            { L09, B2 }, { L08, B1 }, { L13, B2 }, { L12, B1 }, { L11, B0 },
            { L07, B1 }, { L07, B0 }, { L10, B2 }, { L09, B1 }, { L08, B0 },
            { L07, B7 }, { L05, B6 }, { L06, B5 }, { L04, B4 }, { L02, B3 },
            { L08, B7 }, { L08, B6 }, { L03, B5 }, { L07, B6 }, { L05, B5 },
            { L06, B4 }, { L04, B3 }, { L02, B2 }, { L09, B7 }, { L09, B0 },
            { L03, B4 }, { L07, B5 }, { L05, B4 }, { L06, B3 }, { L04, B2 },
            { L02, B4 }, { L10, B1 }, { L10, B0 }, { L03, B3 }, { L07, B4 },
            { L05, B3 }, { L06, B2 }, { L04, B5 }, { L02, B5 }, { L11, B7 },
            { L11, B6 }, { L03, B2 }, { L07, B3 }, { L05, B2 }, { L06, B6 },
            { L04, B6 }, { L02, B6 }, { L12, B7 }, { L12, B0 }, { L03, B6 },
            { L07, B2 }, { L05, B7 }, { L06, B7 }, { L04, B7 }, { L02, B7 },
            { L13, B1 }, { L13, B0 }, { L03, B7 } };

    public int[] check_sum(int[] in)
    {
        int[] out = new int[16];
        // int i=0;

        int i, j, n, m;
        for (i = 0; i < 12; i++)
        {
            for (j = 0; j < 8; j++)
            {
                n = jiami_data_tab[i * 8 + j][1];
                m = in[i + 2] & (1 << (7 - j));
                if (m != 0)
                {
                    out[jiami_data_tab[i * 8 + j][0]] |= (1 << n);
                }
            }
        }
        out[0] = in[0];
        out[0] = in[0];
        out[14] = in[14];
        out[15] = in[15];
        return out;
    }




    private void startCommunication() {
        //set up to send cmd to the device or receive data from the device.
        //send request command to the device.
        UsbEndpoint epIn, epOut;
        if (epIntOut != null && epIntIn != null) {
            epIn = epIntIn;
            epOut = epIntOut;
        } else if (epBulkOut != null && epBulkIn != null) {
            epIn = epBulkIn;
            epOut = epBulkOut;
        } else {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->epIn and epOut are null!");
            return;
        }

        UsbRequest sendRequest = new UsbRequest();
        UsbRequest receiveRequest = new UsbRequest();
        if (!sendRequest.initialize(usbDeviceConnection, epOut)) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->sendRequest.initialize() failed!");
            return;
        }
        if (!receiveRequest.initialize(usbDeviceConnection, epIn)) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->receiveRequest.initialize() failed!");
            return;
        }

        int sendBufferLength = epOut.getMaxPacketSize();
        ByteBuffer sendBuffer = ByteBuffer.allocate(sendBufferLength);
        int receiveBufferLength = epIn.getMaxPacketSize();
        ByteBuffer receiveBuffer = ByteBuffer.allocate(receiveBufferLength);

        byte[] cmdEp1 = new byte[3];
        cmdEp1[0] = 0x09;
        cmdEp1[1] = 0x00;
        cmdEp1[2] = 0x00;

        byte cmd =0x01;

        while (true) {
            sendBuffer.rewind();
            sendBuffer.clear();
            receiveBuffer.rewind();
            switch (cmd) {
                case 0x01:
                    //send  EP1 to device
                    sendBuffer.put((byte) 0x09);
                    sendBuffer.put((byte) 0x00);
                    sendBuffer.put((byte) 0x00);
                    break;

                case 0x02:
                    //get EP2 and decrypt it then send EP3 to device
                    cmd = receiveBuffer.get();
                    byte cmdLength = receiveBuffer.get();
                    short dataLength = receiveBuffer.getShort();
                    int[] encryptData = new int[dataLength/4], decryptData;
                    receiveBuffer.asIntBuffer().get(encryptData);
                    decryptData = check_sum(encryptData);

                    sendBuffer.put((byte) 0x03);
                    sendBuffer.put((byte) 0x01);
                    sendBuffer.putShort((short) (encryptData.length * 4));
                    sendBuffer.asIntBuffer().put(decryptData);
                    break;

                case 0x04:
                    if (sendBuffer.get(4) == 0x01 || true) {
                        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->shake hands succeed, start to receive sensor data!");
                        // start new thread to parse package
                        if (epIn.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            BulkTransferThread bulkTransferThread = new BulkTransferThread("BulkTransferThread");
                            Thread bulkThread = new Thread(bulkTransferThread, bulkTransferThread.name);
                            bulkThread.start();
                        } else if (epIn.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                            InterruptTransferThread  interruptTransferThread = new InterruptTransferThread("InterruptTransferThread");
                            Thread interruptThread = new Thread(interruptTransferThread, interruptTransferThread.name);
                            interruptThread.start();
                        } else {
                        }
                    }
                    break;

                default:
                    break;
            }

            if (sendRequest.queue(sendBuffer, sendBuffer.arrayOffset())) {
                if (receiveRequest.queue(receiveBuffer, receiveBuffer.array().length)) {
                    if (receiveRequest.equals(usbDeviceConnection.requestWait())) {
                        cmd = receiveBuffer.get(0);
                    }
                }
            }
            // disable shake hands
            cmd = 0x04;
        }
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
                } else {
                    startCommunication();
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
                            if (usbRequestEpIntIn2 != null) {
                                usbRequestEpIntIn2.close();
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

}