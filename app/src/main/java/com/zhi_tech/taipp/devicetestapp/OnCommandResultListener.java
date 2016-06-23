package com.zhi_tech.taipp.devicetestapp;

public interface OnCommandResultListener {
    void commandResultChanged(final int cmd, final byte[] buffer);
}
