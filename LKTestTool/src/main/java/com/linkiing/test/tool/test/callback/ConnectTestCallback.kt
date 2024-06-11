package com.linkiing.test.tool.test.callback

import com.linkiing.ble.BLEDevice

interface ConnectTestCallback {

    /**
     * 连接测试开始
     */
    fun onStart()

    /**
     * 连接测试停止
     */
    fun onStop()

    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 设备连接测试开始
     */
    fun onConnectTestStart(bleDevice: BLEDevice)

    /**
     * 设备连接测试失败
     */
    fun onConnectTestFail(bleDevice: BLEDevice)

    /**
     * 设备连接测试成功
     */
    fun onConnectTestOk(bleDevice: BLEDevice)

}