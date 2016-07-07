package com.zhi_tech.devicetestapp;

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

        if (context.getResources().getBoolean(R.bool.config_backup_show_allreport)){
            SetResults(name, flag);
        }
    }

    private static void SetResults(int name, String flag) {
        for (int i = 0; i < DeviceTestApp.itemIds.size(); i++) {
            if(DeviceTestApp.itemIds.get(i) == name){
                if (AppDefine.DT_SUCCESS.equals(flag)){
                    DeviceTestApp.result[i] = AppDefine.DVT_OK;
                } else if (AppDefine.DT_FAILED.equals(flag)){
                    DeviceTestApp.result[i] = AppDefine.DVT_FAIL;
                } else if (AppDefine.DT_DEFAULT.equals(flag)){
                    DeviceTestApp.result[i] = AppDefine.DVT_DEFAULT;
                } else {
                    DeviceTestApp.result[i] = AppDefine.DVT_DEFAULT;
                }
                break;
            }
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
