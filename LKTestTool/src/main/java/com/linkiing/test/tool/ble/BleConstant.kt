package com.linkiing.test.tool.ble

import com.linkiing.ble.NotificationFormat

object BleConstant {
    const val SERVICE_RESET_MESH = "0000fff0-0000-1000-8000-00805f9b34fb" // 服务
    const val SEND_UUID_RESET_MESH = "0000fff2-0000-1000-8000-00805f9b34fb" // 通道
    const val NOTIF_UUID_RESET_MESH = "" // 通道

    const val SERVICE_TEST1 = "f000ccc0-0451-4000-b000-000000000000" // 服务
    const val SEND_UUID_TEST1 = "f000ccc5-0451-4000-b000-000000000000" // 通道
    const val NOTIF_UUID_TEST1 = "f000ccc5-0451-4000-b000-000000000000" // 通道

    /**
     * 设置通知
     */
    fun getNotificationList(): List<NotificationFormat> {
        return listOf(
            NotificationFormat(
                BleConstant.SERVICE_TEST1,
                BleConstant.NOTIF_UUID_TEST1
            )
        )
    }
}