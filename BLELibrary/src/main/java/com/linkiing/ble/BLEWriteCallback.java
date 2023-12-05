package com.linkiing.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * 写回应
 */
interface BLEWriteCallback {

    /**
     * 数据写回调
     * @param gatt           BluetoothGatt
     * @param characteristic BluetoothGattCharacteristic
     */
    void onCharacteristicWriteCallback(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    /**
     * 数据读回调
     * @param gatt           BluetoothGatt
     * @param characteristic BluetoothGattCharacteristic
     */
    void onCharacteristicReadCallback(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

    /**
     * 发送操作命令
     * @param commandFormat 命令
     * @param mtuSize       mtu
     */
    boolean sendCommandFormat(CommandFormat commandFormat, BluetoothGatt bluetoothGatt, int mtuSize);

    /**
     * 停止发送并清空所有操作命令
     */
    void stopAndClearCommandFormat();
}
