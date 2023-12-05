package com.linkiing.ble.callback;

import com.linkiing.ble.BLEDevice;

/**
 * 读回应
 */
public interface BLEReadCallback {

    /**
     * 读数据
     * @param bleDevice 设备
     * @param uuid 通道uuid
     * @param bytes 数据
     */
    void onReadCallback(BLEDevice bleDevice, String uuid, byte[] bytes);
}
