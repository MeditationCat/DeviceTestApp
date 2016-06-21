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
import java.lang.reflect.Array;
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

    private OnDeviceStatusListener onDeviceStatusListener;

    private DtaBinder dtaBinder;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private List<UsbInterface> usbInterfaceList;
    private UsbInterface usbInterface;
    private UsbEndpoint EPCTRL;
    private UsbEndpoint[] EPIN, EPOUT;
    private final int MaxEpNumber = 0x0F;
    private UsbDeviceConnection usbDeviceConnection;
    private final int DATA_RECV_TIMEOUT = 4; // ms
    private final int THREAD_SLEEP_TIME = 100; // ms
    private Thread receiveDataThread;
    private Thread commandDaemonThread;

    private PendingIntent pendingIntent;

    @Override
    public void onCreate() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        super.onCreate();
        dtaBinder = new DtaBinder();
        usbInterfaceList = new ArrayList<>();
        EPIN = new UsbEndpoint[MaxEpNumber];
        EPOUT = new UsbEndpoint[MaxEpNumber];
        pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        receiveDataThread = null;
        commandDaemonThread = null;
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

    public void setOnDeviceStatusListener(OnDeviceStatusListener onDeviceStatusListener) {
        this.onDeviceStatusListener = onDeviceStatusListener;
    }

    /* usb device operation methods */
    private  void initConnection() {
        usbManager = null;
        usbDevice = null;
        usbInterfaceList.clear();
        usbInterface = null;

        EPCTRL = null;
        usbDeviceConnection = null;
        receiveDataThread = null;
    }

    public void connectToDevice() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "");
        if (deviceIsOpened()) {
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
                openTargetDevice(usbInterface);
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
                                EPOUT[ep.getEndpointNumber()] = ep;
                                Log.d(TAG, String.format(" EPOUT[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                            } else if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                                EPIN[ep.getEndpointNumber()] = ep;
                                Log.d(TAG, String.format(" EPIN[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                            }
                        }
                        // look for contorl endpoint
                        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_CONTROL) {
                            EPCTRL = ep;
                            Log.d(TAG, String.format(" EPIN[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                        }
                        // look for interrupt endpoint
                        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                            if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                                EPOUT[ep.getEndpointNumber()] = ep;
                                Log.d(TAG, String.format(" EPOUT[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                            } else if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                                EPIN[ep.getEndpointNumber()] = ep;
                                Log.d(TAG, String.format(" EPIN[%d]->address:%#x", ep.getEndpointNumber(), ep.getAddress()));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

    private boolean openTargetDevice(UsbInterface mInterface) {

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

    private boolean deviceIsOpened() {
        return (usbDeviceConnection != null && usbDeviceConnection.getSerial() != null);
    }

    public class ReceiveDataRunnable implements Runnable {

        private char[] packetHeader;
        private short[] packetDataGyroscope;
        private short[] packetDataAccelerometer;
        private short[] packetDataMagnetic;
        private short[] packetDataTemperature;
        private short[] packetDataLight;
        private short[] packetDataProximity;
        private long[] packetDataTimestamp;

        public ReceiveDataRunnable() {
            packetHeader = new char[2];
            packetDataGyroscope = new short[3];
            packetDataAccelerometer = new short[3];
            packetDataMagnetic = new short[3];
            packetDataTemperature = new short[1];
            packetDataLight = new short[1];
            packetDataProximity = new short[1];
            packetDataTimestamp = new long[1];
        }

        @Override
        public void run() {

            if (!deviceIsOpened()) {
                Log.d(TAG, Thread.currentThread().getName() + "->Device is not opened!");
                return;
            }
            if (EPIN[2] == null) {
                Log.d(TAG, Thread.currentThread().getName() + "->EPIN[2] is not available!");
                return;
            }

            int MaxPacketSize = EPIN[2].getMaxPacketSize();
            int retVal = 0;
            int offset = 0;
            byte[] dataBuffer = new byte[MaxPacketSize];
            SensorPackageObject sensorPackageObject = new SensorPackageObject();

            long timeStamp = System.currentTimeMillis();
            long timeStamp2;

            while (true) {
                synchronized (this) {
                    try {
                        retVal = usbDeviceConnection.bulkTransfer(EPIN[2], dataBuffer, dataBuffer.length, DATA_RECV_TIMEOUT);
                    } catch (Exception e) {
                        e.printStackTrace();
                        packetHeader[0] = 'x';
                        packetHeader[1] = 'x';
                        sensorPackageObject.setHeader(packetHeader);
                        sensorPackageObject.setTimestampValue(0);
                        onDataChangedListener.sensorDataChanged(sensorPackageObject);
                        Log.d(TAG, Thread.currentThread().getName() + String.format("->usbDeviceConnection.bulkTransfer(EPIN[2]) failed!"));
                        break;
                    }
                    if (retVal > 0) {
                        timeStamp2 = System.currentTimeMillis();
                        if (timeStamp2 - timeStamp > 8) {
                            Log.d(TAG, Thread.currentThread().getName() + String.format("->%d ms", +timeStamp2 - timeStamp));
                        }
                        timeStamp = timeStamp2;

                        //packet header: char: 'M','5'
                        offset = 0;
                        for (int i = 0; i < packetHeader.length; i++) {
                            packetHeader[i] = (char) dataBuffer[offset + i];
                        }
                        //packet data: gryo: short: x, y, z
                        offset = 2;
                        for (int i = 0; i < packetDataGyroscope.length; i++) {
                            packetDataGyroscope[i] =
                                    (short) ((dataBuffer[offset + i * 2] & 0xFF) << 8 | (dataBuffer[offset + i * 2 + 1] & 0xFF));
                        }
                        //packet data: accelerometer: short: x, y, z
                        offset = 8;
                        for (int i = 0; i < packetDataAccelerometer.length; i++) {
                            packetDataAccelerometer[i] =
                                    (short) ((dataBuffer[offset + i * 2] & 0xFF) << 8 | (dataBuffer[offset + i * 2 + 1] & 0xFF));
                        }
                        //packet data: magnetic: short: x, y, z
                        offset = 14;
                        for (int i = 0; i < packetDataMagnetic.length; i++) {
                            packetDataMagnetic[i] =
                                    (short) ((dataBuffer[offset + i * 2] & 0xFF) << 8 | (dataBuffer[offset + i * 2 + 1] & 0xFF));
                        }
                        //packet data: temperature: short
                        offset = 20;
                        packetDataTemperature[0] =
                                (short) ((dataBuffer[offset] & 0xFF) << 8 | (dataBuffer[offset + 1] & 0xFF));
                        //packet data: light: short
                        offset = 22;
                        packetDataLight[0] =
                                (short) ((dataBuffer[offset] & 0xFF) << 8 | (dataBuffer[offset + 1] & 0xFF));
                        //packet data: proximity: short
                        offset = 24;
                        packetDataProximity[0] =
                                (short) ((dataBuffer[offset] & 0xFF) << 8 | (dataBuffer[offset + 1] & 0xFF));
                        //packet data: timestamp: int
                        offset = 26;
                        packetDataTimestamp[0] =
                                (long) ((dataBuffer[offset] & 0xFF) | (dataBuffer[offset + 1] & 0xFF) << 8
                                        | (dataBuffer[offset + 2] & 0xFF) << 16 | (dataBuffer[offset + 3] & 0xFF) << 24);

                        sensorPackageObject.setHeader(packetHeader);
                        sensorPackageObject.gyroscopeSensor.setValues(packetDataGyroscope[0], packetDataGyroscope[1], packetDataGyroscope[2]);
                        sensorPackageObject.accelerometerSensor.setValues(packetDataAccelerometer[0], packetDataAccelerometer[1], packetDataAccelerometer[2]);
                        sensorPackageObject.magneticSensor.setValues(packetDataMagnetic[0], packetDataMagnetic[1], packetDataMagnetic[2]);
                        sensorPackageObject.temperatureSensor.setTemperature(packetDataTemperature[0]);
                        sensorPackageObject.lightSensor.setLightSensorValue(packetDataLight[0]);
                        sensorPackageObject.proximitySensor.setProximitySensorValue(packetDataProximity[0]);
                        sensorPackageObject.setTimestampValue(packetDataTimestamp[0]);

                        onDataChangedListener.sensorDataChanged(sensorPackageObject);
                        try {
                            Thread.sleep(THREAD_SLEEP_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            packetHeader[0] = 'x';
                            packetHeader[1] = 'x';
                            sensorPackageObject.setHeader(packetHeader);
                            sensorPackageObject.setTimestampValue(0);
                            onDataChangedListener.sensorDataChanged(sensorPackageObject);
                            Log.d(TAG, Thread.currentThread().getName() + String.format("->InterruptedException!"));
                            break;
                        }
                        /*
                        Log.d(TAG, String.format("Header:%c%c", (char)packetHeader[0], (char)packetHeader[1]));
                        Log.d(TAG, String.format("Gyroscope:%d,%d,%d", packetDataGyroscope[0], packetDataGyroscope[1], packetDataGyroscope[2]));
                        Log.d(TAG, String.format("Accelerometer:%d,%d,%d", packetDataAccelerometer[0], packetDataAccelerometer[1], packetDataAccelerometer[2]));
                        Log.d(TAG, String.format("Magnetic:%d,%d,%d", packetDataMagnetic[0], packetDataMagnetic[1], packetDataMagnetic[2]));
                        Log.d(TAG, String.format("Temperature:%d", packetDataTemperature[0]));
                        Log.d(TAG, String.format("Light:%d", packetDataLight[0]));
                        Log.d(TAG, String.format("Proximity:%d", packetDataProximity[0]));
                        Log.d(TAG, String.format("Timestamp:%d", packetDataTimestamp[0]));

                        for (int i = 0; i < 30; i++) {
                            Log.d(TAG, String.format("receiveBuffer[%d] = 0x%02x", i, dataBuffer[i]));
                        }
                        if (count++ > 5) {
                            break;
                        }
                        */
                    }
                }
            }
            packetHeader[0] = 'x';
            packetHeader[1] = 'x';
            sensorPackageObject.setHeader(packetHeader);
            sensorPackageObject.setTimestampValue(0);
            onDataChangedListener.sensorDataChanged(sensorPackageObject);
        }
    }

    public int SendCommandToDevice(byte cmd, byte[] data, int dataLength) {
        byte[] cmdBuffer = new byte[dataLength + 2];
        cmdBuffer[0] = cmd;
        cmdBuffer[1] = (byte) dataLength;
        if (data != null && dataLength > 0) {
            System.arraycopy(data, 0, cmdBuffer, 2, dataLength);
        }

        return SendCommandToDevice(cmdBuffer);
    }

    public int SendCommandToDevice(byte cmd) {
        return SendCommandToDevice(cmd, null, (byte) 0x00);
    }

    public int SendCommandToDevice(byte[] cmdBuffer) {
        int retVal = -1;

        if (!deviceIsOpened()) {
            connectToDevice();
        }
        if (!deviceIsOpened()) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("Device is not opened!"));
            return retVal;
        }

        if (EPOUT[1] == null) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("EPIN[2] is not available!"));
            return retVal;
        }

        if (cmdBuffer == null) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("cmdBuffer == null!"));
            return retVal;
        }

        try {
            retVal = usbDeviceConnection.bulkTransfer(EPOUT[1], cmdBuffer, cmdBuffer.length, DATA_RECV_TIMEOUT);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, Thread.currentThread().getName() + String.format("->usbDeviceConnection.bulkTransfer(EPIN[2]) failed!"));
        }

        startCommandDaemonThread();
        return retVal;
    }

    public class ReceiveCommandRunnable implements Runnable {

        @Override
        public void run() {

            if (!deviceIsOpened()) {
                Log.d(TAG, Thread.currentThread().getName() + "->Device is not opened!");
                return;
            }
            if (EPIN[1] == null) {
                Log.d(TAG, Thread.currentThread().getName() + "->EPIN[1] is not available!");
                return;
            }

            int MaxPacketSize = EPIN[1].getMaxPacketSize();
            int retVal = 0;
            byte[] dataBuffer = new byte[MaxPacketSize];
            SensorPackageObject sensorPackageObject = new SensorPackageObject();

            while (true) {
                synchronized (this) {
                    try {
                        retVal = usbDeviceConnection.bulkTransfer(EPIN[1], dataBuffer, dataBuffer.length, DATA_RECV_TIMEOUT);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, Thread.currentThread().getName() + String.format("->usbDeviceConnection.bulkTransfer(EPIN[1]) failed!"));
                        break;
                    }
                    if (retVal > 0) {
                        ProcessingCommandFeedback(dataBuffer, retVal);
                        try {
                            Thread.sleep(THREAD_SLEEP_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Log.d(TAG, Thread.currentThread().getName() + String.format("->InterruptedException!"));
                            break;
                        }
                    }
                }
            }
        }
    }

    private void ProcessingCommandFeedback(byte[] buffer, int length) {
        int cmd = buffer[0] & 0xFF;
        Log.d(TAG, Thread.currentThread().getName() + String.format("->%#x, %#x, %#x", cmd, buffer[2], buffer[3]));
        switch (cmd) {
            case 0x2C: //G sensor calibration return value;
                onDataChangedListener.sendsorCommandReturnValue(cmd , buffer);
                break;
            case 0xB2: //version number for BLE and CY7C63813
                onDataChangedListener.sendsorCommandReturnValue(cmd, buffer);
                break;
            // dfu upgrade return value;
            case 0xA0:
            case 0xA1:
            case 0xA2:
            case 0xA3:
            case 0xA4:
            case 0xA5:
                DfuUpgradeCase(cmd, buffer, length);
                break;
        }
    }

    private void DfuUpgradeCase(int cmd, byte[] buffer, int length) {
        String path;
        File binFile;
        FileInputStream inputStream = null;
        int fileSize = 0;
        try {
            path = Environment.getExternalStorageDirectory() + "/USBIAP.bin";
            binFile = new File(path);
            if (!binFile.exists()) {
                Log.d(TAG, Thread.currentThread().getName() + String.format("->Failed to open file!"));
                return;
            }
            inputStream = new FileInputStream(path);
            fileSize = (int) inputStream.getChannel().size();
            Log.d(TAG, Thread.currentThread().getName() + String.format("->path = %s fileSize = %d", path, fileSize));
            if (fileSize == 0) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + String.format("cmd = 0x%x", cmd));
        switch (cmd) {
            case 0xA1:
                byte[] cmdbuffer = new byte[6];
                cmdbuffer[0] = (byte) 0xA2;
                cmdbuffer[1] = 0x04;
                cmdbuffer[2] = (byte) ((fileSize >> 3) & 0xFF);
                cmdbuffer[3] = (byte) ((fileSize >> 2) & 0xFF);
                cmdbuffer[4] = (byte) ((fileSize >> 1) & 0xFF);
                cmdbuffer[5] = (byte) (fileSize & 0xFF);
                SendCommandToDevice(cmdbuffer);
                break;
            case 0xA3:
                int readBytes = 0;
                byte[] readBuffer = new byte[60];
                ByteBuffer sendBuffer = ByteBuffer.allocate(1 + 1 + readBuffer.length);
                try {
                    if (inputStream != null) {
                        while ((readBytes = inputStream.read(readBuffer)) != -1) {
                            sendBuffer.rewind();
                            sendBuffer.clear();
                            sendBuffer.put((byte) 0xA4);
                            sendBuffer.put((byte) readBytes);
                            sendBuffer.put(readBuffer);
                            SendCommandToDevice(sendBuffer.array());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 0xA5:
                if (buffer[2] == 0x00) {
                    //succeed
                } else if (buffer[2] == 0x01) {
                    //failed
                } else {
                    //error
                }
                break;
            default:
                break;
        }
    }

    public void StartUpgrade() {
        SendCommandToDevice((byte) 0xA0);
}

    public void StartToCalibration() {
        SendCommandToDevice((byte) 0x2B);
    }

    public void startToReceiveData() {
        SendCommandToDevice((byte) 0x0B);

        if (!deviceIsOpened()) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("deviceIsOpened == false!"));
            return;
        }
        if (receiveDataThread == null) {
            receiveDataThread = new Thread(new ReceiveDataRunnable(), "ReceiveDataThread");
            receiveDataThread.start();
        } else {
            receiveDataThread.interrupt();
            receiveDataThread = null;
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("Thread error!"));
        }
        onDeviceStatusListener.deviceStatusChanged(getDeviceInformation());
    }

    public void startCommandDaemonThread() {
        if (commandDaemonThread == null) {
            commandDaemonThread = new Thread(new ReceiveCommandRunnable(), "commandDaemonThread");
            commandDaemonThread.start();
        }
    }
    public void stopCommandDaemonThread() {
        if (commandDaemonThread != null) {
            commandDaemonThread.interrupt();
            commandDaemonThread = null;
        }
    }

    public String getDeviceInformation() {
        if (usbDevice != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return String.format("Manufacturer: %s%n ProductName: %s", usbDevice.getManufacturerName(), usbDevice.getProductName());
            }
        }
        return String.format("Manufacturer: %s%n ProductName: %s", "Unknown", "Unknown");
    }

    public void StartToGetVersion() {
        SendCommandToDevice((byte) 0xB1);
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
                                openTargetDevice(usbInterface);
                                if (deviceIsOpened()) {
                                    Log.d(TAG, "startToReceiveData()!");
                                    //startToReceiveData();
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
                if (!deviceIsOpened()) {
                    Log.d(TAG, "connectToDevice()!");
                    connectToDevice();
                } else {
                    //startToReceiveData();
                }
                onDeviceStatusListener.deviceStatusChanged(getDeviceInformation());
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
                stopCommandDaemonThread();
                onDeviceStatusListener.deviceStatusChanged(getDeviceInformation());
            }
        }
    };

}