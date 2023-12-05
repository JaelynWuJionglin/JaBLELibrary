package com.linkiing.ble;


import android.bluetooth.BluetoothGattCharacteristic;

import com.linkiing.ble.api.BLENotifyDataFormat;
import com.linkiing.ble.api.BLEReadDataFormat;
import com.linkiing.ble.api.BLEWriteDataFormat;
import com.linkiing.ble.utils.BLEConstant;

/**
 * Created by Jaelyn on 2017/8/7.
 * 命令发送方式集合
 */
 class CommandFormat {
    private String servicesUUID = "";//命令操作的服务
    private String characteristicUUID = "";//命令操作的通道
    private String commandType = "";//命令操作类型
    private byte[] bytes = new byte[0];//命令的数据
    private int WRITE_TYPE = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;//命令写入方式
    private int sendCmdNumber;//若命令执行失败，重试次数

    public CommandFormat(CommandFormat commandFormat) {
        this.servicesUUID = commandFormat.getServicesUUID();
        this.characteristicUUID = commandFormat.getCharacteristicUUID();
        this.commandType = commandFormat.getCommandType();
        this.WRITE_TYPE = commandFormat.getWRITE_TYPE();
        this.sendCmdNumber = commandFormat.getSendCmdNumber();
    }

    public CommandFormat(BLEWriteDataFormat writeDataFormat){
        this.servicesUUID = writeDataFormat.getServicesUUID();
        this.characteristicUUID = writeDataFormat.getCharacteristicUUID();
        this.commandType = BLEConstant.Command_Type_write;
        this.WRITE_TYPE = writeDataFormat.getWriteType();
        this.sendCmdNumber = writeDataFormat.getSendCmdNumber();
        this.bytes = writeDataFormat.getBytes();
    }

    public CommandFormat(BLEReadDataFormat readDataFormat){
        this.servicesUUID = readDataFormat.getServicesUUID();
        this.characteristicUUID = readDataFormat.getCharacteristicUUID();
        this.commandType = BLEConstant.Command_Type_read;
        this.sendCmdNumber = readDataFormat.getSendCmdNumber();
    }

    public CommandFormat(BLENotifyDataFormat notifyDataFormat){
        this.servicesUUID = notifyDataFormat.getServicesUUID();
        this.characteristicUUID = notifyDataFormat.getCharacteristicUUID();
        if (notifyDataFormat.isEnable()) {
            this.commandType = BLEConstant.Command_Type_enNotify;
        } else {
            this.commandType = BLEConstant.Command_Type_disNotify;
        }
        this.sendCmdNumber = notifyDataFormat.getSendCmdNumber();
    }

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

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getWRITE_TYPE() {
        return WRITE_TYPE;
    }

    public void setWRITE_TYPE(int WRITE_TYPE) {
        this.WRITE_TYPE = WRITE_TYPE;
    }

    public int getSendCmdNumber() {
        return sendCmdNumber;
    }

    public void reduceCmdNumber() {
        this.sendCmdNumber--;
        if (this.sendCmdNumber < 0) {
            this.sendCmdNumber = 0;
        }
    }
}
