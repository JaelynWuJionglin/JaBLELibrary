package com.linkiing.test.tool.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import com.linkiing.ble.BLEDevice
import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.callback.BLENotificationCallback
import com.linkiing.ble.callback.BLEReadRssiCallback
import com.linkiing.ble.log.LOGUtils
import com.linkiing.ble.utils.ByteUtils
import com.linkiing.test.tool.base.BaseActivity
import com.linkiing.test.tool.ble.BleAgreement
import com.linkiing.test.tool.ble.BleConstant
import com.linkiing.test.tool.databinding.ActivityConnectedBinding
import com.linkiing.test.tool.dialog.InputTextDialog
import com.linkiing.test.tool.utlis.MyUtils
import com.linkiing.test.tool.utlis.ToastUtils
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

class ConnectedActivity : BaseActivity<ActivityConnectedBinding>(),
    BLEReadRssiCallback,
    BLENotificationCallback {
    private val handler = Handler(Looper.getMainLooper())
    private val rssiList = CopyOnWriteArrayList<Int>()
    private var mInputTextDialog: InputTextDialog? = null
    private var readRssiTime = 500L
    private var isReadRssiStart = false
    private var refReadRssiUiTime = 0L
    private var deviceName = ""
    private var deviceMac = ""
    private var modifyAdvName = ""

    override fun initBind(): ActivityConnectedBinding {
        return ActivityConnectedBinding.inflate(layoutInflater)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        deviceName = intent.getStringExtra("deviceName") ?: ""
        deviceMac = intent.getStringExtra("deviceMac") ?: ""

        binding.lyTop.tvDevName.text = deviceName
        binding.lyTop.tvMac.text = deviceMac

        binding.itemRssi.btModify.text = "读取Rssi"
        binding.itemRssi.etModify.setText("500")

        binding.itemAdvInv.btModify.text = "修改广播间隔"
        binding.itemAdvInv.etModify1.setText("100")
        binding.itemAdvInv.etModify2.setText("100")

        binding.itemConInv.btModify.text = "修改连接间隔"
        binding.itemConInv.etModify1.setText("30")
        binding.itemConInv.etModify2.setText("30")

        mInputTextDialog = InputTextDialog(this)

        BLEManager.getInstance().addReadRssiCallback(this)
        BLEManager.getInstance().addNotificationCallback(this)
    }

    @SuppressLint("SetTextI18n")
    override fun initLister() {
        binding.lyTop.lrStart.setOnClickListener {
            finish()
        }

        binding.btResetMesh.setOnClickListener {
            BleAgreement.instance.sendResetMesh()
            handler.removeCallbacks(meshResetRunnable)
            handler.postDelayed(meshResetRunnable, 350)
        }

        binding.lyTop.tvDisconnect.setOnClickListener {
            BLEManager.getInstance().disconnectDevice(deviceMac)
            finish()
        }

        binding.itemRssi.btModify.setOnClickListener {
            isReadRssiStart = if (isReadRssiStart) {
                handler.removeCallbacks(readRssiRunnable)
                rssiList.clear()
                binding.itemRssi.btModify.text = "开始读RSSI"
                false
            } else {
                val time = binding.itemRssi.etModify.text?.toString()?.trim() ?: ""
                readRssiTime = MyUtils.strOrLong(time)
                if (readRssiTime < 200) {
                    readRssiTime = 200
                    binding.itemRssi.etModify.setText("200")
                }
                nextReadRssi()
                binding.itemRssi.btModify.text = "停止读RSSI"
                true
            }
        }

        binding.itemAdvInv.btModify.setOnClickListener {
            val minInv = MyUtils.strOrInt(binding.itemAdvInv.etModify1.text?.toString() ?: "")
            val maxInv = MyUtils.strOrInt(binding.itemAdvInv.etModify2.text?.toString() ?: "")
            BleAgreement.instance.modifyAdvInv(minInv, maxInv)
        }

        binding.itemConInv.btModify.setOnClickListener {
            val minInv = MyUtils.strOrInt(binding.itemConInv.etModify1.text?.toString() ?: "")
            val maxInv = MyUtils.strOrInt(binding.itemConInv.etModify2.text?.toString() ?: "")
            BleAgreement.instance.modifyConInv(minInv, maxInv)
        }

        binding.btModifyAdvName.setOnClickListener {
            mInputTextDialog?.setTitleText("请输入蓝牙广播名(长度小于10)")
                ?.setEditTextMaxInputLength(10)
                ?.setDefText(deviceName)
                ?.setEditTextInputType(InputTextDialog.TEXT_INPUT_TYPE_EN_TEXT)
                ?.setOnDialogListener {
                    if (it.isNotEmpty()) {
                        if (BleAgreement.instance.modifyAdvName(it)) {
                            modifyAdvName = it
                        }
                    } else {
                        ToastUtils.instance.toastInfo("请输入名称!")
                    }
                }
                ?.showDialog()
        }

        binding.btModifyPid.setOnClickListener {
            mInputTextDialog?.setTitleText("请输入PID(长度4)")
                ?.setEditTextMaxInputLength(4)
                ?.setDefText("")
                ?.setEditTextInputType(InputTextDialog.TEXT_INPUT_TYPE_EN_TEXT)
                ?.setOnDialogListener {
                    if (it.isNotEmpty()) {
                        BleAgreement.instance.modifyPid(it)
                    } else {
                        ToastUtils.instance.toastInfo("请输入PID!")
                    }
                }
                ?.showDialog()
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
        handler.postDelayed(readRssiRunnable, readRssiTime)
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
                for ((index, rssi) in rssiList.withIndex()) {
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

    override fun onNotificationCallback(
        bleDevice: BLEDevice,
        uuid: String,
        bytes: ByteArray
    ) {
        if (uuid.uppercase(Locale.ENGLISH) == BleConstant.NOTIF_UUID_TEST1.uppercase(Locale.ENGLISH)) {
            LOGUtils.i("MSG ==> bytes:${ByteUtils.toHexString(bytes)}")
            if (bytes.size >= 6) {
                val cmd = ByteUtils.byteArrayToInt(byteArrayOf(bytes[2], bytes[3]))
                val result = ByteUtils.byteToInt(bytes[4])
                runOnUiThread {
                    when (cmd) {
                        0x8001, 0x8002 -> {//修改广播间隔,修改连接间隔
                            when (result) {
                                0 -> {
                                    ToastUtils.instance.toastInfo("修改成功!")
                                }

                                1 -> {
                                    ToastUtils.instance.toastInfo("参数范围不对!")
                                }

                                2 -> {
                                    ToastUtils.instance.toastInfo("参数长度不对!")
                                }

                                else -> {
                                    ToastUtils.instance.toastInfo("其他错误!")
                                }
                            }
                        }

                        0x8003 -> {//修改广播名称
                            if (result == 0) {
                                deviceName = modifyAdvName
                                binding.lyTop.tvDevName.text = deviceName
                                ToastUtils.instance.toastInfo("修改成功!")
                            } else {
                                ToastUtils.instance.toastInfo("修改失败!")
                            }
                        }

                        0x8004 -> {//修改PID
                            if (result == 0) {
                                ToastUtils.instance.toastInfo("修改成功!")
                            } else {
                                ToastUtils.instance.toastInfo("修改失败!")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BLEManager.getInstance().removeReadRssiCallback(this)
        handler.removeCallbacksAndMessages(null)
    }
}