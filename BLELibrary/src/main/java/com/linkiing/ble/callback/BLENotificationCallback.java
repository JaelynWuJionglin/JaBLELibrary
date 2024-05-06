package com.linkiing.ble.callback;


import com.linkiing.ble.BLEDevice;

import org.jetbrains.annotations.NotNull;

/**
 * 通知
 */
public interface BLENotificationCallback {

    /**
     * 通知
     * @param bleDevice 设备
     * @param uuid 通道uuid
     * @param bytes 数据
     */
    void onNotificationCallback(@NotNull BLEDevice bleDevice, @NotNull String uuid, @NotNull byte[] bytes);
}
