package com.zhi_tech.taipp.devicetestapp;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taipp on 5/20/2016.
 */
public class Report extends Activity {

    private SharedPreferences mSp;
    private TextView mSuccess;
    private TextView mFailed;
    private TextView mDefault;
    private List<String> mOkList;
    private List<String> mFailedList;
    private List<String> mDefaultList;

    final int itemString[] = {
            R.string.KeyCode_name,
            R.string.gsensor_name,
            R.string.msensor_name,
            R.string.lsensor_name,
            R.string.psensor_name,
            R.string.gyroscopesensor_name,
            R.string.bluetooth_name
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report);
        mSp = getSharedPreferences("DeviceTestApp", Context.MODE_PRIVATE);
        mSuccess = (TextView) findViewById(R.id.report_success);
        mFailed = (TextView) findViewById(R.id.report_failed);
        mDefault = (TextView) findViewById(R.id.report_default);
        mOkList = new ArrayList<String>();
        mFailedList = new ArrayList<String>();
        mDefaultList = new ArrayList<String>();

        for (int itemId : itemString) {
            if((mSp.getString(getString(itemId), null) == null)){
                continue;
            }
            if (AppDefine.DT_SUCCESS.equals(mSp.getString(getString(itemId), null))) {
                mOkList.add(getString(itemId));
            } else if (AppDefine.DT_FAILED.equals(mSp.getString(getString(itemId), null))) {
                mFailedList.add(getString(itemId));
            } else {
                mDefaultList.add(getString(itemId));
            }
        }
        ShowInfo();
    }

    protected void ShowInfo() {
        String okItem = "\n" + getString(R.string.report_ok) + "\n";
        for (int i = 0; i < mOkList.size(); i++) {
            okItem += mOkList.get(i) + " | ";
        }

        mSuccess.setText(okItem);

        String failedItem = "\n" + getString(R.string.report_failed) + "\n";
        for (int j = 0; j < mFailedList.size(); j++) {
            failedItem += mFailedList.get(j) + " | ";
        }
        mFailed.setText(failedItem);

        String defaultItem = "\n" + getString(R.string.report_notest) + "\n";
        for (int k = 0; k < mDefaultList.size(); k++) {
            defaultItem += mDefaultList.get(k) + " | ";
        }
        mDefault.setText(defaultItem);
    }
}
