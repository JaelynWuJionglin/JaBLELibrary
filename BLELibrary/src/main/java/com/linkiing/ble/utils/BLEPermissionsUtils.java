package com.linkiing.ble.utils;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.StringRes;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.linkiing.ble.R;
import com.linkiing.ble.api.BLEManager;
import com.linkiing.ble.callback.BLEPermissionCallback;
import com.linkiing.ble.log.LOGUtils;

import java.util.List;

public class BLEPermissionsUtils {
    private static boolean checkGps = true;
    private static @StringRes int titleTextId = R.string.notifyTitle;
    private static @StringRes int msgTextId = R.string.gpsNotifyMsg;
    private static @StringRes int confirmTextId = R.string.confirm_text;
    private static @StringRes int cancelTextId = R.string.cancel_text;

    public static void setDialogStringRes(
            @StringRes int tiTextId,
            @StringRes int mTextId,
            @StringRes int cfTextId,
            @StringRes int clTextId) {
        titleTextId = tiTextId;
        msgTextId = mTextId;
        confirmTextId = cfTextId;
        cancelTextId = clTextId;
    }

    /**
     * 是否检查GPS
     */
    public static void setCheckGps(boolean check) {
        checkGps = check;
    }

    /**
     * 蓝牙权限
     */
    public static void blePermissions(Activity activity, BLEPermissionCallback belPermissionCallback) {
        if (!BLEManager.getInstance().isBleSupport()) {
            LOGUtils.e("blePermissions Error! 设备不支持蓝牙");
            return;
        }

        OnPermissionCallback permCallback = new OnPermissionCallback() {
            @Override
            public void onGranted(List<String> permissions, boolean all) {
                //检查蓝牙和gps是否打开
                if (checkBleAndGps(activity, belPermissionCallback)) {
                    if (belPermissionCallback != null) {
                        belPermissionCallback.onGranted(permissions, all);
                    }
                }
            }

            @Override
            public void onDenied(List<String> permissions, boolean never) {
                OnPermissionCallback.super.onDenied(permissions, never);
                if (belPermissionCallback != null) {
                    belPermissionCallback.onDenied(permissions, never);
                }
            }
        };

        //申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            XXPermissions
                    .with(activity)
                    //.unchecked()// 设置不触发错误检测机制（局部设置）
                    .permission(Permission.ACCESS_FINE_LOCATION)
                    .permission(Permission.ACCESS_COARSE_LOCATION)
                    .permission(Permission.BLUETOOTH_SCAN)
                    .permission(Permission.BLUETOOTH_CONNECT)
                    .permission(Permission.BLUETOOTH_ADVERTISE)
                    .request(permCallback);
        } else {
            XXPermissions
                    .with(activity)
                    .permission(Permission.ACCESS_FINE_LOCATION)
                    .permission(Permission.ACCESS_COARSE_LOCATION)
                    .request(permCallback);
        }
    }

    /**
     * 检查蓝牙和GPS是否打开
     */
    private static boolean checkBleAndGps(Activity activity, BLEPermissionCallback belPermissionCallback) {
        if (BLEManager.getInstance().isBleOpen()) {
            if (!checkGps && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                //安卓12及以上，蓝牙不需要定位。
                //checkGps 是为了app有其他定位需求，在这里可以一起申请打开。
                return true;
            } else {
                return BLEManager.getInstance().openGPSSettings(
                        activity,
                        titleTextId,
                        msgTextId,
                        confirmTextId,
                        cancelTextId,
                        belPermissionCallback);
            }
        } else {
            BLEManager.getInstance().sysOpenBLE(activity);
            return false;
        }
    }
}
