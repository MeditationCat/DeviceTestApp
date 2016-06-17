package com.zhi_tech.taipp.devicetestapp;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private UsbEndpoint epBulkOut, epBulkOut2, epBulkIn, epBulkIn2, epBulkIn3;
    private UsbEndpoint epIntOut, epIntOut2, epIntIn, epIntIn2, epIntIn3;
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
        return new DtaBinder();//dtaBinder;
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
        epBulkOut2 = null;
        epBulkIn3 = null;
        epIntIn2 = null;
        epIntOut2 = null;
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
                                if (ep.getAddress() == 0x01) {
                                    epBulkOut = ep;
                                } else if (ep.getAddress() == 0x02) {
                                    epBulkOut2 = ep;
                                }
                                Log.d(TAG,"epBulkOut info->addr:" + ep.getAddress() + " epNumber:" + ep.getEndpointNumber());
                            } else {
                                if (ep.getAddress() == 0x81) {
                                    epBulkIn = ep;
                                } else if (ep.getAddress() == 0x82) {
                                    epBulkIn2 = ep;
                                } else if (ep.getAddress() == 0x83) {
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
                                if (ep.getAddress() == 0x01) {
                                    epIntOut = ep;
                                } else if (ep.getAddress() == 0x02) {
                                    epIntOut2 = ep;
                                }
                                Log.d(TAG,"epIntOut info->addr:" + ep.getAddress() + " epNumber:" + ep.getEndpointNumber());
                            }
                            if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                                if (ep.getAddress() == 0x81) {
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

        if (epBulkOut == null && epBulkIn == null && epBulkIn2 == null && epBulkOut2 == null && epBulkIn3 == null && epControl == null
                && epIntOut == null && epIntIn == null && epIntIn2 == null && epIntOut2 == null && epIntIn3 == null) {
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
        private long[] packageDataTimestamp = new long[1];
        //private byte[] packageDataKeyCode = new byte[3];

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
                        int count = 0;
                        byte[] bytes = new byte[2];
                        int offset = 0;
                        while (true) {
                            synchronized (this) {
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

                                            //package header: char: 'M','5'
                                            offset = 0;
                                            for (int i = 0; i < packageHeader.length; i++) {
                                                packageHeader[i] = (char)receiveBuffer.get(offset + i);
                                            }
                                            //package data: gryo: short: x, y, z
                                            offset = 2;
                                            for (int i = 0; i < packageDataGyroscope.length; i++) {
                                                packageDataGyroscope[i] =
                                                        (short) ((receiveBuffer.get(offset + i * 2) & 0xFF) << 8 | (receiveBuffer.get(offset + i * 2 + 1) & 0xFF));
                                            }
                                            //package data: accelerometer: short: x, y, z
                                            offset = 8;
                                            for (int i = 0; i < packageDataAccelerometer.length; i++) {
                                                packageDataAccelerometer[i] =
                                                        (short) ((receiveBuffer.get(offset + i * 2) & 0xFF) << 8 | (receiveBuffer.get(offset + i * 2 + 1) & 0xFF));
                                            }
                                            //package data: magnetic: short: x, y, z
                                            offset = 14;
                                            for (int i = 0; i < packageDataMagnetic.length; i++) {
                                                packageDataMagnetic[i] =
                                                        (short) ((receiveBuffer.get(offset + i * 2) & 0xFF) << 8 | (receiveBuffer.get(offset + i * 2 + 1) & 0xFF));
                                            }
                                            //package data: temperature: short
                                            offset = 20;
                                            packageDataTemperature[0] =
                                                    (short) ((receiveBuffer.get(offset) & 0xFF) << 8 | (receiveBuffer.get(offset + 1) & 0xFF));
                                            //package data: light: short
                                            offset = 22;
                                            packageDataLight[0] =
                                                    (short) ((receiveBuffer.get(offset) & 0xFF) << 8 | (receiveBuffer.get(offset + 1) & 0xFF));
                                            //package data: proximity: short
                                            offset = 24;
                                            packageDataProximity[0] =
                                                    (short) ((receiveBuffer.get(offset) & 0xFF) << 8 | (receiveBuffer.get(offset + 1) & 0xFF));
                                            //package data: timestamp: int
                                            offset = 26;
                                            packageDataTimestamp[0] =
                                                    (long) ((receiveBuffer.get(offset) & 0xFF) | (receiveBuffer.get(offset + 1) & 0xFF) << 8
                                                            | (receiveBuffer.get(offset + 2) & 0xFF) << 16 | (receiveBuffer.get(offset + 3) & 0xFF) << 24);

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

                                            for (int i = 0; i < 30; i++) {
                                                Log.d(TAG, String.format("receiveBuffer[%d] = 0x%02x", i, receiveBuffer.get(i)));
                                            }
                                            if (count++ > 5) {
                                                break;
                                            }
                                            //*/
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

    public void StartUpgrade() {
    //set up to send cmd to the device or receive data from the device.
    //send request command to the device.
    UsbEndpoint epIn, epOut, epOut2;
    if (epIntOut != null && epIntIn != null) {
        epIn = epIntIn;
        epOut = epIntOut;
        epOut2 = epIntOut2;
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("epIn = %x,epIntOut = %x,epIntOut2 = %x", epIn.getAddress(), epIntOut.getAddress(), epIntOut2.getAddress()));
    } else if (epBulkOut != null && epBulkIn != null && epBulkOut2 != null) {
        epIn = epBulkIn;
        epOut = epBulkOut;
        epOut2 = epBulkOut2;
    } else {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->epIn and epOut are null!");
        return;
    }

    UsbRequest sendRequest = new UsbRequest();
    UsbRequest receiveRequest= new UsbRequest();
    //UsbRequest sendDataRequest = new UsbRequest();

    if (!sendRequest.initialize(usbDeviceConnection, epOut)) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->sendRequest.initialize() failed!");
        return;
    }
    if (!receiveRequest.initialize(usbDeviceConnection, epIn)) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->receiveRequest.initialize() failed!");
        return;
    }
    //if (!sendDataRequest.initialize(usbDeviceConnection, epOut2)) {
    //    Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->sendDataRequest.initialize() failed!");
    //    return;
    //}

    int sendBufferLength = epOut.getMaxPacketSize();
    ByteBuffer sendBuffer = ByteBuffer.allocate(sendBufferLength);
    int receiveBufferLength = epIn.getMaxPacketSize();
    ByteBuffer receiveBuffer = ByteBuffer.allocate(receiveBufferLength);

    int cmd = 0xA0;
    short dataLength = 0;
    //readFileByBytes
    byte[] readBuffer = new byte[60];
    int readBytes = 0;
    int fileSize = 0;
    //String filePath = getResources().getResourceEntryName();

    FileInputStream inputStream = null;
    try {
        String path = Environment.getExternalStorageDirectory() + "/USBIAP.bin";
        File binFile = new File(path);
        if (!binFile.exists()) {
            Log.d(TAG, "Failed to open file!");
            return;
        }
        inputStream = new FileInputStream(path);
        fileSize = (int) inputStream.getChannel().size();

        Log.d(TAG, "FilePath: " + path + "   fileSize:" + fileSize);

    } catch (IOException e) {
        e.printStackTrace();
    }

    int count = 0;
    int sendCount = 0;
    while (true) {
        sendBuffer.rewind();
        sendBuffer.clear();
        receiveBuffer.rewind();

        switch (cmd) {
            case 0xA0:
                Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + String.format("0x%x", cmd));
                sendBuffer.put((byte) 0xA0);
                sendBuffer.put((byte) 0x00);

                if (sendRequest.queue(sendBuffer, sendBuffer.position() + 1)) {
                    cmd = 0x00;
                    while(true){
                        if (receiveRequest.queue(receiveBuffer, receiveBuffer.array().length)) {
                            if (receiveRequest.equals(usbDeviceConnection.requestWait())) {
                                cmd = receiveBuffer.get(0);
                                cmd = cmd&0xff;
                                Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->0xA0 : " + String.format("0x%x", cmd));
                                if (cmd == 0xA1) {
                                    break;
                                }
                            }
                        }
                    }
                }
                break;

            case 0xA1:
                Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + String.format("0x%x", cmd));
                cmd = receiveBuffer.get();
                cmd = cmd&0xff;
                dataLength = receiveBuffer.get();
                Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->fileSize : " + fileSize);
                sendBuffer.put((byte) 0xA2);
                sendBuffer.put((byte) 0x04);
                sendBuffer.putInt(fileSize);

                if (sendRequest.queue(sendBuffer, sendBuffer.position() + 1)) {
                    cmd = 0x00;
                    if (fileSize == 0) {
                        Log.d(TAG, "fileSize == 0! exit!");
                        return;
                    }
                    while (true) {
                    if (receiveRequest.queue(receiveBuffer, receiveBuffer.array().length)) {
                        if (receiveRequest.equals(usbDeviceConnection.requestWait())) {
                            cmd = receiveBuffer.get(0);
                            cmd = cmd&0xff;
                            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->0xA2 : " + String.format("0x%x", cmd));
                            if (cmd == 0xA3) {
                                break;
                            }
                        }
                    }
                }}
                break;

            case 0xA3:
            //case 0xA5:
                Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + String.format("0x%x", cmd));
                cmd = receiveBuffer.get();
                cmd = cmd&0xff;
                dataLength = receiveBuffer.get();
                try {
                    if (inputStream == null) {
                        Log.d(TAG,"inputStream == null");
                        return;
                    }
                    while ((readBytes = inputStream.read(readBuffer)) != -1) {
                        sendBuffer.rewind();
                        sendBuffer.clear();
                        sendBuffer.put((byte) 0xA4);
                        sendBuffer.put((byte) readBytes);
                        sendBuffer.put(readBuffer);

                        if (sendRequest.queue(sendBuffer, sendBuffer.position() + 1)) {
                            sendCount++;
                        } else {
                            Log.d(TAG, "send error!");
                        }
                        Log.d(TAG, "StartUpgrade:send Bytes:" + readBytes + "sendCount: " + sendCount);
                        if (readBytes < 60) {
                            cmd = 0;
                            return;
                            //break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 0xA6:
                Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->0xA6");
                cmd = 0x00;
                break;

            default:
                sendCount = 0;
                break;
        }
    }
}

    public String getDeviceInformation() {
        if (usbDevice != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return String.format("Manufacturer: %s%n ProductName: %s%n", usbDevice.getManufacturerName(), usbDevice.getProductName());
            }
        }
        return String.format("Manufacturer: %s%n ProductName: %s%n", "Unknown", "Unknown");
    }
    private void startCommunication2() {
        StartUpgrade();
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
        UsbRequest receiveRequest= new UsbRequest();

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

        byte cmd =0x01;
        while (true) {
            sendBuffer.rewind();
            sendBuffer.clear();
            receiveBuffer.rewind();
            switch (cmd) {
                case 0x01:
                    Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->send cmd 0xAA!");
                    //send  EP1 to device
                    sendBuffer.put((byte) 0xAA);
                    sendBuffer.put((byte) 0x00);
                    break;

                case 0x02:
                    Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->send cmd 0x03!");
                    //get EP2 and decrypt it then send EP3 to device
                    cmd = receiveBuffer.get();
                    byte cmdLength = receiveBuffer.get();
                    short dataLength = receiveBuffer.getShort();
                    int[] encryptData = new int[dataLength/4], decryptData;
                    receiveBuffer.asIntBuffer().get(encryptData);
                    //decryptData = check_sum(encryptData);

                    sendBuffer.put((byte) 0x03);
                    sendBuffer.putShort((short) (encryptData.length * 4));
                    //sendBuffer.asIntBuffer().put(decryptData);
                    break;

                case 0x04:
                    Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->get cmd 0x04!");
                    if (sendBuffer.get(4) == 0x00 || true) {
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
                    return;

                default:
                    break;
            }

            if (sendRequest.queue(sendBuffer, sendBuffer.position() + 1)) {
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
                    //startCommunication();
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