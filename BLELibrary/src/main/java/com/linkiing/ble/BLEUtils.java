package com.linkiing.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;

import com.linkiing.ble.api.BLEManager;
import com.linkiing.ble.log.LOGUtils;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BLEUtils {
    /**
     * 设置通知
     **/
    private final static String Notification_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    /**
     * 计算蓝牙信号强度等级（0-100，越大表示信号越强）
     * @param rssi 蓝牙信号值
     */
    public static int getRssiLevel(int rssi) {
        return (int) Math.floor(100.0f * (127.0f + rssi) / (127.0f + 20.0f));
    }

    /**
     * 根据gattCharacteristic 获取 BluetoothGattCharacteristic
     * @param ServicesUUID       服务UUID。
     * @param CharacteristicUUID 通道UUID。
     */
    public static BluetoothGattCharacteristic getGattCharacteristic(BluetoothGatt gatt, String ServicesUUID, String CharacteristicUUID) {
        BluetoothGattCharacteristic mBluetoothGattCharacteristic = null;
        if (gatt != null) {
            List<BluetoothGattService> GattService = gatt.getServices();
            if (GattService.size() == 0) {
                return null;
            }
            for (BluetoothGattService gattService : GattService) {
                if (gattService.getUuid().toString().trim().toLowerCase(Locale.ENGLISH).equals(ServicesUUID.toLowerCase(Locale.ENGLISH))) {
                    List<BluetoothGattCharacteristic> mGattCharacteristic = gattService.getCharacteristics();
                    if (mGattCharacteristic.size() == 0) {
                        return null;
                    }
                    for (BluetoothGattCharacteristic gattCharacteristic : mGattCharacteristic) {
                        if (gattCharacteristic.getUuid().toString().trim().toLowerCase(Locale.ENGLISH).equals(CharacteristicUUID.toLowerCase(Locale.ENGLISH))) {
                            mBluetoothGattCharacteristic = gattCharacteristic;
                        }
                    }
                }
            }
        }
        return mBluetoothGattCharacteristic;
    }

    /**
     * 打开通知
     */
    @SuppressLint("MissingPermission")
    public static Boolean enableCharacteristicNotification(BluetoothGatt gatt, String servicesUUID, String characteristicUUID) {
        if (gatt == null){
            LOGUtils.e("Error! enableLostNordic() gatt == null");
            return false;
        }
        BluetoothGattCharacteristic characteristic = getGattCharacteristic(gatt, servicesUUID, characteristicUUID);
        if (characteristic == null) {
            LOGUtils.e("Error! enableLostNordic() characteristic == null");
            return false;
        }
        boolean isNotification = gatt.setCharacteristicNotification(characteristic, true);
        if (isNotification){
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(Notification_UUID));
            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            }
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    /**
     * 关闭通知
     */
    @SuppressLint("MissingPermission")
    public static Boolean disableCharacteristicNotification(BluetoothGatt gatt, String servicesUUID, String characteristicUUID) {
        if (gatt == null){
            LOGUtils.e("Error! enableLostNordic() gatt == null");
            return false;
        }
        BluetoothGattCharacteristic characteristic = getGattCharacteristic(gatt, servicesUUID, characteristicUUID);
        if (characteristic == null) {
            LOGUtils.e("Error! enableLostNordic() characteristic == null");
            return false;
        }
        boolean isNotification = gatt.setCharacteristicNotification(characteristic, true);
        if (isNotification){
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(Notification_UUID));
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    /**
     * 设备是否连接
     * @param device 蓝牙设备对象
     */
    @SuppressLint("MissingPermission")
    public static boolean isBLEConnected(BluetoothDevice device) {
        if (device == null) {
            LOGUtils.e("BLEDevices isConnect() device==null");
            return false;
        }
        BluetoothManager mBluetoothManager = BLEManager.getInstance().getBluetoothManager();
        if (mBluetoothManager != null) {
            int status = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
            LOGUtils.d( "isConnect ConnectState : " + status);
            return status == BluetoothAdapter.STATE_CONNECTED
                    || status == BluetoothAdapter.STATE_CONNECTING;
        }
        return false;
    }
}
