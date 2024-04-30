package com.linkiing.ble.callback;

import com.linkiing.ble.BLEDevice;

import org.jetbrains.annotations.NotNull;

public interface BLEScanDeviceCallback {

    /**
     * 扫描设备回调
     *
     * @param devices 设备
     */
    void onScanDevice(@NotNull BLEDevice devices);

    /**
     * 扫描设备结束
     */
    default void onScanStop() {

    }
}
