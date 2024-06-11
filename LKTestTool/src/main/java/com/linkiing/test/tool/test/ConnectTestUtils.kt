package com.linkiing.test.tool.test

import com.linkiing.ble.BLEDevice
import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.api.BLEScanner
import com.linkiing.ble.callback.BLEConnectStatusCallback
import com.linkiing.ble.callback.BLEScanDeviceCallback
import com.linkiing.test.tool.sp.SpHelper
import com.linkiing.test.tool.test.callback.ConnectTestCallback
import com.linkiing.test.tool.utlis.ByteUtils

class ConnectTestUtils private constructor() : BLEScanDeviceCallback, BLEConnectStatusCallback {
    private var bleScanner: BLEScanner? = null
    private var connectTestCallback: ConnectTestCallback? = null
    private var isTestStart = false

    init {
        bleScanner = BLEScanner.getInstance()
        bleScanner?.addScanDeviceCallback(this)
        bleScanner?.setScanTime(30 * 1000)
        setBleScannerFilter()

        BLEManager.getInstance().addBleConnectStatusCallback(this)
    }

    companion object {
        val instance: ConnectTestUtils by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ConnectTestUtils()
        }
    }

    fun startTest(connectTestCallback: ConnectTestCallback) {
        this.connectTestCallback = connectTestCallback
        this.isTestStart = true
        startTestScan()
    }

    fun stopTest() {
        this.isTestStart = false
    }

    override fun onScanDevice(bleDevice: BLEDevice) {
        if (!SpHelper.instance.isConnectTestFilterDev(bleDevice.deviceMac)) {

        }
    }

    override fun onBLEConnectStatus(bleDevice: BLEDevice, connectStatus: Int) {
        TODO("Not yet implemented")
    }

    private fun startTestScan() {

    }

    private fun setBleScannerFilter() {
        bleScanner?.setFilterNameStr(SpHelper.instance.getFilterName())
        bleScanner?.setFilterServiceUuid(SpHelper.instance.getFilterUUID())
        bleScanner?.setFilterRecord(
            arrayListOf(
                ByteUtils.hexStringToBytes(SpHelper.instance.getFilterRecord1()),
                ByteUtils.hexStringToBytes(SpHelper.instance.getFilterRecord2()),
                ByteUtils.hexStringToBytes(SpHelper.instance.getFilterRecord3())
            )
        )
        bleScanner?.setFilterRssi(-SpHelper.instance.getFilterRssi())
    }
}