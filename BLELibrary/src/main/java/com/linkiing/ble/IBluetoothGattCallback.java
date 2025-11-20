package com.linkiing.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.linkiing.ble.api.BLEManager;
import com.linkiing.ble.callback.BLENotificationCallback;
import com.linkiing.ble.callback.BLEReadCallback;
import com.linkiing.ble.callback.BLEReadRssiCallback;
import com.linkiing.ble.log.LOGUtils;
import com.linkiing.ble.utils.ByteUtils;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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
    @NotNull
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
        super.onConnectionStateChange(gatt, status, newState);
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
        super.onServicesDiscovered(gatt, status);
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
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (!isThisGatt(gatt)) {
            return;
        }
        if (status == BluetoothGatt.GATT_SUCCESS) {
            bleCommandPolicy.onCharacteristicWriteCallback(gatt, characteristic);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        if (!isThisGatt(gatt)) {
            return;
        }

        if (status == BluetoothGatt.GATT_SUCCESS) {
            bleConnect.onDescriptorWrite(gatt, descriptor);
            bleCommandPolicy.onDescriptorWrite(gatt, descriptor);
        }
    }

    /**
     * 通知
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (!isThisGatt(gatt)) {
            return;
        }
        if (characteristic != null) {
            UUID uuid = characteristic.getUuid();
            String uuidStr = uuid != null ? uuid.toString() : null;
            byte[] bytes = characteristic.getValue();
            LOGUtils.d("onCharacteristicChanged uuid:" + uuidStr + " bytes:" + ByteUtils.toHexString(bytes));
            for (BLENotificationCallback notificationCallback : BLECallbackImp.getInstance().getNotificationCallbackList()) {
                if (notificationCallback != null && uuidStr != null) {
                    notificationCallback.onNotificationCallback(getCurrentDevice(), uuidStr, bytes);
                }
            }
        }
    }

    /**
     * mtu
     */
    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        if (!isThisGatt(gatt)) {
            return;
        }
        bleConnect.onBLEMtuChanged(mtu, status);
    }

    /**
     * 读回应
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (!isThisGatt(gatt)) {
                return;
            }
            if (characteristic != null) {
                UUID uuid = characteristic.getUuid();
                String uuidStr = uuid != null ? uuid.toString() : null;
                byte[] bytes = characteristic.getValue();
                for (BLEReadCallback readCallback : BLECallbackImp.getInstance().getReadCallbackList()) {
                    if (readCallback != null && uuidStr != null && bytes != null) {
                        readCallback.onReadCallback(getCurrentDevice(), uuidStr, bytes);
                    }
                }
            }
            bleCommandPolicy.onCharacteristicReadCallback(gatt, characteristic);
        }
    }

    /**
     * 读信号值
     */
    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (!isThisGatt(gatt)) {
                return;
            }
            for (BLEReadRssiCallback readRssiCallback : BLECallbackImp.getInstance().getReadRssiCallbackList()) {
                if (readRssiCallback != null) {
                    readRssiCallback.onReadRssiCallback(getCurrentDevice(), rssi);
                }
            }
        }
    }
}
