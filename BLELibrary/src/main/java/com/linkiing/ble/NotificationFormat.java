package com.linkiing.ble;

/**
 * 通知设置，数据类
 */
public class NotificationFormat {
    //服务uuid
    private String SERVICE = "";

    //通道uuid
    private String UUID = "";

    public NotificationFormat(String SERVICE,String UUID){
        this.SERVICE = SERVICE;
        this.UUID = UUID;
    }

    public String getSERVICE() {
        return SERVICE;
    }

    public void setSERVICE(String SERVICE) {
        this.SERVICE = SERVICE;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}
