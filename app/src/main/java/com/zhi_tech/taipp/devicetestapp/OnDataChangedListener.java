package com.zhi_tech.taipp.devicetestapp;

/**
 * Created by taipp on 5/26/2016.
 */
public interface OnDataChangedListener {

    void sensorDataChanged(SensorPackageObject object);
    void sendsorCommandReturnValue(int cmd, int value);

}
