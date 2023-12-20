package com.linkiing.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.linkiing.ble.api.BLEManager;
import com.linkiing.ble.callback.BLEConnectStatusCallback;
import com.linkiing.ble.log.LOGUtils;
import com.linkiing.ble.utils.BLEConstant;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 蓝牙连接
 */
@SuppressLint("MissingPermission")
class BLEConnect implements BLEConnectCallback {
    private static final String TAG = "BLEConnect";
    public static final int hanConnect = 1001;
    public static final int hanConnected = 1002;
    public static final int hanRequestMtu = 1003;
    public static final int hanServicesDiscovered = 1004;
    public static final int hanServicesDiscoverTimeOut = 1005;
    public static final int hanBleActivate = 1006;
    public static final int hanConnectOutTime = 1007;
    public static final int hanDisconnect = 1008;
    public static final int hanSetNotification = 1009;
    private static final int DISCOVER_SERVICES_MAX_INDEX = 3;//扫描服务重试次数
    private final int MTU_SET = 512;
    private final BluetoothGattCallback bluetoothGattCallback;
    private final ArrayList<NotificationFormat> notificationList = new ArrayList<>();//需要设置通知的列表
    private final CopyOnWriteArrayList<NotificationFormat> notificationSetList = new CopyOnWriteArrayList<>();//需要设置通知的列表
    private final BLEConnectHandler bleConnectHandler;
    private final Object BLUETOOTH_GATT_LOCK = new Object();
    private final BLEManager bleManager;
    private BLEDevice bleDevice;
    private BluetoothGatt bluetoothGatt;
    private long connectOutTime = BLEManager.DEF_CONNECT_OUT_TIME;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    private boolean isMtuRequested = false;
    private int mtuSize = 20;
    private int discoverServicesIndex = 0;

    public BLEConnect(BluetoothGattCallback bluetoothGattCallback) {
        this.bluetoothGattCallback = bluetoothGattCallback;
        this.bleManager = BLEManager.getInstance();

        bleConnectHandler = new BLEConnectHandler(Looper.getMainLooper(), bleManager.getContext());

        if (bluetoothGattCallback == null) {
            LOGUtils.e(TAG + " connect() bluetoothGattCallback==null");
        }
    }

    /**
     * 设置通知
     *
     * @param notificationFormatList 需要设置通知的列表
     */
    @Override
    public void setNotificationList(List<NotificationFormat> notificationFormatList) {
        notificationList.clear();
        if (notificationFormatList == null) {
            return;
        }
        if (notificationFormatList.isEmpty()) {
            return;
        }
        for (NotificationFormat bean : notificationFormatList) {
            if (!hasNotification(bean)) {
                notificationList.add(bean);
            }
        }
    }

    private boolean hasNotification(NotificationFormat format) {
        for (int i = 0; i < notificationList.size(); i++) {
            NotificationFormat bean = notificationList.get(i);
            if (bean.getSERVICE().equals(format.getSERVICE())
                    && bean.getUUID().equals(format.getUUID())) {
                return true;
            }
        }
        return false;
    }

    /**
     * MTU_SIZE
     */
    @Override
    public int getMtuSize() {
        return mtuSize;
    }

    @Override
    public BluetoothGatt getBluetoothGatt() {
        synchronized (BLUETOOTH_GATT_LOCK) {
            return bluetoothGatt;
        }
    }

    /**
     * 正在连接设备或者设备连接中
     */
    @Override
    public boolean isConnect() {
        return isConnected;
    }

    @Override
    public void setConnectOutTime(long outTime) {
        if (outTime > 6 * 1000) {
            this.connectOutTime = outTime;
        }
    }

    /**
     * 连接设备
     */
    @Override
    public synchronized boolean connect(BLEDevice bleDevice) {
        this.bleDevice = bleDevice;

        if (bleDevice == null) {
            LOGUtils.e(TAG + " connect() device==null");
            return false;
        }

        if (BLEUtils.isBLEConnect(bleDevice.getDevice())) {
            LOGUtils.e(TAG + " connect() isConnect() = true");
            return false;
        } else {
            isConnected = false;
        }

        int size = bleManager.getConnectDevice().size();
        if (size >= getMaxConnectNumber()) {
            LOGUtils.e(TAG + " getConnectDevice().size() >= getMaxConnectNumber()!  size:" + size);
            return false;
        }

        isConnecting = true;

        long delayTime;
        synchronized (BLUETOOTH_GATT_LOCK) {
            if (bluetoothGatt != null) {
                //连接之前先释放资源
                belGattClose(0);

                //延时1s再连接，让系统释放资源
                delayTime = 1000;
            } else {
                delayTime = 100;
            }
        }

        //发起设备连接
        if (bleConnectHandler != null) {
            postMessageDelayed(hanConnect,delayTime);
        }

        //开启超时检测
        if (bleConnectHandler != null) {
            postMessageDelayed(hanConnectOutTime,connectOutTime);
        }

        return true;
    }

    private int getMaxConnectNumber() {
        int maxConnectNumber = bleManager.getBleConfig().BLE_CONNECT_DEVICE_MAX_NUMBER;
        if (maxConnectNumber < 1) {
            //最小可连接数量1
            maxConnectNumber = 1;
        }
        if (maxConnectNumber > 8) {
            //最大可连接数量8
            maxConnectNumber = 8;
        }
        LOGUtils.v(TAG + " BLE_CONNECT_DEVICE_MAX_NUMBER:" + maxConnectNumber);
        return maxConnectNumber;
    }

    @Override
    public boolean disconnect() {
        synchronized (BLUETOOTH_GATT_LOCK) {
            if (bluetoothGatt != null) {
                refreshCache();
            }
            return bleDisconnect(0);
        }
    }

    @Override
    public void gattClose() {
        synchronized (BLUETOOTH_GATT_LOCK) {
            if (bluetoothGatt != null) {
                refreshCache();
            }
            belGattClose(1);
        }
    }

    private boolean bleDisconnect(int disCode) {
        LOGUtils.d(TAG + " bleDisconnect() disCode:" + disCode);
        synchronized (BLUETOOTH_GATT_LOCK) {
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
            }
            isConnected = false;
            notificationSetList.clear();
            removeAllMessage();
            return true;
        }
    }

    private void belGattClose(int code) {
        LOGUtils.d(TAG + " belGattClose() code:" + code);
        synchronized (BLUETOOTH_GATT_LOCK) {
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
            }
        }
    }

    /**
     * 清除蓝牙缓存
     */
    private synchronized Boolean refreshCache() {
        synchronized (BLUETOOTH_GATT_LOCK) {
            try {
                if (bluetoothGatt != null) {
                    Method localMethod = bluetoothGatt.getClass().getMethod("refresh");
                    return (Boolean) localMethod.invoke(bluetoothGatt);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private class BLEConnectHandler extends Handler {
        WeakReference<Context> wContext;

        public BLEConnectHandler(@NonNull Looper looper, Context context) {
            super(looper);
            if (context != null) {
                wContext = new WeakReference<>(context);
            }
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (wContext == null) {
                return;
            }

            Context context = wContext.get();
            if (context == null) {
                //context销毁了
                return;
            }

            switch (msg.what) {
                case hanConnect:
                    if (bluetoothGattCallback != null && bleDevice != null && bleDevice.getDevice() != null) {
                        //连接设备
                        synchronized (BLUETOOTH_GATT_LOCK) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                bluetoothGatt = bleDevice.getDevice().connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                            } else {
                                bluetoothGatt = bleDevice.getDevice().connectGatt(context, false, bluetoothGattCallback);
                            }
                            if (bluetoothGatt == null) {
                                LOGUtils.e(TAG + " Error! bluetoothGatt == null");
                                bleDisconnect(1);
                                postConnectStatus(BLEConstant.BLE_STATUS_CONNECT_FAIL);
                            } else {
                                LOGUtils.i(TAG + " connectGatt ok!");
                            }
                        }
                    } else {
                        LOGUtils.e(TAG + " Error! handleMessage bluetoothGattCallback == null");
                        bleDisconnect(2);
                        postConnectStatus(BLEConstant.BLE_STATUS_CONNECT_FAIL);
                    }
                    break;
                case hanConnected:
                    //设备连接成功
                    if (bleConnectHandler != null) {
                        bleConnectHandler.removeMessages(hanConnect);
                        bleConnectHandler.removeMessages(hanConnectOutTime);
                    }
                    discoverServicesIndex = 0;
                    //扫描服务
                    discoverServices();
                    break;
                case hanServicesDiscoverTimeOut:
                    //扫描服务超时
                    if (discoverServicesIndex < DISCOVER_SERVICES_MAX_INDEX) {
                        discoverServicesIndex++;
                        //重新扫描服务
                        discoverServices();
                    } else {
                        bleDisconnect(4);
                        postConnectStatus(BLEConstant.BLE_STATUS_CONNECT_FAIL);
                    }
                    break;
                case hanServicesDiscovered:
                    //扫描到服务
                    if (notificationList.isEmpty()) {
                        //无需设置通知，直接请求mtu
                        postMessageDelayed(hanRequestMtu, 500);
                    } else {
                        //开启通知
                        if (notificationSetList.isEmpty()) {
                            notificationSetList.addAll(notificationList);
                            postMessageDelayed(hanSetNotification, 500);
                        }
                    }
                    break;
                case hanSetNotification:
                    //设置通知
                    if (notificationSetList.isEmpty()) {
                        //设置通知完成
                        //请求配置mtu需要在打开通知之后。
                        postMessageDelayed(hanRequestMtu, 500);
                    } else {
                        synchronized (BLUETOOTH_GATT_LOCK) {
                            if (bluetoothGatt != null) {
                                NotificationFormat format = notificationSetList.get(0);
                                boolean b = BLEUtils.enableCharacteristicNotification(bluetoothGatt, format.getSERVICE(), format.getUUID());
                                LOGUtils.i(TAG + " 设置通知:" + format.getUUID() + " " + b);
                                notificationSetList.remove(0);

                                postMessageDelayed(hanSetNotification, 350);
                            } else {
                                bleDisconnect(5);
                                postConnectStatus(BLEConstant.BLE_STATUS_CONNECT_FAIL);
                            }
                        }
                    }
                    break;
                case hanRequestMtu:
                    //请求MTU
                    requestMtu();
                    break;
                case hanBleActivate:
                    //蓝牙连接成功并可用（通知设置完成，mtu申请完成）
                    isConnecting = false;
                    isConnected = true;
                    postConnectStatus(BLEConstant.BLE_STATUS_CONNECTED);
                    break;
                case hanConnectOutTime:
                    bleDisconnect(6);
                    postConnectStatus(BLEConstant.BLE_STATUS_CONNECT_TIME_OUT);
                    break;
                case hanDisconnect:
                    if (bleConnectHandler != null) {
                        bleConnectHandler.removeMessages(hanConnect);
                        bleConnectHandler.removeMessages(hanConnectOutTime);
                    }
                    postConnectStatus(BLEConstant.BLE_STATUS_DISCONNECTED);
                    break;
            }
        }
    }

    private void postMessageDelayed(int what, long delayMillis) {
        //LOGUtils.d(TAG + " postMessageDelayed what:" + what + " delayMillis:" + delayMillis);
        if (bleConnectHandler != null) {
            bleConnectHandler.removeMessages(what);
            bleConnectHandler.sendEmptyMessageDelayed(what, delayMillis);
        } else {
            bleDisconnect(7);
            postConnectStatus(BLEConstant.BLE_STATUS_CONNECT_FAIL);
        }
    }

    private void removeAllMessage() {
        if (bleConnectHandler != null) {
            bleConnectHandler.removeCallbacksAndMessages(null);
        }
    }

    private void postConnectStatus(int connectStatus) {
        LOGUtils.d(TAG + " postConnectStatus connectStatus:" + connectStatus);
        for (BLEConnectStatusCallback callback : bleManager.getBleConnectStatusCallbackList()) {
            if (callback != null && bleDevice != null) {
                callback.onBLEConnectStatus(bleDevice, connectStatus);
            }
        }
    }

    private void discoverServices() {
        LOGUtils.i(TAG + " discoverServices start!");
        notificationSetList.clear();
        synchronized (BLUETOOTH_GATT_LOCK) {
            if (bluetoothGatt != null) {
                if (!bluetoothGatt.discoverServices()) {
                    bluetoothGatt.disconnect();
                } else {
                    postMessageDelayed(hanServicesDiscoverTimeOut, 1500);
                }
            }
        }
    }

    /*请求设置mtu*/
    private void requestMtu() {
        LOGUtils.i(TAG + " requestMtu start!");
        isMtuRequested = true;
        boolean isRequestMtu = false;
        synchronized (BLUETOOTH_GATT_LOCK) {
            if (bluetoothGatt != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                isRequestMtu = bluetoothGatt.requestMtu(this.MTU_SET + 3);
                LOGUtils.v(TAG + " requestMtu  = " + isRequestMtu);
            }
        }
        //2s mtu请求为回复，直接连接设备完成
        postMessageDelayed(hanBleActivate, 2000);
    }

    @Override
    public void onBLEConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        switch (newState) {
            case BluetoothProfile.STATE_DISCONNECTED:
                isConnected = false;
                belGattClose(2);
                if (isConnecting) {
                    //重连设备
                    postMessageDelayed(hanConnect, 3000);
                } else {
                    gatt.close();
                    postMessageDelayed(hanDisconnect, 3000);
                }
                break;
            case BluetoothProfile.STATE_CONNECTED:
                LOGUtils.i(TAG + " connected!");
                postMessageDelayed(hanConnected, 500);
                break;
            default:
                isConnected = false;
                break;
        }
    }

    @Override
    public void onBLEServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            LOGUtils.i(TAG + " discoverServices success!");
            if (bleConnectHandler != null) {
                bleConnectHandler.removeMessages(hanServicesDiscoverTimeOut);
            }
            postMessageDelayed(hanServicesDiscovered, 10);
        } else {
            if (gatt.getDevice().getUuids() == null) {
                gatt.disconnect();
            }
        }
    }

    @Override
    public void onBLEMtuChanged(int mtu, int status) {
        LOGUtils.v(TAG + " onBLEMtuChanged status:" + status + "  mtu:" + mtu);
        if (!isMtuRequested) {
            LOGUtils.e(TAG + " onMtuChanged 未协商返回,不处理！ mtu:" + mtu);
            return;
        }
        isMtuRequested = false;
        if (BluetoothGatt.GATT_SUCCESS == status) {
            if (mtu <= 23) {
                mtuSize = 20;
            } else if (mtu > MTU_SET + 3) {
                mtuSize = MTU_SET;
            } else {
                mtuSize = mtu - 3;
            }
        } else {
            //未申请成功，则还是20
            mtuSize = 20;
        }

        //连接设备完成
        postMessageDelayed(hanBleActivate, 10);
    }
}
