package com.linkiing.test.tool.ble

import com.linkiing.ble.BLEDevice
import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.lkagm.LKPackageAgm
import com.linkiing.ble.lkagm.LkAgmPackageCallback

/**
 * 蓝牙协议类
 */
class BluAgreement private constructor() : LkAgmPackageCallback {

    /**
     * 单利模式(双重校验锁式)
     */
    companion object {
        val instance: BluAgreement by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            BluAgreement()
        }
    }

    private fun sendCmdBytes(cmd: Int,sendBytes: ByteArray): Boolean {
        return LKPackageAgm.instance.sendCmdBytes(
            BleConstant.SERVICE,
            BleConstant.SEND_UUID,
            cmd,
            sendBytes
        )
    }

    override fun onAgmPackageData(
        bleDevice: BLEDevice,
        notificationUUID: String,
        bytes: ByteArray
    ) {
        //Notification

    }
}