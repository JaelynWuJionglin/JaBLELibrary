package com.linkiing.test.tool.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import com.linkiing.ble.BLEDevice
import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.api.BLEWriteDataFormat
import com.linkiing.ble.callback.BLEReadRssiCallback
import com.linkiing.test.tool.base.BaseActivity
import com.linkiing.test.tool.databinding.ActivityConnectedBinding
import com.linkiing.test.tool.utlis.MyUtils
import java.util.concurrent.CopyOnWriteArrayList

class ConnectedActivity: BaseActivity<ActivityConnectedBinding>(), BLEReadRssiCallback{
    private val handler = Handler(Looper.getMainLooper())
    private val rssiList = CopyOnWriteArrayList<Int>()
    private var readRssiTime = 500L
    private var isReadRssiStart = false
    private var refReadRssiUiTime = 0L
    private var deviceName = ""
    private var deviceMac = ""

    override fun initBind(): ActivityConnectedBinding {
        return ActivityConnectedBinding.inflate(layoutInflater)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        deviceName = intent.getStringExtra("deviceName") ?: ""
        deviceMac = intent.getStringExtra("deviceMac") ?: ""

        binding.lyTop.tvDevName.text = deviceName
        binding.lyTop.tvMac.text = deviceMac

        BLEManager.getInstance().addReadRssiCallback(this)
    }

    @SuppressLint("SetTextI18n")
    override fun initLister() {
        binding.lyTop.lrStart.setOnClickListener {
            finish()
        }

        binding.lyTop.tvDisconnect.setOnClickListener {
            BLEManager.getInstance().disconnectDevice(deviceMac)
            finish()
        }

        binding.btRssiStart.setOnClickListener {
            isReadRssiStart = if (isReadRssiStart) {
                handler.removeCallbacks(readRssiRunnable)
                rssiList.clear()
                binding.btRssiStart.text = "开始读RSSI"
                false
            } else {
                val time = binding.etRssiTime.text?.toString()?.trim() ?: ""
                readRssiTime = MyUtils.strOrLong(time)
                if (readRssiTime < 200) {
                    readRssiTime = 200
                    binding.etRssiTime.setText("200")
                }
                nextReadRssi()
                binding.btRssiStart.text = "停止读RSSI"
                true
            }
        }

        binding.btResetMesh.setOnClickListener {
            val writeDataFormat = BLEWriteDataFormat()
            writeDataFormat.servicesUUID = "0000fff0-0000-1000-8000-00805f9b34fb"
            writeDataFormat.characteristicUUID = "0000fff2-0000-1000-8000-00805f9b34fb"
            writeDataFormat.bytes = byteArrayOf(0x44,0x56,0x52,0x45,0x53,0x45,0x54)
            BLEManager.getInstance().sendData(deviceMac,writeDataFormat)

            handler.removeCallbacks(meshResetRunnable)
            handler.postDelayed(meshResetRunnable,350)
        }
    }

    private val meshResetRunnable = Runnable {
        BLEManager.getInstance().disconnectDevice(deviceMac)
        finish()
    }

    private val readRssiRunnable = Runnable {
        val belDevice = BLEManager.getInstance().getBLEDevice(deviceMac) ?: return@Runnable
        belDevice.readRssi()

        nextReadRssi()
    }

    private fun nextReadRssi() {
        handler.removeCallbacks(readRssiRunnable)
        handler.postDelayed(readRssiRunnable,readRssiTime)
    }

    override fun onReadRssiCallback(devices: BLEDevice, rssi: Int) {
        if (!isReadRssiStart) {
            return
        }
        if (rssiList.size > 50) {
            rssiList.removeAt(0)
        }
        rssiList.add(rssi)

        if (System.currentTimeMillis() - refReadRssiUiTime > 500) {
            refReadRssiUiTime = System.currentTimeMillis()
            runOnUiThread {
                var text = ""
                for ((index,rssi) in rssiList.withIndex()) {
                    text = if (index == 0) {
                        "$rssi"
                    } else {
                        "$text  $rssi"
                    }
                }
                binding.tvRssiList.text = text
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BLEManager.getInstance().removeReadRssiCallback(this)
        handler.removeCallbacksAndMessages(null)
    }
}