package com.linkiing.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;

import com.linkiing.ble.api.BLEManager;
import com.linkiing.ble.callback.BLENotificationCallback;
import com.linkiing.ble.callback.BLEReadCallback;
import com.linkiing.ble.log.LOGUtils;

/**
 * BluetoothGattCallback
 */
@SuppressLint("MissingPermission")
abstract class IBluetoothGattCallback extends BluetoothGattCallback {
    protected final BLEConnectCallback bleConnect = new BLEConnect(this);
    protected final BLEWriteCallback bleCommandPolicy = new BLECommandPolicy();

    /**
     * 获取当前设备
     */
    protected abstract BLEDevice getCurrentDevice();

    private boolean isThisGatt(BluetoothGatt gatt) {
        if (gatt == null) {
            LOGUtils.e("isGatt() Error! gatt == null");
            return false;
        }
        if (getCurrentDevice() == null) {
            LOGUtils.e("isGatt() Error! getCurrentDevice() == null");
            return false;
        }
        return gatt.getDevice().getAddress().equals(getCurrentDevice().getDeviceMac());
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        LOGUtils.v("onConnectionStateChange ==> status:" + status + "   newState:" + newState);
        if (!isThisGatt(gatt)) {
            return;
        }
        if (gatt.getDevice().getAddress().equals(getCurrentDevice().getDeviceMac())) {
            bleConnect.onBLEConnectionStateChange(gatt, status, newState);
        } else {
            gatt.close();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (!isThisGatt(gatt)) {
            return;
        }
        LOGUtils.logUUID(gatt);
        bleConnect.onBLEServicesDiscovered(gatt, status);
    }

    /**
     * 写回应
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (!isThisGatt(gatt)) {
            return;
        }
        if (status == BluetoothGatt.GATT_SUCCESS) {
            bleCommandPolicy.onCharacteristicWriteCallback(gatt, characteristic);
        }
    }

    /**
     * 读回应
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (!isThisGatt(gatt)) {
                return;
            }
            if (characteristic != null) {
                String uuid = characteristic.getUuid().toString();
                byte[] bytes = characteristic.getValue();
                for (BLEReadCallback readCallback : BLEManager.getInstance().getReadCallbackList()){
                    if (readCallback != null){
                        readCallback.onReadCallback(getCurrentDevice(), uuid, bytes);
                    }
                }
            }
            bleCommandPolicy.onCharacteristicReadCallback(gatt, characteristic);
        }
    }

    /**
     * 通知
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!isThisGatt(gatt)) {
            return;
        }
        if (characteristic != null) {
            String uuid = characteristic.getUuid().toString();
            byte[] bytes = characteristic.getValue();
            for (BLENotificationCallback notificationCallback : BLEManager.getInstance().getNotificationCallbackList()){
                if (notificationCallback != null){
                    notificationCallback.onNotificationCallback(getCurrentDevice(), uuid, bytes);
                }
            }
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        if (!isThisGatt(gatt)) {
            return;
        }
        bleConnect.onBLEMtuChanged(mtu, status);
    }
}
