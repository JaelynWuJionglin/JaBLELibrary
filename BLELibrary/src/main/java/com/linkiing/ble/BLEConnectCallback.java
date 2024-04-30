package com.linkiing.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.List;

/**
 * BLE连接设备Callback
 */
interface BLEConnectCallback {

    /**
     * 连接状态改变
     *
     * @param gatt     BluetoothGatt
     * @param status   status
     * @param newState newState
     */
    void onBLEConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    /**
     * 扫描到服务通道
     *
     * @param gatt   BluetoothGatt
     * @param status status
     */
    void onBLEServicesDiscovered(BluetoothGatt gatt, int status);

    /**
     * 特侦值写回调
     * @param gatt           BluetoothGatt
     * @param descriptor     BluetoothGattDescriptor
     */
    void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor);

    /**
     * MTU改变
     *
     * @param mtu    mth
     * @param status status
     */
    void onBLEMtuChanged(int mtu, int status);

    /**
     * 设置需要在连接完成之前要开启的通知
     *
     * @param notificationFormatList 要开启的通知列表
     */
    void setNotificationList(List<NotificationFormat> notificationFormatList);

    /**
     * 获取mtu
     *
     * @return mtu
     */
    int getMtuSize();

    /**
     * 获取BluetoothGatt
     *
     * @return BluetoothGatt
     */
    BluetoothGatt getBluetoothGatt();

    /**
     * 设备是否连接
     *
     * @return true:已连接  false:未连接
     */
    boolean isConnect();

    /**
     * 设备是否在连接中
     *
     * @return true:连接中  false:否
     */
    boolean isConnecting();

    /**
     * 设置连接超时时间
     *
     * @param outTime 大于6*1000
     */
    void setConnectOutTime(long outTime);

    /**
     * 连接设备
     *
     * @param bleDevice 要连接的设备
     * @return true:执行成功  false:执行出错
     */
    boolean connect(BLEDevice bleDevice);

    /**
     *读取信号强度
     */
    boolean readRssi();

    /**
     * 断开连接
     *
     * @return true:执行成功  false:执行出错
     */
    boolean disconnect();

    /**
     * gattClose
     */
    void gattClose();
}
