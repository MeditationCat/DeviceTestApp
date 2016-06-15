package com.zhi_tech.taipp.devicetestapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

/**
 * Created by taipp on 5/20/2016.
 */
public class Utils {

    private static final String TAG = "Utils";

    public static void SetPreferences(Context context, SharedPreferences sp, int name, String flag) {
        String nameStr = context.getResources().getString(name);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(nameStr, flag);
        editor.apply();
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
                break;
            }
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
    }

    //ViewHolder tools
    static class ViewHolder {
        public static <T extends View> T get(View view, int id) {
            SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();
            if (viewHolder == null) {
                viewHolder = new SparseArray<View>();
                view.setTag(viewHolder);
            }
            View childView = viewHolder.get(id);
            if (childView == null) {
                childView = view.findViewById(id);
                viewHolder.put(id, childView);
            }
            return (T) childView;
        }
    }
}
