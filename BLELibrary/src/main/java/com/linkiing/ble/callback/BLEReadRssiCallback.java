package com.linkiing.ble.callback;

public interface BLEReadRssiCallback {

    /**
     * 读取设备信号强度回调
     * @param rssi rssi
     */
    void onReadRssiCallback(int rssi);
}
