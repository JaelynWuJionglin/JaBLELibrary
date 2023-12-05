package com.linkiing.ble.api;

/**
 * 读命令 装载类
 */
public class BLEReadDataFormat {
    private String servicesUUID = "";//命令操作的服务
    private String characteristicUUID = "";//命令操作的通道
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

    public int getSendCmdNumber() {
        return sendCmdNumber;
    }

    public void setSendCmdNumber(int sendCmdNumber) {
        this.sendCmdNumber = sendCmdNumber;
    }
}
