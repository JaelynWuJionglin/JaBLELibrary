package com.linkiing.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.linkiing.ble.api.BLENotifyDataFormat;
import com.linkiing.ble.api.BLEReadDataFormat;
import com.linkiing.ble.api.BLEWriteDataFormat;

import java.util.List;

/**
 * 扫描到的设备数据类
 */
@SuppressLint("MissingPermission")
public class BLEDevice extends IBluetoothGattCallback {
    private BluetoothDevice device = null;
    private BluetoothGatt bluetoothGatt = null;
    private String deviceName = "";
    private String deviceMac = "";
    private byte[] scanRecord = null;
    private int rssi = -1;

    public void setData(BluetoothDevice device, byte[] scanRecord, int rssi) {
        this.device = device;
        this.deviceName = device.getName();
        this.deviceMac = device.getAddress();
        this.scanRecord = scanRecord;
        this.rssi = rssi;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(byte[] scanRecord) {
        this.scanRecord = scanRecord;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getRssiLevel() {
        return BLEUtils.getRssiLevel(rssi);
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }

    @Override
    protected BLEDevice getCurrentDevice() {
        return this;
    }

    /**
     * 设置连接之前开启通知
     */
    public BLEDevice setBefConnectEnNotificationList(List<NotificationFormat> notificationFormatList) {
        bleConnect.setNotificationList(notificationFormatList);
        return this;
    }

    /**
     * 设备是否连接
     */
    public boolean isConnected() {
        return bleConnect.isConnect();
    }

    /**
     * 设备是否在连接中
     */
    public boolean isConnecting() {
        return bleConnect.isConnecting();
    }

    /**
     * 设置连接超时时间
     */
    public BLEDevice setConnectOutTime(long outTime){
        bleConnect.setConnectOutTime(outTime);
        return this;
    }

    /**
     * 连接设备
     */
    public boolean connect() {
        return bleConnect.connect(this);
    }

    public boolean readRssi() {
        return bleConnect.readRssi();
    }

    /**
     * gattClose
     */
    public void gattClose() {
        bleConnect.gattClose();
    }

    /**
     * 断开设备连接
     */
    public boolean disconnect() {
        return bleConnect.disconnect();
    }

    /**
     * 写数据
     */
    public boolean writeCommand(BLEWriteDataFormat writeDataFormat) {
        return sendCommandFormat(new CommandFormat(writeDataFormat));
    }

    /**
     * 读数据
     */
    public boolean readCommand(BLEReadDataFormat readDataFormat) {
        return sendCommandFormat(new CommandFormat(readDataFormat));
    }

    /**
     * 设置通知
     */
    public boolean setNotification(BLENotifyDataFormat notifyDataFormat) {
        return sendCommandFormat(new CommandFormat(notifyDataFormat));
    }

    /**
     * 停止并清空命令
     */
    public void stopAndClearCommandFormat() {
        bleCommandPolicy.stopAndClearCommandFormat();
    }


    /*
     * 发送命令（读/写/通知）
     * @param commandFormat 命令
     */
    private boolean sendCommandFormat(CommandFormat commandFormat) {
        return bleCommandPolicy.sendCommandFormat(commandFormat, bleConnect.getBluetoothGatt(), bleConnect.getMtuSize());
    }
}
