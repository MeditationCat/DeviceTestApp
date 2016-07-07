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
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeviceTestAppService extends Service {

    private static final String TAG = "DeviceTestAppService";

    private OnDataChangedListener onDataChangedListener;

    private OnDeviceStatusListener onDeviceStatusListener;

    private OnCommandResultListener onCommandResultListener;

    private DtaBinder dtaBinder;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private List<UsbInterface> usbInterfaceList;
    private UsbInterface usbInterface;
    private UsbEndpoint EPCTRL;
    private UsbEndpoint[] EPIN, EPOUT;
    private final int MaxEpNumber = 0x0E;
    private UsbDeviceConnection usbDeviceConnection;
    private final int DATA_RECV_TIMEOUT = 4; // ms
    private final int THREAD_SLEEP_TIME = 100; // ms
    private Thread receiveDataThread;
    private Thread commandDaemonThread;

    private PendingIntent pendingIntent;

    @Override
    public void onCreate() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
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
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
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
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }

    public void setOnDataChangedListener(OnDataChangedListener onDataChangedListener) {
        this.onDataChangedListener = onDataChangedListener;
    }

    public void setOnDeviceStatusListener(OnDeviceStatusListener onDeviceStatusListener) {
        this.onDeviceStatusListener = onDeviceStatusListener;
    }

    public void setOnCommandResultListener(OnCommandResultListener onCommandResultListener) {
        this.onCommandResultListener = onCommandResultListener;
    }

    /* usb device operation methods */
    private  void initConnection() {
        usbManager = null;
        usbDevice = null;
        usbInterfaceList.clear();
        usbInterface = null;

        EPCTRL = null;
        usbDeviceConnection = null;
    }

    private void enumerateDevice() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
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
                Log.d(TAG, String.format("Device matched:{pid=%d, vid=%d} name:%s",
                        device.getProductId(), device.getVendorId(), device.getDeviceName()));
                onDeviceStatusListener.deviceStatusChanged(getDeviceInformation());
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
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
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
                    Log.d(TAG, String.format("interface[%d]: %d", i, interfaceList.get(i).getId()));
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
                        Log.d(TAG,String.format("EP[%d] =%s",i, ep.toString()));
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

    public void connectToDevice() {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName());
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
                if (openTargetDevice(usbInterface)) {
                    startCommandDaemonThread();
                    //StartToCheckVersion();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean deviceIsOpened() {
        return (usbDeviceConnection != null && usbDeviceConnection.getSerial() != null);
    }

    public class ReceiveDataRunnable implements Runnable {

        private char[] packetHeader = new char[2];
        private int[] packetDataGyroscope = new int[3];
        private int[] packetDataAccelerometer = new int[3];
        private int[] packetDataMagnetic = new int[3];
        private int[] packetDataTemperature = new int[1];
        private int[] packetDataLight = new int[1];
        private int[] packetDataProximity = new int[1];
        private long[] packetDataTimestamp = new long[1];
        private int[] touchPadXY = new int[2];

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

            while (checkingSensorStatus) {
                synchronized (this) {
                    try {
                        retVal = usbDeviceConnection.bulkTransfer(EPIN[2], dataBuffer, dataBuffer.length, DATA_RECV_TIMEOUT);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, Thread.currentThread().getName() + "->usbDeviceConnection.bulkTransfer(EPIN[2]) failed!");
                        checkingSensorStatus = false;
                        receiveDataThread = null;
                    }
                    if (retVal > 0) {
                        timeStamp2 = System.currentTimeMillis();
                        if (timeStamp2 - timeStamp > THREAD_SLEEP_TIME + 8) {
                            Log.d(TAG, Thread.currentThread().getName() + String.format(Locale.US, "->%d ms", timeStamp2 - timeStamp));
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
                                (int) ((dataBuffer[offset] & 0xFF) << 8 | (dataBuffer[offset + 1] & 0xFF));
                        //packet data: proximity: short
                        offset = 24;
                        packetDataProximity[0] =
                                (int) ((dataBuffer[offset] & 0xFF) << 8 | (dataBuffer[offset + 1] & 0xFF));
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

                        if ((dataBuffer[0] & 0xFF) == 0xB4) {
                            touchPadXY[0] = dataBuffer[1] & 0xFF;
                            touchPadXY[1] = dataBuffer[2] & 0xFF;
                            sensorPackageObject.setTouchPadXY(touchPadXY);
                            //Log.d(TAG, String.format("xy:->%d, %d", touchPadXY[0], touchPadXY[1]));

                        }
                        if (dataBuffer[0] == 'M' && dataBuffer[1] == '5') {
                        }
                        onDataChangedListener.sensorDataChanged(sensorPackageObject);

                        if ((dataBuffer[0]&0xFF) == 0x45) {
                            checkingSensorStatus = false;
                            receiveDataThread = null;
                            Log.d(TAG, Thread.currentThread().getName() + "->stop sensor command 0x07 effected!");
                        }
                        try {
                            Thread.sleep(THREAD_SLEEP_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Log.d(TAG, Thread.currentThread().getName() + "->InterruptedException!");
                            checkingSensorStatus = false;
                            receiveDataThread = null;
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
                            Log.d(TAG, String.format("receiveBuffer[%d] = %#04x", i, dataBuffer[i]));
                        }
                        */
                    }
                }
            }
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
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->Device is not opened!");
            return retVal;
        }

        if (EPOUT[1] == null) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->EPIN[2] is not available!");
            return retVal;
        }

        if (cmdBuffer == null) {
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->cmdBuffer == null!");
            return retVal;
        }

        try {
            retVal = usbDeviceConnection.bulkTransfer(EPOUT[1], cmdBuffer, cmdBuffer.length, DATA_RECV_TIMEOUT);
            if (retVal > 0) {
                Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("->send cmd %#04x OK!", cmdBuffer[0] & 0xFF));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->usbDeviceConnection.bulkTransfer(EPIN[2]) failed!");
        }

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

            while (true) {
                synchronized (this) {
                    try {
                        retVal = usbDeviceConnection.bulkTransfer(EPIN[1], dataBuffer, dataBuffer.length, DATA_RECV_TIMEOUT);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, Thread.currentThread().getName() + "->usbDeviceConnection.bulkTransfer(EPIN[1]) failed!");
                        break;
                    }
                    if (retVal > 0) {
                        ProcessingCommandFeedback(dataBuffer, retVal);
                        try {
                            Thread.sleep(THREAD_SLEEP_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Log.d(TAG, Thread.currentThread().getName() + "->InterruptedException!");
                            break;
                        }
                    }
                }
            }
        }
    }

    private void ProcessingCommandFeedback(byte[] buffer, int retVal) {
        int cmd = buffer[0] & 0xFF;
        Log.d(TAG, Thread.currentThread().getName() + String.format(Locale.US, "->cmd:%#04x %nlength:%#x %ndata:%x,%x,%x,%x,%x,%x",
                buffer[0], buffer[1], buffer[2], buffer[3], buffer[4],buffer[5], buffer[6], buffer[7]));
        switch (cmd) {
            case 0xA5: // result for iap upgrade
            case 0x2C: //G sensor calibration return value;
            case 0xB2: //version number for BLE and CY7C63813
            case 0xB6: //write ble mac return value
            case 0xB8: //read ble mac return value
            case 0xBB: //result for serial number writing
            case 0xBD: //read serial number return value
                onCommandResultListener.commandResultChanged(cmd, buffer);
                break;
            // dfu upgrade return value;
            case 0xA0:
            case 0xA1:
            case 0xA2:
            case 0xA3:
            case 0xA4:
                DfuUpgradeCase(cmd, buffer);
                break;
            default:
                break;
        }
    }

    private void DfuUpgradeCase(int cmd, byte[] buffer) {
        String path;
        File binFile;
        FileInputStream inputStream = null;
        int fileSize = 0;
        try {
            path = Environment.getExternalStorageDirectory().getPath() + "/USBIAP.bin";
            Log.d(TAG, Thread.currentThread().getName() + String.format("->%s", path));
            binFile = new File(path);
            if (!binFile.exists()) {
                Log.d(TAG, Thread.currentThread().getName() + "->Failed to open file!");
                return;
            }
            inputStream = new FileInputStream(path);
            fileSize = (int) inputStream.getChannel().size();
            Log.d(TAG, Thread.currentThread().getName() + String.format(Locale.US, "->path = %s fileSize = %d", path, fileSize));
            if (fileSize == 0) {
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format("->cmd = %#04x", cmd));
        switch (cmd) {
            case 0xA1:
                byte[] cmdBuffer = new byte[6];
                cmdBuffer[0] = (byte) 0xA2;
                cmdBuffer[1] = 0x04;
                cmdBuffer[2] = (byte) ((fileSize >> 24) & 0xFF);
                cmdBuffer[3] = (byte) ((fileSize >> 16) & 0xFF);
                cmdBuffer[4] = (byte) ((fileSize >> 8) & 0xFF);
                cmdBuffer[5] = (byte) (fileSize & 0xFF);
                SendCommandToDevice(cmdBuffer);
                Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName()  + String.format(Locale.US, "->fileSize:%d", fileSize));
                break;
            case 0xA3:
                DfuUpgradeSendFile(inputStream);
                break;
            default:
                break;
        }
    }

    private void DfuUpgradeSendFile(InputStream inputStream) {
        int readBytes = 0;
        byte[] readBuffer = new byte[60];
        ByteBuffer sendBuffer = ByteBuffer.allocate(EPOUT[1].getMaxPacketSize());
        UsbRequest usbRequest = new UsbRequest();
        usbRequest.initialize(usbDeviceConnection, EPOUT[1]);

        try {
            if (inputStream.available() > 0) {
                int count = 0;
                while ((readBytes = inputStream.read(readBuffer)) != -1) {
                    sendBuffer.rewind();
                    sendBuffer.clear();
                    sendBuffer.put((byte) 0xA4);
                    sendBuffer.put((byte) readBytes);
                    sendBuffer.put(readBuffer);
                    if (usbRequest.queue(sendBuffer, sendBuffer.position() + 1)) {
                        count++;
                        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format(Locale.US, "->bytes:%d, count:%d", readBytes, count));
                    } else {
                        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + String.format(Locale.US, "->##LOST:bytes:%d, count:%d", readBytes, count));
                    }
                }
                usbRequest.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void StartToUpgrade() {
        SendCommandToDevice((byte) 0xA0);
}

    public void StartToCalibration() {
        SendCommandToDevice((byte) 0x2B);
    }

    public void StartToCheckVersion() {
        SendCommandToDevice((byte) 0xB1);
    }

    public void startToReceiveData() {
        SendCommandToDevice((byte) 0x0B);
    }

    public void stopToReceiveData() {
        SendCommandToDevice((byte) 0x07);
    }

    public void startToReceiveTouchPadXY() {
        SendCommandToDevice((byte) 0xB3);
    }

    public void StopToReceiveTouchPadXY() {
        SendCommandToDevice((byte) 0xB9);
    }

    public void StartToWriteMacAddress(byte[] address) {
        SendCommandToDevice((byte) 0xB5, address, (byte) address.length);
    }

    public void StartToReadMacAddress() {
        SendCommandToDevice((byte) 0xB7);
    }

    public void StartToWriteSerialNumber(byte[] sn) {
        SendCommandToDevice((byte) 0xBA, sn, (byte) sn.length);
    }

    public void StartToReadSerialNumber() {
        SendCommandToDevice((byte) 0xBC);
    }

    public void StartSensorSwitch() {
        if (!deviceIsOpened()) {
            connectToDevice();
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->deviceIsOpened == false!");
        }
        if (receiveDataThread == null) {
            startToReceiveData();
            checkingSensorStatus = true;
            receiveDataThread = new Thread(new ReceiveDataRunnable(), "ReceiveDataThread");
            receiveDataThread.start();
        } else {
            //stopToReceiveData();
            startToReceiveData();
        }
    }

    public void StartToReceiveTouchPadXY() {
        if (!deviceIsOpened()) {
            connectToDevice();
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->deviceIsOpened == false!");
        }
        if (receiveDataThread == null) {
            checkingSensorStatus = true;
            receiveDataThread = new Thread(new ReceiveDataRunnable(), "ReceiveDataThread");
            receiveDataThread.start();
        }
        startToReceiveTouchPadXY();
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
                return String.format("%s: %s%n%s: %s", getString(R.string.device_manufacturer), usbDevice.getManufacturerName(),
                        getString(R.string.device_productname), usbDevice.getProductName());
            }
        }
        return String.format("%s: %s%n%s: %s", getString(R.string.device_manufacturer), getString(R.string.device_unknown),
                getString(R.string.device_productname), getString(R.string.device_unknown));
    }

    private boolean checkingSensorStatus = true;
    private Thread checkDeviceStatusThread = null;
    private boolean checkingDeviceStatus = false;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "->" + action);

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "extra permission is granted for device" + device);
                        if(device != null){
                            //call method to set up device communication
                            try {
                                if (!deviceIsOpened()) {
                                    openTargetDevice(usbInterface);
                                    //Log.d(TAG, "startToReceiveData()!");
                                    //startToReceiveData();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //startCommandDaemonThread();
                            //StartToCheckVersion();
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                /*
                if (checkDeviceStatusThread == null) {
                    checkingDeviceStatus = true;
                    checkDeviceStatusThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (checkingDeviceStatus) {
                                connectToDevice();
                                if (usbDevice != null) {
                                    checkingDeviceStatus = false;
                                    Log.d(TAG, "deviceIsOpened!");
                                }
                                try {
                                    Thread.sleep(THREAD_SLEEP_TIME);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    checkDeviceStatusThread.start();
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            checkingDeviceStatus = false;
                        }
                    }, 3 * 1000);
                }*/
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

                checkDeviceStatusThread = null;
                checkingDeviceStatus = false;
                checkingSensorStatus = false;
                receiveDataThread = null;

                stopCommandDaemonThread();
                onDeviceStatusListener.deviceStatusChanged(getDeviceInformation());
            }
        }
    };
}