package com.linkiing.ble.callback;

import com.linkiing.ble.BLEDevice;

public interface BLEConnectStatusCallback {

   /**
    * 连接状态改变
    * @param bleDevice 设备
    * @param connectStatus 连接状态
    */
   void onBLEConnectStatus(BLEDevice bleDevice, int connectStatus);
}
