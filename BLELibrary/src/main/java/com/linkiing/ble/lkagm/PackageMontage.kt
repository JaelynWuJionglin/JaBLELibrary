package com.linkiing.ble.lkagm

import com.linkiing.ble.BLEDevice
import com.linkiing.ble.log.LOGUtils
import com.linkiing.ble.utils.ByteUtils
import com.linkiing.ble.utils.CRC8
import java.io.ByteArrayOutputStream

/**
 * 拼包
 */
internal class PackageMontage constructor(
    private val agmMaxLength: Int = 512,
    private val vendorId: Int = 0xA0,
    private val startByte: Int = 0xDC,
    val bleDevice: BLEDevice,
    val notificationUUID: String,
    private val deviceNotificationCallback: LkAgmPackageCallback
) {
    private val dataStreamList: MutableList<ByteArrayOutputStream> = mutableListOf()
    private val dataStream = ByteArrayOutputStream()
    private val streamAll = ByteArrayOutputStream()
    private var packDataLen = 0
    private var isNext = true
    private var crcDevice = 0

    /**
     * onNotificationMontage
     */
    fun onNotificationMontage(device: BLEDevice, uuid: String, bytes: ByteArray) {
        if (uuid == notificationUUID && bleDevice.deviceMac == device.deviceMac) {
            //协议拼包
            if (bytes.size >= 6) {
                //包起始
                val dataInt = ByteUtils.byteArrayToIntArray(bytes)
                val ack = dataInt[2]
                if (dataInt[0] == vendorId
                    && dataInt[1] == startByte
                    && isAck(ack)
                ) {
                    packDataLen = ByteUtils.byteArrayToInt(byteArrayOf(bytes[3], bytes[4]))
                    if (packDataLen <= 0 || packDataLen > agmMaxLength) {
                        //包错误
                        packUpReset(4)
                        return
                    }

                    isNext = isNext(ack)
                    crcDevice = dataInt[5]

                    val remainLen = bytes.size - 6
                    if (remainLen > 0) {
                        val remainBytes = ByteArray(remainLen)
                        System.arraycopy(bytes, 6, remainBytes, 0, remainLen)
                        packUp(remainBytes, 0)
                    } else {
                        //只有协议头的数据无效
                        packUpReset(1)
                    }
                } else if (dataStream.size() < packDataLen && packDataLen > 0 && (bytes.size <= packDataLen - dataStream.size())) {
                    //拼包
                    packUp(bytes, 1)
                } else {
                    packUpReset(2)
                }
            } else if (dataStream.size() < packDataLen && packDataLen > 0 && (bytes.size <= packDataLen - dataStream.size())) {
                //拼包
                packUp(bytes, 2)
            } else {
                //错误的数据包
                dataStreamList.clear()
                packUpReset(3)
            }
        }
    }

    /**
     * 是否有下一包
     */
    private fun isNext(ack: Int): Boolean {
        return ack and 1 == 1
    }

    /**
     * 是否是设备发送到App的包
     */
    private fun isAck(ack: Int): Boolean {
        return ack and 2 == 2
    }

    //packUp
    private fun packUp(bytes: ByteArray, packUpCode: Int) {
        LOGUtils.d(
            "packUp() ==> packUpCode:$packUpCode  bytes:${
                ByteUtils.toHexString(
                    bytes,
                    ","
                )
            }"
        )
        val rLen = packDataLen - dataStream.size()
        if (rLen > 0) {
            //还未接收完数据，继续添加
            val dataBytes: ByteArray
            if (bytes.size >= rLen) {
                dataBytes = ByteArray(rLen)
                System.arraycopy(bytes, 0, dataBytes, 0, rLen)
            } else {
                dataBytes = bytes
            }
            dataStream.write(dataBytes)
        }

        if (dataStream.size() == packDataLen) {
            //接收数据完成
            if (crcDevice == CRC8.getCrc8(dataStream.toByteArray())) {
                //CRC通过
                dataStreamList.add(dataStream)
                if (!isNext) {
                    if (dataStreamList.isNotEmpty()) {
                        streamAll.reset()
                        for (stream in dataStreamList) {
                            streamAll.write(stream.toByteArray())
                        }
                        deviceNotificationCallback.onAgmPackageData(
                            bleDevice,
                            notificationUUID,
                            streamAll.toByteArray()
                        )
                    } else {
                        packUpReset(5)
                    }
                    dataStreamList.clear()
                }
                packUpReset(0)
            } else {
                //CRC失败
                dataStreamList.clear()
                packUpReset(6)
            }
        }
    }

    private fun packUpReset(resetCode: Int) {
        dataStream.reset()
        isNext = true
        packDataLen = 0
        crcDevice = 0
        LOGUtils.v("packUpReset() ==> resetCode:$resetCode")
    }
}