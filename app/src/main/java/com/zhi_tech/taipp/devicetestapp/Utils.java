package com.zhi_tech.taipp.devicetestapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created by taipp on 5/20/2016.
 */
public class Utils {
    private static final String TAG = "DeviceTestApp";
    //private static Phone phone = null;
    public static void SetPreferences(Context context, SharedPreferences sp, int name, String flag) {
        String nameStr = context.getResources().getString(name);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(nameStr, flag);
        editor.commit();
        /*liuyang add begin@20130108*/
        if (context.getResources().getBoolean(R.bool.config_backup_show_allreport)){
            SetResults(name, flag);
        }
        /*liuyang add end@20130108*/
    }

    /*liuyang add begin@20130108*/
    private static void SetResults(int name, String flag) {
        int i = 0;
        Log.v(TAG,"SetResults-name="+name+"flag="+flag);
        for (i = 0; i < DeviceTestApp.itemString.length; i++) {
            if(DeviceTestApp.itemString[i] == name){
                Log.v(TAG,"SetResult-i="+i);
                if (AppDefine.DT_SUCCESS.equals(flag)){
                    //Log.v(TAG,"SetResult-flag=SUCCESS---"+i);
                    DeviceTestApp.result[i] = AppDefine.DVT_OK;
                }
                else if (AppDefine.DT_FAILED.equals(flag)){
                    //Log.v(TAG,"SetResult-flag=FAILED---"+i);
                    DeviceTestApp.result[i] = AppDefine.DVT_FAIL;
                }
                else if (AppDefine.DT_DEFAULT.equals(flag)){
                    //Log.v(TAG,"SetResult-flag=DEFAULT---"+i);
                    DeviceTestApp.result[i] = AppDefine.DVT_DEFAULT;
                }
                else {
                    //Log.v(TAG,"SetResult-flag=0");
                    DeviceTestApp.result[i] = AppDefine.DVT_DEFAULT;
                }
                //WriteData(i);
                break;
            }
        }

        WriteData();
    }


    //Write NvRAM
    private static void WriteData(/*int index*/) {
        int ret = 0;
        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);

        try {
            //AP_CFG_CUSTOM_FILE_CUSTOM_DVT_LID 30,AP_CFG_REEB_PRODUCT_INFO_LID is 25
            int flag = agent.writeFile(DeviceTestApp.AP_CFG_CUSTOM_FILE_CUSTOM_LID, DeviceTestApp.result);
            Log.v(TAG,"write success flag="+flag);

        } catch (RemoteException e) {
            e.printStackTrace();
            Log.v(TAG,"write failed"+e);
        }
    }

    //backup NvRAM
    public static void NvRAMBackupData() {
        int ret = 0;
        IBinder binder = ServiceManager.getService("NvRAMBackupAgent");

        if(binder != null){
            NvRAMBackupAgent agent = NvRAMBackupAgent.Stub.asInterface(binder);
            try {
                ret = agent.testBackup();
                Log.v(TAG, "NvRAMBackupAgent succuss");
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.v(TAG, "NvRAMBackupAgent fail" + e);
            }
        }
        else
        {
            Log.v(TAG, "NvRAMBackupAgent service = "+binder);
        }
    }


    private static boolean checkIsAllTestOK(){
        boolean isAllTestOK = true;

        for (int i = 0; i < DeviceTestApp.itemString.length; i++) {
            boolean flag = false;

            for (int j = 0; j < DeviceTestApp.excludeIds.size(); j++) {
                if(DeviceTestApp.itemString[i] == DeviceTestApp.excludeIds.get(j).intValue()){
                    Log.v(TAG,"checkIsAllTestOK--DeviceTestApp.excludeIds-j="+j);
                    flag = true;
                    break;
                }
            }
            if (flag) {
                //Log.v(TAG,"DeviceTestApp.excludeIds--continue");
                continue;
            }

            if (DeviceTestApp.result[i] != AppDefine.DVT_OK) {
                Log.v(TAG,"checkIsAllTestOK--isAllTestOK = false--i="+i);
                isAllTestOK = false;
                break;
            }
        }

        Log.v(TAG,"WriteIsAllTestOK-isAllTestOK="+isAllTestOK);


        return isAllTestOK;
    }

    //Write NvRAM
    public static void WriteIsAllTestOK() {
        int ret = 0;
        Log.v(TAG,"WriteIsAllTestOK-DeviceTestApp.result.length="+DeviceTestApp.result.length);
        if (checkIsAllTestOK()) {
            Log.v(TAG,"WriteIsAllTestOK-checkIsAllTestOK()=true");
            DeviceTestApp.result[DeviceTestApp.result.length-1] = AppDefine.DVT_OK;

        } else {
            Log.v(TAG,"WriteIsAllTestOK-checkIsAllTestOK()=false");
            DeviceTestApp.result[DeviceTestApp.result.length-1] = AppDefine.DVT_FAIL;
        }

        IBinder binder = ServiceManager.getService("NvRAMAgent");
        NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);

        try {
            //AP_CFG_CUSTOM_FILE_CUSTOM_DVT_LID 30,AP_CFG_REEB_PRODUCT_INFO_LID is 25
            int flag = agent.writeFile(DeviceTestApp.AP_CFG_CUSTOM_FILE_CUSTOM_LID, DeviceTestApp.result);
            Log.v(TAG,"WriteIsAllTestOK-write success flag="+flag);

        } catch (RemoteException e) {
            e.printStackTrace();
            Log.v(TAG,"WriteIsAllTestOK-write failed"+e);
        }
    }

    /*liuyang add begin@20130108*/
}
