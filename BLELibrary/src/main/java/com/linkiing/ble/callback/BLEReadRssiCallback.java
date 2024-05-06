package com.linkiing.ble.callback;

import com.linkiing.ble.BLEDevice;

import org.jetbrains.annotations.NotNull;

public interface BLEReadRssiCallback {

    /**
     * 读取设备信号强度回调
     * @param rssi rssi
     */
    void onReadRssiCallback(@NotNull BLEDevice devices, int rssi);
}
