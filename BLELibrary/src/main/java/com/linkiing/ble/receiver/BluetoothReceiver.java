package com.linkiing.ble.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.linkiing.ble.BLECallbackImp;
import com.linkiing.ble.BLEDevice;
import com.linkiing.ble.api.BLEManager;
import com.linkiing.ble.api.BLEScanner;
import com.linkiing.ble.callback.BLEConnectStatusCallback;
import com.linkiing.ble.callback.BLEOnOffStatusCallback;
import com.linkiing.ble.log.LOGUtils;
import com.linkiing.ble.utils.BLEConstant;

public class BluetoothReceiver extends BroadcastReceiver {
    private boolean isStateOff = false;

    public void registerBluetoothReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(this, filter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        String action = intent.getAction();
        if (action == null) return;
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            LOGUtils.d("BluetoothReceiver state:" + state);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    //蓝牙关闭
                    disconnectAllAndPostStatus();

                    if (BLEScanner.getInstance().isStartScan()) {
                        isStateOff = true;
                        BLEScanner.getInstance().stopScan();
                    }

                    postBleStatus(BLEConstant.BLE_STATUS_STATE_OFF);
                    break;
                case BluetoothAdapter.STATE_ON:
                    //蓝牙打开
                    if (isStateOff) {
                        isStateOff = false;
                        if (!BLEScanner.getInstance().isStartScan()) {
                            BLEScanner.getInstance().startScan(true);
                        }
                    }

                    postBleStatus(BLEConstant.BLE_STATUS_STATE_ON);
                    break;
            }
        }
    }

    private void disconnectAllAndPostStatus() {
        LOGUtils.d("BluetoothReceiver disconnectAllAndPostStatus()");
        for (BLEDevice bleDevice : BLEScanner.getInstance().getAllDevList()) {
            if (bleDevice != null) {
                bleDevice.disconnect();
                for (BLEConnectStatusCallback callback : BLECallbackImp.getInstance().getBleConnectStatusCallbackList()) {
                    if (callback != null) {
                        callback.onBLEConnectStatus(bleDevice, BLEConstant.BLE_STATUS_DISCONNECTED);
                    }
                }
            }
        }
    }

    private void postBleStatus(int bleStatus) {
        LOGUtils.d("BluetoothReceiver postBleStatus() bleStatus:" + bleStatus);
        for (BLEOnOffStatusCallback callback : BLECallbackImp.getInstance().getBleOnOffStatusCallbackList()) {
            if (callback != null) {
                callback.onOffStatusCallback(bleStatus);
            }
        }
    }
}
