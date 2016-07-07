package com.zhi_tech.devicetestapp;

public interface OnCommandResultListener {
    void commandResultChanged(final int cmd, final byte[] buffer);
}
