package com.linkiing.test.tool.ble

import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.api.BLEWriteDataFormat
import com.linkiing.ble.log.LOGUtils
import com.linkiing.ble.utils.ByteUtils

/**
 * 蓝牙协议类
 */
class BleAgreement private constructor() {

    /**
     * 单利模式(双重校验锁式)
     */
    companion object {
        val instance: BleAgreement by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            BleAgreement()
        }
    }

    /**
     * Mesh重置
     */
    fun sendResetMesh(): Boolean {
        val writeDataFormat = BLEWriteDataFormat()
        writeDataFormat.servicesUUID = BleConstant.SERVICE_RESET_MESH
        writeDataFormat.characteristicUUID = BleConstant.SEND_UUID_RESET_MESH
        writeDataFormat.bytes = byteArrayOf(0x44, 0x56, 0x52, 0x45, 0x53, 0x45, 0x54)
        val result = BLEManager.getInstance().sendDataToConnect(writeDataFormat)
        LOGUtils.v("BLE sendResetMesh ==> result:$result")
        return result
    }

    /**
     * 修改广播间隔
     */
    fun modifyAdvInv(minInv: Int, maxInv: Int): Boolean {
        val minBytes = ByteUtils.intToByteArray(minInv)
        val maxBytes = ByteUtils.intToByteArray(maxInv)
        val sendBytes = byteArrayOf(
            minBytes[3],minBytes[2],minBytes[1],minBytes[0],
            maxBytes[3],maxBytes[2],maxBytes[1],maxBytes[0]
        )
        return sendBytesTest1(1,sendBytes)
    }

    /**
     * 修改连接间隔
     */
    fun modifyConInv(minInv: Int, maxInv: Int): Boolean {
        val minBytes = ByteUtils.intToByteArray(minInv)
        val maxBytes = ByteUtils.intToByteArray(maxInv)
        val sendBytes = byteArrayOf(
            minBytes[3],minBytes[2],
            maxBytes[3],maxBytes[2]
        )
        return sendBytesTest1(2,sendBytes)
    }

    /**
     * 修改广播名称
     */
    fun modifyAdvName(nameStr: String): Boolean {
        val nameBytes = nameStr.toByteArray(Charsets.UTF_8)
        val sendBytes = if (nameBytes.size <= 10) {
            nameBytes
        } else {
            nameBytes.slice(0 until 10).toByteArray()
        }
        return sendBytesTest1(3,sendBytes)
    }

    /**
     * 修改PID
     */
    fun modifyPid(pidStr: String): Boolean {
        val pidBytes = pidStr.toByteArray(Charsets.UTF_8)
        val sendBytes = if (pidBytes.size <= 4) {
            val bytes = ByteArray(4)
            System.arraycopy(pidBytes,0,bytes,0,pidBytes.size)
            bytes
        } else {
            pidBytes.slice(0 until 4).toByteArray()
        }
        val reversedSendBytes = sendBytes.reversedArray()
        return sendBytesTest1(4,reversedSendBytes)
    }

    //sendBytesTest1
    private fun sendBytesTest1(cmd: Int,dataBytes: ByteArray): Boolean {
        val startBytes = byteArrayOf(
            0xFF.toByte(),
            0xFE.toByte(),
            ((cmd shr 8) and 0xFF).toByte(),
            (cmd and 0xFF).toByte()
        )
        val sendData = ByteArray(startBytes.size + dataBytes.size + 1)
        System.arraycopy(startBytes,0,sendData,0,startBytes.size)
        System.arraycopy(dataBytes,0,sendData,startBytes.size,dataBytes.size)
        sendData[startBytes.size + dataBytes.size] = 0xFE.toByte()

        val writeDataFormat = BLEWriteDataFormat()
        writeDataFormat.servicesUUID = BleConstant.SERVICE_TEST1
        writeDataFormat.characteristicUUID = BleConstant.SEND_UUID_TEST1
        writeDataFormat.bytes = sendData
        return if (BLEManager.getInstance().sendDataToConnect(writeDataFormat)) {
            LOGUtils.v("BLE sendBytesTest1 ==> bytes:${ByteUtils.toHexString(sendData, ",")}")
            true
        } else {
            false
        }
    }
}