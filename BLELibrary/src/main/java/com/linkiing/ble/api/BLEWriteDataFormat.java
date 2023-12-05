package com.linkiing.ble.api;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 写命令 装载类
 */
public class BLEWriteDataFormat {
    private String servicesUUID = "";//命令操作的服务
    private String characteristicUUID = "";//命令操作的通道
    private byte[] bytes = new byte[0];//命令的数据
    private int writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;//命令写入方式
    private int sendCmdNumber = 3;//若命令执行失败，重试次数

    public String getServicesUUID() {
        return servicesUUID;
    }

    public void setServicesUUID(String servicesUUID) {
        this.servicesUUID = servicesUUID;
    }

    public String getCharacteristicUUID() {
        return characteristicUUID;
    }

    public void setCharacteristicUUID(String characteristicUUID) {
        this.characteristicUUID = characteristicUUID;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getWriteType() {
        return writeType;
    }

    public void setWriteType(int writeType) {
        this.writeType = writeType;
    }

    public int getSendCmdNumber() {
        return sendCmdNumber;
    }

    public void setSendCmdNumber(int sendCmdNumber) {
        this.sendCmdNumber = sendCmdNumber;
    }
}
