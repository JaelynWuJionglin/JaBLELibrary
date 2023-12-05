package com.linkiing.ble.callback;

public interface BLEScannerFilterCallback {

    /**
     * 自定义过滤
     * @param deviceName 蓝牙名
     * @param scanRecord 广播包
     * @param rssi 信号强度
     * @return true:需要搜索的设备   false:过滤的设备
     */
    boolean isFilter(String deviceName, byte[] scanRecord, int rssi);

}
