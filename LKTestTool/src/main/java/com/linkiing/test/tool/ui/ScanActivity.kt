package com.linkiing.test.tool.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.linkiing.ble.BLEDevice
import com.linkiing.ble.api.BLEConfig
import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.api.BLEScanner
import com.linkiing.ble.callback.BLEConnectStatusCallback
import com.linkiing.ble.callback.BLEPermissionCallback
import com.linkiing.ble.callback.BLEScanDeviceCallback
import com.linkiing.ble.lkagm.LKPackageAgm
import com.linkiing.ble.log.FileJaUtils
import com.linkiing.ble.log.LOGUtils
import com.linkiing.ble.utils.BLEConstant
import com.linkiing.ble.utils.BLEPermissionsUtils
import com.linkiing.test.tool.R
import com.linkiing.test.tool.adapter.BleListAdapter
import com.linkiing.test.tool.base.BaseActivity
import com.linkiing.test.tool.bean.AcExtra
import com.linkiing.test.tool.ble.BleConstant
import com.linkiing.test.tool.ble.BluAgreement
import com.linkiing.test.tool.databinding.ActivityScanBinding
import com.linkiing.test.tool.dialog.RoundProcessDialog
import com.linkiing.test.tool.sp.SpHelper
import com.linkiing.test.tool.utlis.ByteUtils
import com.linkiing.test.tool.utlis.ExcelUtils
import com.linkiing.test.tool.utlis.ToastUtils
import com.linkiing.test.tool.view.WcLinearLayoutManager

class ScanActivity : BaseActivity<ActivityScanBinding>(), BLEScanDeviceCallback,
    BLEConnectStatusCallback {
    private lateinit var bleScanner: BLEScanner
    private lateinit var adapter: BleListAdapter
    private lateinit var dialog: RoundProcessDialog
    private var isAvailable = false
    private var isScanning = false
    private var launcherFilter: ActivityResultLauncher<Intent>? = null
    private var startScanTime = 0L

    override fun initBind(): ActivityScanBinding {
        return ActivityScanBinding.inflate(layoutInflater)
    }

    override fun initView() {
        isAvailable = true

        dialog = RoundProcessDialog(this)

        initRecyclerView()
        initRefreshLayout()

        BLEManager.getInstance().addBleConnectStatusCallback(this)

        launcherFilter =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    setBleScannerFilter()
                    clearItem()

                    bleScanner.startScan(false)
                    uiIsScan(true)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        initScanDevice()
        listUpdate()
    }

    private fun initRefreshLayout() {
        binding.refreshLayout.setEnableLoadMoreWhenContentNotFull(false)
        binding.refreshLayout.setOnRefreshListener {
            clearItem()

            if (!isScanning) {
                bleScanner.startScan(false)
                uiIsScan(true)
            }

            binding.refreshLayout.finishRefresh(true)
        }
    }

    override fun initLister() {
        binding.titleBar.setEndOnClickListener {
            if (isScanning) {
                bleScanner.stopScan()
                uiIsScan(false)
            } else {
                clearItem()
                bleScanner.startScan(false)
                uiIsScan(true)
            }
        }

        adapter.setOnBtListener { bleDevice ->
            if (BLEManager.getInstance().isConnectDevice(bleDevice.deviceMac)) {
                BLEManager.getInstance().disconnectDevice(bleDevice.deviceMac)
                listUpdate()
            } else {
                //连接设备之前先停止搜索设备
                BLEScanner.getInstance().stopScan()
                uiIsScan(false)

                if (BLEManager.getInstance()
                        .setBleConfig(BLEConfig.getBLEConfig(1))
                        .setConnectOutTime(30 * 1000)
                        .connectDevice(bleDevice)
                ) {
                    dialog.showDialog(R.string.text_wait)
                } else {
                    ToastUtils.instance.toastInfo(resources.getString(R.string.connected_dev))
                }
            }
        }

        adapter.setOnItemListener {
            if (it.isConnected) {
                bleScanner.stopScan()
                goActivity(
                    ConnectedActivity::class.java, false,
                    AcExtra("deviceName", it.deviceName),
                    AcExtra("deviceMac", it.deviceMac)
                )
            }
        }

        binding.topItem.tvFilter.setOnClickListener {
            bleScanner.stopScan()
            uiIsScan(false)
            launcherFilter?.launch(Intent(this, FilterActivity::class.java))
        }

        //设置
        binding.titleBar.setStartOnClickListener {
            goActivity(SettingActivity::class.java, false)
        }

        //自动连接测试
        binding.topItem.tvListNumber.setOnClickListener {
            bleScanner.stopScan()
            uiIsScan(false)

            BLEPermissionsUtils.blePermissions(this, object : BLEPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    goActivity(ConnectTestActivity::class.java, false)
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    ToastUtils.instance.toastInfo("Permission Denied。")
                }
            })
        }

        //导出搜索设备测试数据
        binding.topItem.tvListNumber.setOnLongClickListener {
            if (adapter.devList.isNotEmpty()) {

                //停止搜索
                BLEScanner.getInstance().stopScan()
                uiIsScan(false)

                ExcelUtils.exportScanDeviceTest(this, adapter.devList, startScanTime) {
                    runOnUiThread {
                        FileJaUtils.shareFile(
                            this,
                            it,
                            getString(R.string.test_share_scan_test_data)
                        )
                    }
                }
            }
            false
        }
    }

    /**
     * 初始化RecyclerView
     */
    private fun initRecyclerView() {
        adapter = BleListAdapter(this)
        val manager = WcLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = manager
        binding.recyclerView.adapter = adapter

        //去刷新动画
        if (binding.recyclerView.itemAnimator != null) {
            binding.recyclerView.itemAnimator!!.addDuration = 0
            binding.recyclerView.itemAnimator!!.changeDuration = 0
            binding.recyclerView.itemAnimator!!.moveDuration = 0
            binding.recyclerView.itemAnimator!!.removeDuration = 0
            (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations =
                false
        }
    }

    private fun initScanDevice() {
        bleScanner = BLEScanner.getInstance()
        bleScanner.addScanDeviceCallback(this)
        bleScanner.setScanTime(0)
        setBleScannerFilter()
        bleScanner.startScan(false)
        uiIsScan(true)
    }

    private fun setBleScannerFilter() {
        bleScanner.setFilterMacStr(SpHelper.instance.getFilterMac())
        bleScanner.setFilterNameStr(SpHelper.instance.getFilterName())
        bleScanner.setFilterServiceUuid(SpHelper.instance.getFilterUUID())
        bleScanner.setFilterRecord(
            arrayListOf(
                ByteUtils.hexStringToBytes(SpHelper.instance.getFilterRecord1()),
                ByteUtils.hexStringToBytes(SpHelper.instance.getFilterRecord2()),
                ByteUtils.hexStringToBytes(SpHelper.instance.getFilterRecord3())
            )
        )
        bleScanner.setFilterRssi(-SpHelper.instance.getFilterRssi())
    }

    private fun uiIsScan(isScan: Boolean) {
        this.isScanning = isScan
        if (isScanning) {
            binding.titleBar.setEndText(getString(R.string.text_stop)).build()
            binding.titleBar.setTitleProVisibility(true)
            startScanTime = System.currentTimeMillis()
        } else {
            binding.titleBar.setEndText(getString(R.string.text_scan)).build()
            binding.titleBar.setTitleProVisibility(false)
        }
    }

    override fun onBLEConnectStatus(bleDevice: BLEDevice, connectStatus: Int) {
        runOnUiThread {
            when (connectStatus) {
                BLEConstant.BLE_STATUS_CONNECTED -> {
                    //连接上设备
                    Log.d("BLE_TAG", "--------------->BLE_STATUS_CONNECTED")

                    //添加设备通知数据监听
                    LKPackageAgm.instance.addDeviceNotificationListener(
                        bleDevice,
                        BleConstant.NOT_UUID,
                        BluAgreement.instance
                    )

                    dialog.dismissDialog()

                    listUpdate()

                    if (adapter.itemCount > 0) {
                        binding.recyclerView.scrollToPosition(0)
                    }
                }

                BLEConstant.BLE_STATUS_DISCONNECTED -> {
                    //断开连接
                    Log.d("BLE_TAG", "--------------->BLE_STATUS_DISCONNECTED")

                    //移除设备通知数据监听
                    LKPackageAgm.instance.removeDeviceNotificationListener(
                        bleDevice,
                        BleConstant.NOT_UUID
                    )

                    listUpdate()
                }

                BLEConstant.BLE_STATUS_CONNECT_TIME_OUT -> {
                    //连接超时
                    Log.d("BLE_TAG", "--------------->BLE_STATUS_CONNECT_TIME_OUT")
                    dialog.dismissDialog()
                }

                BLEConstant.BLE_STATUS_CONNECT_FAIL -> {
                    //连接失败
                    Log.d("BLE_TAG", "--------------->BLE_STATUS_CONNECT_FAIL")
                    dialog.dismissDialog()
                }
            }
        }
    }

    private fun listUpdate() {
        adapter.update()
        binding.topItem.tvListNumber.text = "Num:${adapter.itemCount}"
    }

    private fun clearItem() {
        adapter.clearItem()
        binding.topItem.tvListNumber.text = "Num:${adapter.itemCount}"
    }

    @SuppressLint("SetTextI18n")
    override fun onScanDevice(bleDevice: BLEDevice) {
        if (!isAvailable) {
            return
        }
        LOGUtils.v("deviceMac:${bleDevice.deviceMac}  name:${bleDevice.deviceName}  scanRecord:${ByteUtils.toHexString(bleDevice.scanRecord,",")}")
        runOnUiThread {
            listUpdate()
        }
    }

    override fun onScanStop() {
        super.onScanStop()
        uiIsScan(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        isAvailable = false
        bleScanner.stopAndClear()
        bleScanner.removeScanDeviceCallback(this)
        BLEManager.getInstance().removeBleConnectStatusCallback(this)
    }
}