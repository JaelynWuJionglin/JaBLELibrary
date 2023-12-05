package com.linkiing.ble.lkagm

import com.linkiing.ble.BLEDevice

interface LkAgmPackageCallback {

    /**
     * LK标准协议拼包后数据返回
     */
    fun onAgmPackageData(bleDevice: BLEDevice, notificationUUID: String, bytes: ByteArray)
}