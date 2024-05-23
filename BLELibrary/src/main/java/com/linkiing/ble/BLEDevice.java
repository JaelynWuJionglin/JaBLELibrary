package com.linkiing.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.ParcelUuid;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.linkiing.ble.api.BLENotifyDataFormat;
import com.linkiing.ble.api.BLEReadDataFormat;
import com.linkiing.ble.api.BLEWriteDataFormat;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 扫描到的设备数据类
 */
@SuppressLint("MissingPermission")
public class BLEDevice extends IBluetoothGattCallback {
    private BluetoothDevice device = null;
    private String deviceName = "";
    private String deviceMac = "";
    private byte[] scanRecord = new byte[0];
    private List<ParcelUuid> parcelUuids = new ArrayList<>();
    private int rssi = -1;
    private final List<Integer> rssiList = new ArrayList<>();

    public void setData(@NonNull BluetoothDevice device, @NonNull byte[] scanRecord, List<ParcelUuid> parcelUuids, int rssi) {
        this.deviceName = device.getName() != null ? device.getName() : "";
        this.deviceMac = device.getAddress() != null ? device.getAddress() : "";
        setDevice(device);
        setScanRecord(scanRecord);
        setParcelUuids(parcelUuids);
        setRssi(rssi);
        addRssi(rssi);
    }

    public void setDevice(@NonNull BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName != null ? deviceName : "";
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac != null ? deviceMac : "";
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(byte[] scanRecord) {
        if (scanRecord == null) {
            this.scanRecord = new byte[0];
        } else {
            this.scanRecord = scanRecord;
        }
    }

    public void setParcelUuids(List<ParcelUuid> parcelUuids) {
        if (parcelUuids == null) {
            this.parcelUuids = new ArrayList<>();
        } else {
            this.parcelUuids = parcelUuids;
        }
    }

    public List<ParcelUuid> getParcelUuids() {
        return parcelUuids;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
        addRssi(rssi);
    }

    public List<Integer> getRssiList() {
        return rssiList;
    }

    private void addRssi(int rssi) {
        synchronized (rssiList) {
            rssiList.add(rssi);
            if (rssiList.size() > 30) {
                rssiList.remove(0);
            }
        }
    }

    public int getRssiLevel() {
        return BLEUtils.getRssiLevel(rssi);
    }

    @NonNull
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
    public BLEDevice setConnectOutTime(long outTime) {
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
