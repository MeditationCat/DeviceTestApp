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
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DeviceTestAppService extends Service {

    private static final String TAG = "DeviceTestAppService";
    private static byte result[] = new byte[AppDefine.DVT_NV_ARRAR_LEN];
    private static final long delaytime = 5000;

    private Handler mHandler = new Handler();

    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.d(TAG, "mHandler---mRunnable!---");
            //readDataFromNvram();
            //checkDVTResult();
            mHandler.postDelayed(this, delaytime);
        }
    };

    private DtaBinder dtaBinder = null;

    private final int vendorID = 48899;
    private final int productID = 48898;
    private UsbManager usbManager = null;
    private UsbDevice usbDevice = null;
    private List<UsbInterface> usbInterfaceList = null;
    private UsbEndpoint epControl = null;
    private UsbEndpoint epBulkOut = null;
    private UsbEndpoint epBulkIn = null;
    private UsbEndpoint epIntOut = null;
    private UsbEndpoint epIntIn = null;
    private UsbDeviceConnection usbDeviceConnection = null;
    private PendingIntent pendingIntent = null;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

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

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        enumerateDevice();
        getDeviceInterface();
        assignEndpoint(usbInterfaceList.get(0));
        openDevice(usbInterfaceList.get(0));


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
        super.onDestroy();
    }

    // 枚举设备函数
    private void enumerateDevice() {
        if (usbManager != null) {
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

            if (!(deviceList.isEmpty())) {
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

        } else {
            Log.d(TAG, "usbManager = null!");
        }
    }

    private void getDeviceInterface() {
        if (usbDevice != null) {
            Log.d(TAG, "usbDevice.getInterfaceCount() : " + usbDevice.getInterfaceCount());

            for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                UsbInterface intf = usbDevice.getInterface(i);
                usbInterfaceList.add(intf);
            }
        } else {
            Log.d(TAG, "usbDevice = null!");
        }
    }

    private void assignEndpoint(UsbInterface mInterface) {

        for (int i = 0; i < mInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = mInterface.getEndpoint(i);
            Log.d(TAG,"ep[" + i + "] = " + ep.toString());
            // look for bulk endpoint
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epBulkOut = ep;
                    Log.d(TAG,"epBulkOut info->addr:" + ep.getAddress() + " epNumber:" + ep.getEndpointNumber());
                } else {
                    epBulkIn = ep;
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

        if (epBulkOut == null && epBulkIn == null && epControl == null
                && epIntOut == null && epIntIn == null) {
            Log.d(TAG,"No endpoint is available!");
            throw new IllegalArgumentException("No endpoint is available!");
        }
    }

    public void openDevice(UsbInterface mInterface) {
        if (mInterface != null) {
            UsbDeviceConnection conn = null;
            // 在open前判断是否有连接权限；对于连接权限可以静态分配，也可以动态分配权限
            if (usbManager.hasPermission(usbDevice)) {
                conn = usbManager.openDevice(usbDevice);
            } else {
                usbManager.requestPermission(usbDevice,pendingIntent);
            }

            if (conn != null) {
                if (conn.claimInterface(mInterface, true)) {
                    usbDeviceConnection = conn;
                    if (usbDeviceConnection != null)// 到此你的android设备已经连上zigbee设备
                        System.out.println("open设备成功！");
                    final String mySerial = usbDeviceConnection.getSerial();
                    System.out.println("设备serial number：" + mySerial);
                } else {
                    System.out.println("无法打开连接通道。");
                    conn.close();
                }
            } else {
                Log.d(TAG, "conn = null!");
            }
        }
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    // call your method that cleans up and closes communication with the device
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
