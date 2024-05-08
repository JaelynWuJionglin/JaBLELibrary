package com.linkiing.ble.utils;

import java.util.Arrays;

public class BLEConstant {
    /**
     * 蓝牙设备连接状态返回码
     */
    public final static int BLE_STATUS_CONNECTED = 1;// 设备连接成功
    public final static int BLE_STATUS_DISCONNECTED = 2;// 设备连接断开
    public final static int BLE_STATUS_CONNECT_TIME_OUT = 3;// 设备连接超时
    public final static int BLE_STATUS_CONNECT_FAIL = 4;// 设备连接失败
    public final static int BLE_STATUS_STATE_ON = 5;//蓝牙打开
    public final static int BLE_STATUS_STATE_OFF = 6;//蓝牙关闭

    public final static String Command_Type_write = "Command_Type_write";//写
    public final static String Command_Type_read = "Command_Type_read";//读
    public final static String Command_Type_enNotify = "Command_Type_enNotify";//打开通知
    public final static String Command_Type_disNotify = "Command_Type_disNotify";//关闭通知

    /**
     * 判断数组a是否包数组b
     */
    public static boolean containArray(byte[] a, byte[] b) {
        if (a.length == 0 || b.length == 0) {
            return true;
        }
        if (a.length < b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (i + b.length <= a.length) {
                byte[] bytes = new byte[b.length];
                System.arraycopy(a, i, bytes, 0, b.length);
                if (Arrays.equals(bytes, b)) {
                    return true;
                }
            }
        }
        return false;
    }
}
