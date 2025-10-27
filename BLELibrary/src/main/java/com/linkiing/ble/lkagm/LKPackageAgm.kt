package com.linkiing.ble.lkagm

import com.linkiing.ble.BLEDevice
import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.api.BLEWriteDataFormat
import com.linkiing.ble.callback.BLENotificationCallback
import com.linkiing.ble.log.LOGUtils
import com.linkiing.ble.utils.ByteUtils
import com.linkiing.ble.utils.CRC8
import kotlin.math.max

/**
 * LK BLE数据包协议
 */
class LKPackageAgm private constructor() : BLENotificationCallback {
    private var deviceNotificationListenerList = arrayListOf<PackageMontage>()
    private var agmMaxLength = 512 //协议单帧最大长度
    private var vendorId = 0xA0 //协议帧头
    private var startByte = 0xDC

    companion object {

        @JvmStatic
        val instance: LKPackageAgm by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            LKPackageAgm()
        }
    }

    init {
        BLEManager.getInstance().addNotificationCallback(this)
    }

    /**
     * 协议单帧最大长度
     */
    fun setAgmMaxLength(agmMaxLength: Int): LKPackageAgm {
        this.agmMaxLength = max(agmMaxLength, 0)
        return this
    }

    /**
     * 协议帧头
     */
    fun setVendorIdAndStartByte(vendorId: Int, startByte: Int): LKPackageAgm {
        this.vendorId = vendorId
        this.startByte = startByte
        return this
    }

    /**
     * 添加deviceNotification拼包结果监听
     */
    fun addDeviceNotificationListener(
        bleDevice: BLEDevice,
        notificationUUID: String,
        deviceNotificationCallback: LkAgmPackageCallback
    ) {
        if (!haveListener(bleDevice, notificationUUID)) {
            val packageMontage = PackageMontage(
                agmMaxLength,
                vendorId,
                startByte,
                bleDevice,
                notificationUUID,
                deviceNotificationCallback
            )
            deviceNotificationListenerList.add(packageMontage)
        }
    }

    /**
     * 移除deviceNotification拼包结果监听
     */
    fun removeDeviceNotificationListener(bleDevice: BLEDevice, notificationUUID: String) {
        val iterator = deviceNotificationListenerList.iterator()
        while (iterator.hasNext()) {
            val listener = iterator.next()
            if (listener.bleDevice.deviceMac == bleDevice.deviceMac && notificationUUID == listener.notificationUUID) {
                iterator.remove()
            }
        }
    }

    private fun haveListener(bleDevice: BLEDevice, notificationUUID: String): Boolean {
        for (listener in deviceNotificationListenerList) {
            if (listener.bleDevice.deviceMac == bleDevice.deviceMac && notificationUUID == listener.notificationUUID) {
                return true
            }
        }
        return false
    }

    /**
     * 发送命令
     */
    fun sendCmdBytes(
        serviceUUID: String,
        characteristicUUID: String,
        cmd: Int,
        bytes: ByteArray
    ): Boolean {
        if (bytes.size <= agmMaxLength) {
            return sendData(serviceUUID, characteristicUUID, false, cmd, bytes)
        } else {
            for (size in bytes.indices step agmMaxLength) {
                var len: Int = agmMaxLength
                var next = true
                if (size + agmMaxLength >= bytes.size) {
                    len = bytes.size - size
                    next = false
                }
                val sizeData = ByteArray(len)
                System.arraycopy(bytes, size, sizeData, 0, sizeData.size)
                if (!sendData(serviceUUID, characteristicUUID, next, cmd, sizeData)) {
                    return false
                }
            }
            return true
        }
    }

    private fun sendData(
        serviceUUID: String,
        characteristicUUID: String,
        next: Boolean,
        cmd: Int,
        bytes: ByteArray
    ): Boolean {
        val len = bytes.size + 1
        val data = ByteArray(len)
        data[0] = cmd.toByte()
        System.arraycopy(bytes, 0, data, 1, bytes.size)

        val crc = CRC8.getCrc8(data)

        val sendData = ByteArray(len + 6)
        sendData[0] = vendorId.toByte()
        sendData[1] = startByte.toByte()
        sendData[2] = if (next) {
            1
        } else {
            0
        }
        sendData[3] = ((len shr 8) and 0xFF).toByte()
        sendData[4] = (len and 0xFF).toByte()
        sendData[5] = crc.toByte()
        System.arraycopy(data, 0, sendData, 6, data.size)

        val writeDataFormat = BLEWriteDataFormat()
        writeDataFormat.servicesUUID = serviceUUID
        writeDataFormat.characteristicUUID = characteristicUUID
        writeDataFormat.bytes = sendData
        if (BLEManager.getInstance().sendDataToConnect(writeDataFormat)) {
            LOGUtils.v("BLE SEND ==> bytes:${ByteUtils.toHexString(sendData, ",")}")
            return true
        }
        return false
    }


    /**
     * onNotificationCallback
     */
    override fun onNotificationCallback(bleDevice: BLEDevice, uuid: String, bytes: ByteArray) {
        LOGUtils.i("BLE NOT ==> uuid:$uuid bytes:${ByteUtils.toHexString(bytes, ",")}")

        for (listener in deviceNotificationListenerList) {
            listener.onNotificationMontage(bleDevice, uuid, bytes)
        }
    }
}