package com.linkiing.ble.api;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.text.TextUtils;

import com.linkiing.ble.BLEDevice;
import com.linkiing.ble.BLEUtils;
import com.linkiing.ble.callback.BLEScanDeviceCallback;
import com.linkiing.ble.callback.BLEScannerFilterCallback;
import com.linkiing.ble.log.LOGUtils;
import com.linkiing.ble.utils.BLEConstant;
import com.linkiing.ble.utils.BackstageUtils;
import com.linkiing.ble.utils.ByteUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 蓝牙扫描类
 */
@SuppressLint("MissingPermission")
public class BLEScanner extends ScanCallback implements BackstageUtils.BackstageListener {
    private volatile static BLEScanner instance = null;
    //相同设备发送间隔
    private static final long SEND_VALUE_TIME = 500;
    private final CopyOnWriteArrayList<BLEScanDeviceCallback> bleScanDeviceCallbackList = new CopyOnWriteArrayList<>();
    //上一次开始扫描和结束扫描的时间间隔
    private final long MIN_SCAN_TIME = 6 * 1000;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    //过滤mac地址
    private String FILTER_MAC_STR = "";
    //过滤名称字符串
    private String FILTER_NAME_STR = "";
    //过滤Service UUID
    private String FILTER_SERVICE_UUID_STR = "";
    //过滤广播包
    private final List<byte[]> FILTER_RECORD = new ArrayList<>();
    //过滤信号强度等级
    private int FILTER_RSSI_LEVEL = 0;
    //自定义过滤
    private BLEScannerFilterCallback bleScannerFilterCallback;
    //是否真正扫描
    private boolean isScanning = false;
    private boolean isNeedStop = true;
    private long startTime = 0;
    //默认扫描持续时间
    private long SCAN_TIME = 10 * 1000;
    //仿止callback太过频繁
    private long callBackTime = 0;
    private boolean isStart = false;
    private boolean backstageChangeScanning = false;
    private boolean isBackstageStop = false;
    private final List<BLEDevice> deviceList = new ArrayList<>();

    private BLEScanner() {
        mBluetoothAdapter = BLEManager.getInstance().getBluetoothAdapter();
        BackstageUtils.getInstance().addBackstageListener(this);
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    /**
     * 单利模式（线程安全）
     * 此类使用单利模式，是保证开启扫描和结束扫描的Runnable对象唯一。
     */
    public static BLEScanner getInstance() {
        if (instance == null) {
            instance = new BLEScanner();
        }
        return instance;
    }

    /**
     * 是否开启了设备搜索
     */
    public boolean isStartScan() {
        return isStart;
    }

    /**
     * 添加搜索蓝牙设备扫描监听
     */
    public void addScanDeviceCallback(BLEScanDeviceCallback scanDeviceCallback) {
        if (!bleScanDeviceCallbackList.contains(scanDeviceCallback)) {
            bleScanDeviceCallbackList.add(scanDeviceCallback);
        }
    }

    /**
     * 删除搜索蓝牙设备扫描监听
     */
    public void removeScanDeviceCallback(BLEScanDeviceCallback scanDeviceCallback) {
        bleScanDeviceCallbackList.remove(scanDeviceCallback);
    }

    /**
     * 设置扫描时间,如果==0,则为不停止扫描
     */
    public BLEScanner setScanTime(long SCAN_TIME) {
        if (SCAN_TIME == 0 || SCAN_TIME >= MIN_SCAN_TIME) {
            this.SCAN_TIME = SCAN_TIME;
        } else {
            this.SCAN_TIME = MIN_SCAN_TIME;
        }
        return this;
    }

    /**
     * 设置过滤mac地址
     *
     * @param macStr 过滤的mac字符串
     */
    public BLEScanner setFilterMacStr(String macStr){
        this.FILTER_MAC_STR = macStr.toUpperCase(Locale.ENGLISH);
        return this;
    }

    /**
     * 设置过滤名称字符串
     *
     * @param str 过滤名称字符串
     */
    public BLEScanner setFilterNameStr(String str) {
        this.FILTER_NAME_STR = str;
        return this;
    }

    /**
     * 设置过滤ServiceUuid字符串
     *
     * @param str 过滤ServiceUuid字符串
     */
    public BLEScanner setFilterServiceUuid(String str) {
        this.FILTER_SERVICE_UUID_STR = str.toUpperCase(Locale.ENGLISH);
        return this;
    }

    /**
     * 设置广播包过滤
     *
     * @param filterRecord bytes
     */
    public BLEScanner setFilterRecord(List<byte[]> filterRecord) {
        this.FILTER_RECORD.clear();
        this.FILTER_RECORD.addAll(filterRecord);
        return this;
    }

    /**
     * 设置过滤信号强度等级
     *
     * @param rssi 信号强度
     */
    public BLEScanner setFilterRssi(int rssi) {
        this.FILTER_RSSI_LEVEL = BLEUtils.getRssiLevel(rssi);
        return this;
    }

    /**
     * 设置自定义过滤规则
     *
     * @param bleScannerFilterCallback 过滤接口
     */
    public void setBLEScannerFilterCallback(BLEScannerFilterCallback bleScannerFilterCallback) {
        this.bleScannerFilterCallback = bleScannerFilterCallback;
    }

    /**
     * 清除过滤条件
     */
    public void clearFilters() {
        this.FILTER_MAC_STR = "";
        this.FILTER_NAME_STR = "";
        this.FILTER_SERVICE_UUID_STR = "";
        this.FILTER_RECORD.clear();
        this.FILTER_RSSI_LEVEL = 0;
        this.bleScannerFilterCallback = null;
    }

    /**
     * 开始扫描设备
     *
     * @param nowScan 是否立即开启扫描，不判断扫描状态。
     *                （在重启蓝牙开关的时候应该是true，否则是false）
     */
    public void startScan(boolean nowScan) {
        LOGUtils.d("startScan()  FILTER_NAME_STR:" + FILTER_NAME_STR
                + " FILTER_SERVICE_UUID_STR:" + FILTER_SERVICE_UUID_STR
                + " FILTER_RSSI_LEVEL:" + FILTER_RSSI_LEVEL
                + " FILTER_RECORD:" + FILTER_RECORD.size());

        LOGUtils.d("startScan()  nowScan:" + nowScan + " isScanning:" + isScanning);

        devListClear();
        isStart = true;
        if (!nowScan && isScanning) {
            //正在扫描中，则继续扫描， 不停止。
            isNeedStop = false;
        } else {
            long delayedTime = MIN_SCAN_TIME - (System.currentTimeMillis() - startTime);
            LOGUtils.v("==============> delayedTime:" + delayedTime);
            handler.removeCallbacks(runnableStart);
            handler.postDelayed(runnableStart, Math.max(200, delayedTime));
            isNeedStop = true;
        }

        if (SCAN_TIME != 0 && isNeedStop) {
            handler.removeCallbacks(runnableStop);
            handler.postDelayed(runnableStop, SCAN_TIME);
        }
    }

    /**
     * 结束扫描设备
     */
    public void stopScan() {
        stopScan(1, false);
    }

    /**
     * stopAndClear
     */
    public void stopAndClear() {
        stopScan(2, false);
        devListClear();
    }

    /**
     * 获取所有扫描到的设备列表，不重复
     * (包含已经连接的设备)
     *
     * @return deviceList
     */
    public List<BLEDevice> getAllDevList() {
        return deviceList;
    }

    /**
     * 获取所有扫描到的设备列表，不重复
     * （不包含已连接设备）
     *
     * @return deviceList
     */
    public List<BLEDevice> getAllNotConnectDevList() {
        List<BLEDevice> list = new ArrayList<>();
        for (BLEDevice device : deviceList) {
            if (!device.isConnected()) {
                list.add(device);
            }
        }
        return list;
    }

    /**
     * 根据mac地址获取 BLEDevice
     *
     * @param macAddress mac地址
     * @return BLEDevice
     */
    public BLEDevice getBLEDevice(String macAddress) {
        for (BLEDevice dev : deviceList) {
            if (dev.getDeviceMac().equals(macAddress)) {
                return dev;
            }
        }
        return null;
    }

    /**
     * addBLEDevice
     *
     * @param bleDevice BLEDevice
     */
    public void addBLEDevice(BLEDevice bleDevice) {
        if (bleDevice != null && !TextUtils.isEmpty(bleDevice.getDeviceMac())) {
            synchronized (deviceList) {
                Iterator<BLEDevice> iterator = deviceList.iterator();
                while (iterator.hasNext()) {
                    BLEDevice bean = iterator.next();
                    if (bean != null
                            && !TextUtils.isEmpty(bean.getDeviceMac())
                            && bean.getDeviceMac().equals(bleDevice.getDeviceMac())) {
                        iterator.remove();

                        break;
                    }
                }
                deviceList.add(bleDevice);
            }
        }
    }

    /**
     * 清空设备列表，已连接设备不清除
     */
    public void devListClear() {
        Iterator<BLEDevice> iterator = deviceList.iterator();
        while (iterator.hasNext()) {
            BLEDevice bleDevice = iterator.next();
            if (!bleDevice.isConnected() && !bleDevice.isConnecting()) {
                iterator.remove();
            }
        }
    }

    private void stopScan(int code, boolean isNow) {
        LOGUtils.d("stopScan()  isScanning:" + isScanning + "  isNow:" + isNow + "  code:" + code);
        isStart = false;
        handler.removeCallbacks(runnableStart);
        handler.removeCallbacks(runnableStop);
        if (mBluetoothAdapter == null) {
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            return;
        }
        if (isNow) {
            isScan(false, 0);
        } else {
            if (isScanning) {
                long delayedTime = MIN_SCAN_TIME - (System.currentTimeMillis() - startTime);
                if (delayedTime > 0) {
                    isNeedStop = true;
                    handler.postDelayed(runnableStop, delayedTime + 100);
                } else {
                    isScan(false, 1);
                }
            }
        }
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        if (!isStart) {
            return;
        }
        if (result == null) {
            LOGUtils.e("BLEScanner error! onScanResult result==null");
            return;
        }
        if (result.getScanRecord() == null) {
            LOGUtils.e("BLEScanner error! onScanResult result.getScanRecord()==null");
            return;
        }
        byte[] scanRecord = result.getScanRecord().getBytes();
//        LOGUtils.v("name:" + result.getDevice().getName() + " scanRecord:" + ByteUtils.toHexString(scanRecord,","));
        if (!recordFilters(scanRecord)) {
            return;
        }
        BluetoothDevice device = result.getDevice();
        if (device == null) {
            LOGUtils.e("BLEScanner error! onScanResult device==null");
            return;
        }
        String address = device.getAddress();
        if (address == null) {
            LOGUtils.e("BLEScanner error! onScanResult address==null");
            return;
        }
        if (address.length() != 17) {
            LOGUtils.e("BLEScanner error! onScanResult address.length()!=17  ==> " + address);
            return;
        }
        if (!TextUtils.isEmpty(FILTER_MAC_STR)
                && !address.toUpperCase(Locale.ENGLISH).contains(FILTER_MAC_STR)) {
            return;
        }
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = "";
        }
        if (!nameFilters(name)) {
            //LOGUtils.e("------ nameFilters ------");
            return;
        }
        if (!serviceUuidFilters(result.getScanRecord().getServiceUuids())) {
            return;
        }
        if (!rssiLevelFilters(result.getRssi())) {
            //LOGUtils.e("------ rssiLevelFilters ------");
            return;
        }
        if (bleScannerFilterCallback != null) {
            if (!bleScannerFilterCallback.isFilter(name, scanRecord, result.getRssi())) {
                //LOGUtils.e("------ bleScannerFilterCallback ------");
                return;
            }
        }
//        LOGUtils.v("name:" + device.getName() + " address:" + address);
        if (deviceList.isEmpty()) {
            BLEDevice data = new BLEDevice();
            data.setData(device, result.getScanRecord().getBytes(), result.getScanRecord().getServiceUuids(), result.getRssi());
            deviceList.add(data);
            sendDevData(data);
        } else {
            BLEDevice devData = getBLEDevice(address);
            if (devData != null) {
                //扫描到相同的设备,更新数据
                devData.setRssi(result.getRssi());
                devData.setScanRecord(result.getScanRecord().getBytes());
                devData.setParcelUuids(result.getScanRecord().getServiceUuids());
                if (System.currentTimeMillis() - callBackTime > SEND_VALUE_TIME) {
                    sendDevData(devData);
                }
            } else {
                BLEDevice data = new BLEDevice();
                data.setData(device, result.getScanRecord().getBytes(), result.getScanRecord().getServiceUuids(), result.getRssi());
                deviceList.add(data);
                sendDevData(data);
            }
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        LOGUtils.e("BLEScanner error! onScanFailed errorCode = " + errorCode);
        if (errorCode == ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY) {
            LOGUtils.e("Error！扫描过于频繁。");
        } else if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
            isScan(false, 2);
            startScan(false);
        }
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        super.onBatchScanResults(results);
    }

    /**
     * 名称过滤
     *
     * @param devName 过滤名称字符串
     */
    private boolean nameFilters(String devName) {
        if (FILTER_NAME_STR.equals("")) {
            //过滤字符串为空，则不过滤设备
            return true;
        } else {
            if (devName.equals("")) {
                return false;
            }
        }
        return devName.contains(FILTER_NAME_STR);
    }

    /**
     * ServiceUuid过滤
     *
     * @param parcelUuids 过滤ServiceUuid列表
     */
    private boolean serviceUuidFilters(List<ParcelUuid> parcelUuids) {
        if (FILTER_SERVICE_UUID_STR.equals("")) {
            //过滤字符串为空，则不过滤设备
            return true;
        } else {
            if (parcelUuids == null) {
                return false;
            }
            for (ParcelUuid uuid : parcelUuids) {
                if (uuid != null) {
                    if (uuid.toString().toUpperCase(Locale.ENGLISH).contains(FILTER_SERVICE_UUID_STR)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 广播包过滤
     */
    private boolean recordFilters(byte[] scanRecord) {
        if (scanRecord == null) {
            //广播包错误
            return false;
        }
        if (FILTER_RECORD.isEmpty()) {
            //表示未设置过滤
            return true;
        }

        for (byte[] bytes : FILTER_RECORD) {
            if (!BLEConstant.containArray(scanRecord, bytes)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 信号强度等级过滤
     */
    private boolean rssiLevelFilters(int rssi) {
        int rssiLeve = BLEUtils.getRssiLevel(rssi);
        return rssiLeve >= FILTER_RSSI_LEVEL;
    }

    /**
     * 发送蓝牙扫描到的数据
     *
     * @param bleDevice BLEDevice
     */
    private void sendDevData(@NotNull BLEDevice bleDevice) {
        for (BLEScanDeviceCallback scanDeviceCallback : bleScanDeviceCallbackList) {
            if (scanDeviceCallback != null) {
                scanDeviceCallback.onScanDevice(bleDevice);
                callBackTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * runnableStop
     */
    private final Runnable runnableStop = () -> {
        LOGUtils.d("BLEScanner runnableStop  isNeedStop:" + isNeedStop);
        if (isNeedStop) {
            isScan(false, 3);
            for (BLEScanDeviceCallback scanDeviceCallback : bleScanDeviceCallbackList) {
                if (scanDeviceCallback != null) {
                    scanDeviceCallback.onScanStop();
                }
            }
        }
    };

    /**
     * runnableStart
     */
    private final Runnable runnableStart = () -> {
        if (isStart) {
            isScan(true, 4);
        }
    };

    /**
     * isScan
     *
     * @param scanOrStop ture开启扫描    false停止扫描
     */
    private void isScan(boolean scanOrStop, int code) {
        LOGUtils.d("BLEScanner isScan  scanOrStop:" + scanOrStop + "  code:" + code);
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BLEManager.getInstance().getBluetoothAdapter();
        }
        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        if (scanOrStop) {
            LOGUtils.d("BLEScanner isScan isBackstage:" + BackstageUtils.getInstance().isBackstage());
            if (BackstageUtils.getInstance().isBackstage()) {
                //App在后台
                backstageChangeScanning = true;
            } else {
                LOGUtils.i("BLEScanner BluetoothLeScanner.startScan()");
                startTime = System.currentTimeMillis();
                isScanning = true;
                mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), BLEScanner.this);
            }
        } else {
            isScanning = false;
            backstageChangeScanning = false;
            try {
                LOGUtils.i("BLEScanner BluetoothLeScanner.stopScan()");
                mBluetoothLeScanner.stopScan(BLEScanner.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder scanSettingBuilder = new ScanSettings.Builder();
        //设置蓝牙LE扫描的扫描模式。
        //使用最高占空比进行扫描。建议只在应用程序处于此模式时使用此模式在前台运行
        scanSettingBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //设置蓝牙LE扫描滤波器硬件匹配的匹配模式
            //在主动模式下，即使信号强度较弱，hw也会更快地确定匹配.在一段时间内很少有目击/匹配。
            scanSettingBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);

            //设置蓝牙LE扫描的回调类型
            //为每一个匹配过滤条件的蓝牙广告触发一个回调。如果没有过滤器是活动的，所有的广告包被报告
            scanSettingBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        }
        return scanSettingBuilder.build();
    }

    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilterList = new ArrayList<>();
        // 通过服务 uuid 过滤自己要连接的设备   过滤器搜索GATT服务UUID
        //这里创建一个空的过滤器
        ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder();
        scanFilterList.add(scanFilterBuilder.build());
        return scanFilterList;
    }

    @Override
    public void onBackstageChange(boolean isBackstage) {
        LOGUtils.v("BLEScanner isBackstage:" + isBackstage + "  isScanning:" + isScanning + "  BackstageChangeScanning:" + backstageChangeScanning);
        if (isBackstage) {
            //App退到后台，停止搜索设备
            backstageChangeScanning = isScanning;
            if (isScanning) {
                stopScan(3, false);
                isBackstageStop = true;
            }
        } else {
            //从后台回到前台
            if (isBackstageStop) {
                isBackstageStop = false;
                startScan(false);
            }
        }
    }
}
