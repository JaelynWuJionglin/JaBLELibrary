package com.linkiing.ble.api;

/**
 * BLEConfig
 */
public class BLEConfig {

    private BLEConfig() {}

    /**
     * 允许蓝牙连接设备数量上限
     * (最小可连接数量1，最大可连接数量8)
     **/
    public int BLE_CONNECT_DEVICE_MAX_NUMBER = 1;

    /**
     * 获取BLEConfig
     *
     * @param connectMaxNumber 允许蓝牙连接设备数量上限
     * @return BLEConfig
     */
    public static BLEConfig getBLEConfig(int connectMaxNumber) {
        BLEConfig config = new BLEConfig();
        config.BLE_CONNECT_DEVICE_MAX_NUMBER = connectMaxNumber;
        return config;
    }
}
