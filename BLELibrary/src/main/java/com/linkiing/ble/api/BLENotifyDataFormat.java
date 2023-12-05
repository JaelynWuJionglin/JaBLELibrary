package com.linkiing.ble.api;

/**
 * 写命令 装载类
 */
public class BLENotifyDataFormat {
    private String servicesUUID = "";//命令操作的服务
    private String characteristicUUID = "";//命令操作的通道
    private boolean isEnable = true;//打开还是关闭
    private int sendCmdNumber = 1;//若命令执行失败，重试次数

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

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    public int getSendCmdNumber() {
        return sendCmdNumber;
    }

    public void setSendCmdNumber(int sendCmdNumber) {
        this.sendCmdNumber = sendCmdNumber;
    }
}
