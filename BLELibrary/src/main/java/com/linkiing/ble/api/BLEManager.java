package com.linkiing.ble.api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.linkiing.ble.BLECallbackImp;
import com.linkiing.ble.BLEDevice;
import com.linkiing.ble.NotificationFormat;
import com.linkiing.ble.callback.BLEConnectStatusCallback;
import com.linkiing.ble.callback.BLENotificationCallback;
import com.linkiing.ble.callback.BLEOnOffStatusCallback;
import com.linkiing.ble.callback.BLEPermissionCallback;
import com.linkiing.ble.callback.BLEReadCallback;
import com.linkiing.ble.callback.BLEReadRssiCallback;
import com.linkiing.ble.log.LOGUtils;
import com.linkiing.ble.receiver.BluetoothReceiver;
import com.linkiing.ble.utils.BackstageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 蓝牙操作主类
 * <a href="https://github.com/JaelynWuJionglin/JaBLELibrary">...</a>
 */
public class BLEManager {
    public static final long DEF_CONNECT_OUT_TIME = 15 * 1000;
    private final CopyOnWriteArrayList<BLEDevice> bleConnectDeviceList = new CopyOnWriteArrayList<>();
    private volatile static BLEManager instance = null;
    private final BLECallbackImp bleCallbackImp;
    private BluetoothReceiver bluetoothReceiver;
    private Application context;
    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private List<NotificationFormat> notificationFormatList;
    private BLEConfig bleConfig = new BLEConfig();
    private long connectOutTime = DEF_CONNECT_OUT_TIME;
    private String connectDeviceName = "";

    private BLEManager() {
        bleCallbackImp = BLECallbackImp.getInstance();
    }

    /**
     * 单利模式（线程安全）
     */
    public static BLEManager getInstance() {
        if (instance == null) {
            instance = new BLEManager();
        }
        return instance;
    }

    /**
     * 获取BLE配置
     */
    public BLEConfig getBleConfig() {
        return bleConfig;
    }

    /**
     * 设置BLE配置
     *
     * @param bleConfig BLE配置
     */
    public BLEManager setBleConfig(BLEConfig bleConfig) {
        this.bleConfig = bleConfig;
        return this;
    }

    /**
     * 初始化蓝牙库，在Application中
     */
    public void init(Application context) {
        this.context = context;
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BackstageUtils.getInstance().init(context);

        if (bluetoothReceiver == null) {
            bluetoothReceiver = new BluetoothReceiver();
            bluetoothReceiver.registerBluetoothReceiver(context);
        }
    }

    /**
     * getContext()
     *
     * @return context
     */
    public Application getContext() {
        return context;
    }

    /**
     * 是否支持蓝牙
     */
    public boolean isBleSupport() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 蓝牙是否打开
     */
    public boolean isBleOpen() {
        BluetoothAdapter adapter = getBluetoothAdapter();
        if (adapter == null) {
            LOGUtils.w("BLEManager ble not open");
            return false;
        }
        return adapter.isEnabled();
    }

    /**
     * 系统打开蓝牙
     */
    @SuppressLint("MissingPermission")
    public void sysOpenBLE(Activity activity) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, 9898);
    }

    /**
     * 跳转GPS设置
     */
    public boolean openGPSSettings(
            Activity activity,
            @StringRes int titleTextId,
            @StringRes int msgTextId,
            @StringRes int confirmTextId,
            @StringRes int cancelTextId,
            BLEPermissionCallback belPermissionCallback) {

        if (checkGPSIsOpen(activity)) {
            return true;
        } else {
            //没有打开则弹出对话框
            new AlertDialog.Builder(activity)
                    .setTitle(titleTextId)
                    .setMessage(msgTextId) // 拒绝, 退出应用
                    .setNegativeButton(cancelTextId, (dialog, which) -> {
                        dialog.dismiss();

                        if (belPermissionCallback != null) {
                            belPermissionCallback.onOpenGpsDialogCancel();
                        }
                    })
                    .setPositiveButton(confirmTextId, (dialog, which) -> {
                        //跳转GPS设置界面
                        goGpsSettingActivity(activity);
                    })
                    .setCancelable(false)
                    .show();
            return false;
        }
    }

    /**
     * 检测GPS是否打开
     */
    public boolean checkGPSIsOpen(Activity activity) {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * 跳转GPS设置界面
     */
    public void goGpsSettingActivity(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        activity.startActivityForResult(intent, 1);
    }

    /**
     * 检查蓝牙
     *
     * @return 0:支持蓝牙,蓝牙功能也已经开启
     * 1:支持蓝牙,但蓝牙功能未开启，发送开启蓝牙开关的广播。
     * 2:不支持蓝牙,或 BluetoothAdapter==null
     */
    @SuppressLint("MissingPermission")
    public int checkBluetooth(Activity activity) {
        if (getBluetoothAdapter() != null) {
            if (getBluetoothAdapter().isEnabled()) {
                return 0;
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, 1);
                return 1;
            }
        } else {
            return 2;
        }
    }

    /**
     * 获取mBluetoothAdapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        return mBluetoothAdapter;
    }

    /**
     * 获取mBluetoothManager
     */
    public BluetoothManager getBluetoothManager() {
        return mBluetoothManager;
    }

    /**
     * 获取已连接设备列表
     */
    public List<BLEDevice> getConnectDevice() {
        List<BLEDevice> list = new ArrayList<>();
        List<BLEDevice> deviceList = BLEScanner.getInstance().getAllDevList();
        if (deviceList.isEmpty()) {
            return list;
        } else {
            for (int i = 0; i < deviceList.size(); i++) {
                BLEDevice devices = deviceList.get(i);
                if (devices.isConnected()) {
                    list.add(devices);
                }
            }
        }
        return list;
    }

    /**
     * 获取BLEDevice
     */
    public BLEDevice getBLEDevice(String macAddress) {
        return BLEScanner.getInstance().getBLEDevice(macAddress);
    }

    /**
     * 是否连接上了设备
     */
    public boolean isConnectDevices() {
        return !getConnectDevice().isEmpty();
    }

    /**
     * 是否连接了设备
     */
    public boolean isConnectDevice(String macAddress) {
        if (TextUtils.isEmpty(macAddress)) {
            return false;
        }
        BLEDevice device = getBLEDevice(macAddress);
        if (device == null) {
            return false;
        }
        return device.isConnected();
    }

    /**
     * 是否正在连接设备
     */
    public boolean isConnectingDevice(String macAddress) {
        if (TextUtils.isEmpty(macAddress)) {
            return false;
        }
        BLEDevice device = getBLEDevice(macAddress);
        if (device == null) {
            return false;
        }
        return device.isConnecting();
    }

    /**
     * 连接超时时常
     *
     * @param outTime 大于6*1000
     */
    public BLEManager setConnectOutTime(long outTime) {
        this.connectOutTime = outTime;
        return this;
    }

    /**
     * 连接设备
     */
    public boolean connectDevice(BLEDevice bleDevice) {
        if (bleDevice == null) {
            return false;
        }

        //清除已断开设备的gatt
        for (int i = 0; i < bleConnectDeviceList.size(); i++) {
            BLEDevice dev = bleConnectDeviceList.get(i);
            if (dev != null && !isConnectDevice(dev.getDeviceMac())) {
                dev.disconnect();
                bleConnectDeviceList.remove(dev);
            }
        }

        BLEScanner.getInstance().addBLEDevice(bleDevice);
        if (notificationFormatList != null && !notificationFormatList.isEmpty()) {
            bleDevice.setBefConnectEnNotificationList(notificationFormatList);
        }
        bleDevice.setConnectOutTime(connectOutTime);
        if (bleDevice.connect()) {
            bleConnectDeviceList.add(bleDevice);
            connectOutTime = DEF_CONNECT_OUT_TIME;
            return true;
        } else {
            connectOutTime = DEF_CONNECT_OUT_TIME;
            return false;
        }
    }

    /**
     * 给连接的设备设置设备名
     * （在使用《connectDevice(String macAddress)》连接设备的时候，可能会获取不到设备名，可用这个方法将已知的设备名设置给BLEDevice）
     */
    public BLEManager setConnectDeviceName(String connectDeviceName) {
        if (!TextUtils.isEmpty(connectDeviceName)) {
            this.connectDeviceName = connectDeviceName;
        }
        return this;
    }

    /**
     * 连接设备
     */
    public boolean connectDevice(String macAddress) {
        if (TextUtils.isEmpty(macAddress)) {
            return false;
        }

        BLEDevice bleDevice = getBLEDevice(macAddress);
        if (bleDevice == null) {
            BluetoothDevice bluetoothDevice = getBluetoothAdapter().getRemoteDevice(macAddress);
            if (bluetoothDevice == null) {
                return false;
            }
            bleDevice = new BLEDevice();
            bleDevice.setData(bluetoothDevice, new byte[0], null, 0);
            if (!TextUtils.isEmpty(connectDeviceName)) {
                bleDevice.setDeviceName(connectDeviceName);
            }
        }
        return connectDevice(bleDevice);
    }

    /**
     * 断开设备连接
     */
    public boolean disconnectDevice(String macAddress) {
        if (TextUtils.isEmpty(macAddress)) {
            return false;
        }
        BLEDevice device = getBLEDevice(macAddress);
        if (device == null) {
            return false;
        }
        return device.disconnect();
    }

    /**
     * 断开所有设备
     */
    public boolean disConnectAllDevices() {
        List<BLEDevice> deviceList = BLEScanner.getInstance().getAllDevList();
        if (!deviceList.isEmpty()) {
            for (int i = 0; i < deviceList.size(); i++) {
                BLEDevice device = deviceList.get(i);
                if (device.isConnected() || device.isConnecting()) {
                    device.disconnect();
                }
            }
        }
        return true;
    }

    /**
     * 向设备发送数据
     *
     * @param macAddress      发送数据到设备的mac地址
     * @param writeDataFormat 发送数据装载
     */
    public boolean sendData(String macAddress, BLEWriteDataFormat writeDataFormat) {
        if (TextUtils.isEmpty(macAddress)) {
            return false;
        }
        BLEDevice device = getBLEDevice(macAddress);
        if (device == null) {
            return false;
        }
        return device.writeCommand(writeDataFormat);
    }

    /**
     * 向已经连接的设备发送数据
     *
     * @param writeDataFormat 发送数据装载
     */
    public boolean sendDataToConnect(BLEWriteDataFormat writeDataFormat) {
        List<BLEDevice> connectList = getConnectDevice();
        if (connectList.isEmpty()) {
            return false;
        } else {
            for (BLEDevice bleDevice : connectList) {
                if (!bleDevice.writeCommand(writeDataFormat)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 读设备数据
     *
     * @param macAddress     发送数据到设备的mac地址
     * @param readDataFormat 读取数据装载
     */
    public boolean readData(String macAddress, BLEReadDataFormat readDataFormat) {
        if (TextUtils.isEmpty(macAddress)) {
            return false;
        }
        BLEDevice device = getBLEDevice(macAddress);
        if (device == null) {
            return false;
        }
        return device.readCommand(readDataFormat);
    }

    /**
     * 向已经连接的设备读取数据
     *
     * @param readDataFormat 读取数据装载
     */
    public void readDataToConnect(BLEReadDataFormat readDataFormat) {
        List<BLEDevice> connectList = getConnectDevice();
        for (BLEDevice bleDevice : connectList) {
            bleDevice.readCommand(readDataFormat);
        }
    }

    /**
     * 设置通知
     *
     * @param macAddress       发送数据到设备的mac地址
     * @param notifyDataFormat 设置通知数据装载
     */
    public boolean setNotify(String macAddress, BLENotifyDataFormat notifyDataFormat) {
        if (TextUtils.isEmpty(macAddress)) {
            return false;
        }
        BLEDevice device = getBLEDevice(macAddress);
        if (device == null) {
            return false;
        }
        return device.setNotification(notifyDataFormat);
    }

    /**
     * 向已经连接的设备设置通知
     *
     * @param notifyDataFormat 设置通知数据装载
     */
    public void setNotifyToConnect(BLENotifyDataFormat notifyDataFormat) {
        List<BLEDevice> connectList = getConnectDevice();
        for (BLEDevice bleDevice : connectList) {
            bleDevice.setNotification(notifyDataFormat);
        }
    }

    /**
     * 读取设备信号强度
     *
     * @param macAddress 发送数据到设备的mac地址
     */
    public boolean readRssi(String macAddress) {
        if (TextUtils.isEmpty(macAddress)) {
            return false;
        }
        BLEDevice device = getBLEDevice(macAddress);
        if (device == null) {
            return false;
        }
        if (device.isConnected()) {
            return device.readRssi();
        }
        return false;
    }

    /**
     * 读取已经连接设备的信号强度
     */
    public void readRssiToConnect() {
        List<BLEDevice> connectList = getConnectDevice();
        for (BLEDevice bleDevice : connectList) {
            bleDevice.readRssi();
        }
    }

    /**
     * 停止发送并清空命令队列
     */
    public void stopSendAndClear(String macAddress) {
        if (TextUtils.isEmpty(macAddress)) {
            return;
        }
        BLEDevice device = getBLEDevice(macAddress);
        if (device == null) {
            return;
        }
        device.stopAndClearCommandFormat();
    }

    /**
     * 停止发送并清空命令队列（所有连接的设备）
     */
    public void stopSendAndClearAll() {
        List<BLEDevice> connectList = getConnectDevice();
        for (BLEDevice bleDevice : connectList) {
            bleDevice.stopAndClearCommandFormat();
        }
    }

    /**
     * 设置需要打开的服务通知通道。
     * （connect() 之前设置有效）
     */
    public BLEManager setNotificationList(List<NotificationFormat> notificationFormatList) {
        this.notificationFormatList = notificationFormatList;
        return this;
    }

    /**
     * 添加手机蓝牙开启关闭监听
     */
    public void addBleOnOffStatusCallback(BLEOnOffStatusCallback bleOnOffStatusCallback) {
        if (!bleCallbackImp.getBleOnOffStatusCallbackList().contains(bleOnOffStatusCallback)) {
            bleCallbackImp.getBleOnOffStatusCallbackList().add(bleOnOffStatusCallback);
        }
    }

    /**
     * 移除手机蓝牙开启关闭监听
     */
    public void removeBleOnOffStatusCallback(BLEOnOffStatusCallback bleOnOffStatusCallback) {
        bleCallbackImp.getBleOnOffStatusCallbackList().remove(bleOnOffStatusCallback);
    }

    /**
     * 添加设备连接状态监听
     */
    public void addBleConnectStatusCallback(BLEConnectStatusCallback bleConnectStatusCallback) {
        if (!bleCallbackImp.getBleConnectStatusCallbackList().contains(bleConnectStatusCallback)) {
            bleCallbackImp.getBleConnectStatusCallbackList().add(bleConnectStatusCallback);
        }
    }

    /**
     * 移除连接状态监听
     */
    public void removeBleConnectStatusCallback(BLEConnectStatusCallback bleConnectStatusCallback) {
        bleCallbackImp.getBleConnectStatusCallbackList().remove(bleConnectStatusCallback);
    }

    /**
     * 添加通知数据监听
     */
    public void addNotificationCallback(BLENotificationCallback notificationCallback) {
        if (!bleCallbackImp.getNotificationCallbackList().contains(notificationCallback)) {
            bleCallbackImp.getNotificationCallbackList().add(notificationCallback);
        }
    }

    /**
     * 移除通知数据监听
     */
    public void removeNotificationCallback(BLENotificationCallback notificationCallback) {
        bleCallbackImp.getNotificationCallbackList().remove(notificationCallback);
    }

    /**
     * 添加读取数据回调
     */
    public void addReadCallback(BLEReadCallback readCallback) {
        if (!bleCallbackImp.getReadCallbackList().contains(readCallback)) {
            bleCallbackImp.getReadCallbackList().add(readCallback);
        }
    }

    /**
     * 移除读取数据回调
     */
    public void removeReadCallback(BLEReadCallback readCallback) {
        bleCallbackImp.getReadCallbackList().remove(readCallback);
    }

    /**
     * 添加读取数据回调
     */
    public void addReadRssiCallback(BLEReadRssiCallback readRssiCallback) {
        if (!bleCallbackImp.getReadRssiCallbackList().contains(readRssiCallback)) {
            bleCallbackImp.getReadRssiCallbackList().add(readRssiCallback);
        }
    }

    /**
     * 移除读取数据回调
     */
    public void removeReadRssiCallback(BLEReadRssiCallback readRssiCallback) {
        bleCallbackImp.getReadRssiCallbackList().remove(readRssiCallback);
    }

    /**
     * clear
     */
    public void clear() {
        BLECallbackImp.getInstance().clear();
    }
}
