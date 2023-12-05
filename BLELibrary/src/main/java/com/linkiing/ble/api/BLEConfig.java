package com.linkiing.ble.api;

/**
 * BLEConfig
 */
public class BLEConfig {

    private BLEConfig() {}

    public static final long MIN_CONNECT_OUT_TIME = 15 * 1000;

    /**
     * 允许蓝牙连接设备数量上限
     * (最小可连接数量1，最大可连接数量8)
     **/
    public int BLE_CONNECT_DEVICE_MAX_NUMBER = 1;

    /**
     * 蓝牙连接超时时间
     * ( >= 15s )
     **/
    public long CONNECT_OVER_TIME = 30 * 1000;

    /**
     * 获取BLEConfig
     *
     * @param connectMaxNumber 允许蓝牙连接设备数量上限
     * @param overTime         连接超时时间
     * @return BLEConfig
     */
    public static BLEConfig getBLEConfig(int connectMaxNumber, long overTime) {
        BLEConfig config = new BLEConfig();
        if (overTime >= MIN_CONNECT_OUT_TIME) {
            config.BLE_CONNECT_DEVICE_MAX_NUMBER = connectMaxNumber;
            config.CONNECT_OVER_TIME = overTime;
        }
        return config;
    }
}
