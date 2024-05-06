package com.linkiing.ble;

import com.linkiing.ble.callback.BLEConnectStatusCallback;
import com.linkiing.ble.callback.BLENotificationCallback;
import com.linkiing.ble.callback.BLEReadCallback;
import com.linkiing.ble.callback.BLEReadRssiCallback;

import java.util.concurrent.CopyOnWriteArrayList;

public class BLECallbackImp {
   private volatile static BLECallbackImp instance = null;
   private final CopyOnWriteArrayList<BLEConnectStatusCallback> bleConnectStatusCallbackList = new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<BLENotificationCallback> notificationCallbackList = new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<BLEReadCallback> readCallbackList = new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<BLEReadRssiCallback> readRssiCallbackList = new CopyOnWriteArrayList<>();

   private BLECallbackImp() {}

   /**
    * 单利模式（线程安全）
    */
   public static BLECallbackImp getInstance() {
      if (instance == null) {
         instance = new BLECallbackImp();
      }
      return instance;
   }

   public CopyOnWriteArrayList<BLEConnectStatusCallback> getBleConnectStatusCallbackList() {
      return bleConnectStatusCallbackList;
   }

   public CopyOnWriteArrayList<BLENotificationCallback> getNotificationCallbackList() {
      return notificationCallbackList;
   }

   public CopyOnWriteArrayList<BLEReadCallback> getReadCallbackList() {
      return readCallbackList;
   }

   public CopyOnWriteArrayList<BLEReadRssiCallback> getReadRssiCallbackList() {
      return readRssiCallbackList;
   }

   /**
    * clear
    */
   public void clear() {
      bleConnectStatusCallbackList.clear();
      notificationCallbackList.clear();
      readCallbackList.clear();
   }
}
