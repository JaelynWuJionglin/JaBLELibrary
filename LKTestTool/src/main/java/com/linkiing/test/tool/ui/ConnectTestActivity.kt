package com.linkiing.test.tool.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.linkiing.ble.BLEDevice
import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.api.BLEScanner
import com.linkiing.ble.callback.BLEConnectStatusCallback
import com.linkiing.ble.callback.BLEScanDeviceCallback
import com.linkiing.ble.log.LOGUtils
import com.linkiing.ble.utils.BLEConstant
import com.linkiing.test.tool.R
import com.linkiing.test.tool.adapter.ConnectTestListAdapter
import com.linkiing.test.tool.base.BaseActivity
import com.linkiing.test.tool.bean.ConnectTestBean
import com.linkiing.test.tool.databinding.ActivityConnectTestBinding
import com.linkiing.test.tool.sp.SpHelper
import com.linkiing.test.tool.utlis.ByteUtils
import com.linkiing.test.tool.utlis.ToastUtils
import com.linkiing.test.tool.view.WcLinearLayoutManager

class ConnectTestActivity : BaseActivity<ActivityConnectTestBinding>(), BLEScanDeviceCallback,
    BLEConnectStatusCallback {
    private val handler = Handler(Looper.getMainLooper())
    private var adapter: ConnectTestListAdapter? = null
    private var bleScanner: BLEScanner? = null
    private var launcherFilter: ActivityResultLauncher<Intent>? = null
    private var scanBleDevice: BLEDevice? = null
    private var nowConnectTestBean = ConnectTestBean()
    private var isDevConnectTestIng = false //当前设备是否正在连接测试中
    private var isConnectIng = false //当前设备是否正在连接中
    private var isStartTest = false
    private var isScanStart = false

    override fun initBind(): ActivityConnectTestBinding {
        return ActivityConnectTestBinding.inflate(layoutInflater)
    }

    override fun initView() {
        initRecyclerView()
        initScanDevice()

        BLEManager.getInstance().addBleConnectStatusCallback(this)

        launcherFilter =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    setBleScannerFilter()
                }
            }
    }

    private fun initRecyclerView() {
        adapter = ConnectTestListAdapter(this)
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

        updateBottomUi()
    }

    override fun initLister() {
        binding.titleBar.setStartOnClickListener {
            finish()
        }

        binding.titleBar.setEndOnClickListener {
            if (isDevConnectTestIng || bleScanner?.isStartScan == true) {
                //测试进行中，停止测试
                testStop(0)
            } else {
                //测试停止，开始测试
                isStartTest = true
                testScanStart()
            }
        }

        binding.bottomItem.tvTestFailRetry.setOnClickListener {
            testStop(4)
            val list = mutableListOf<String>()
            for (connectTestBean in SpHelper.instance.getConnectTestList()) {
                if (connectTestBean.testResult != ConnectTestBean.TEST_RESULT_OK
                    && connectTestBean.testResult != ConnectTestBean.TEST_RESULT_TEST
                ) {
                    list.add(connectTestBean.devMac)
                }
            }
            SpHelper.instance.removeConnectTestFilters(list)
            ToastUtils.instance.toastInfo(getString(R.string.text_operation_successful))
        }
    }

    private fun initScanDevice() {
        bleScanner = BLEScanner.getInstance()
        bleScanner?.addScanDeviceCallback(this)
        bleScanner?.setScanTime(30 * 1000)
        setBleScannerFilter()

        //默认停止测试
        testStop(1)
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

    private fun isBleScannerFilterEmpty(): Boolean {
        return (TextUtils.isEmpty(SpHelper.instance.getFilterName())
                && TextUtils.isEmpty(SpHelper.instance.getFilterUUID())
                && TextUtils.isEmpty(SpHelper.instance.getFilterRecord1())
                && TextUtils.isEmpty(SpHelper.instance.getFilterRecord2())
                && TextUtils.isEmpty(SpHelper.instance.getFilterRecord3()))
    }

    override fun onBLEConnectStatus(bleDevice: BLEDevice, connectStatus: Int) {
        if (scanBleDevice == null) {
            return
        }
        if (TextUtils.isEmpty(scanBleDevice?.deviceMac)) {
            return
        }
        if (bleDevice.deviceMac != scanBleDevice?.deviceMac) {
            return
        }

        isConnectIng = false
        runOnUiThread {
            when (connectStatus) {
                BLEConstant.BLE_STATUS_CONNECTED,
                BLEConstant.BLE_STATUS_DISCONNECTED -> {

                    if (connectStatus == BLEConstant.BLE_STATUS_CONNECTED) {
                        nowConnectTestBean.testConnectSusNumber++
                    } else {
                        if (isDevConnectTestIng) {
                            //有设备正在连接中
                        }
                    }

                    if (nowConnectTestBean.testConnectSusNumber >= nowConnectTestBean.connectTestNumber) {
                        isDevConnectTestIng = false

                        nowConnectTestBean.testResult = ConnectTestBean.TEST_RESULT_OK
                        SpHelper.instance.setConnectTestBeanToList(nowConnectTestBean)
                        adapter?.update()
                        updateBottomUi()
                        if (isStartTest) {
                            testScanStart()
                        }
                    } else {
                        //继续连接测试
                        isDevConnectTestIng = true

                        SpHelper.instance.setConnectTestBeanToList(nowConnectTestBean)
                        adapter?.update()

                        BLEManager.getInstance().disConnectAllDevices()
                        handler.removeCallbacks(startConnectDeviceRunnable)
                        handler.postDelayed(startConnectDeviceRunnable, 1500)
                    }
                }

                BLEConstant.BLE_STATUS_CONNECT_TIME_OUT,
                BLEConstant.BLE_STATUS_CONNECT_FAIL -> {
                    isDevConnectTestIng = false

                    //连接失败
                    nowConnectTestBean.testResult = ConnectTestBean.TEST_RESULT_FAIL
                    SpHelper.instance.setConnectTestBeanToList(nowConnectTestBean)
                    adapter?.update()
                    updateBottomUi()
                    if (isStartTest) {
                        testScanStart()
                    }
                }

                else -> {
                    isDevConnectTestIng = false
                }
            }
        }
    }

    override fun onScanDevice(bleDevice: BLEDevice) {
        isScanStart = false

        LOGUtils.d(
            "onScanDevice ==> deviceMac:${bleDevice.deviceMac} " +
                    "deviceName:${bleDevice.deviceName} " +
                    "isDevConnectTestIng:$isDevConnectTestIng " +
                    "filterDevices:${filterDevices(bleDevice)} " +
                    "isStartTest:$isStartTest"
        )

        runOnUiThread {
            if (filterDevices(bleDevice) && isStartTest && !isDevConnectTestIng) {
                this.scanBleDevice = bleDevice

                LOGUtils.i("onScanDevice ==> filterDevices deviceMac:${bleDevice.deviceMac} deviceName:${bleDevice.deviceName}")

                isDevConnectTestIng = true
                bleScanner?.stopScan()

                //扫描到设备
                val outTime = SpHelper.instance.getConnectOutTime() / 1000
                val testNumber = SpHelper.instance.getConnectTestNumber()
                nowConnectTestBean = ConnectTestBean(
                    bleDevice.deviceName,
                    bleDevice.deviceMac,
                    bleDevice.rssi,
                    outTime,
                    testNumber
                )
                SpHelper.instance.setConnectTestBeanToList(nowConnectTestBean)
                SpHelper.instance.addConnectTestFilterDev(nowConnectTestBean.devMac)
                SpHelper.instance.addConnectTestGdIndex()

                //开始连接测试
                handler.removeCallbacks(startConnectDeviceRunnable)
                handler.postDelayed(startConnectDeviceRunnable, 10)

                //刷新列表
                adapter?.update()
            }
        }
    }

    override fun onScanStop() {
        super.onScanStop()
        LOGUtils.d("ConnectTestActivity onScanStop() isDevConnectTestIng:$isDevConnectTestIng isScanStart:$isScanStart")
        //搜索设备停止，且没有设备正在连接，则停止测试
        if (!isDevConnectTestIng && !isScanStart) {
            testStop(2)
        }
    }

    private val startConnectDeviceRunnable = Runnable {
        LOGUtils.i("startConnectDeviceRunnable ==> isConnectIng:$isConnectIng")
        if (scanBleDevice != null && !isConnectIng) {
            isConnectIng = true
            if (!BLEManager.getInstance()
                    .setConnectOutTime(nowConnectTestBean.connectOutTime * 1000)
                    .connectDevice(scanBleDevice)
            ) {
                //执行连接出错
                isDevConnectTestIng = false
                nowConnectTestBean.testResult = ConnectTestBean.TEST_RESULT_CON_FAIL
                SpHelper.instance.setConnectTestBeanToList(nowConnectTestBean)
                adapter?.update()
                updateBottomUi()
                testScanStart()
            }
        }
    }

    //过滤测试过的设备
    private fun filterDevices(bleDevice: BLEDevice): Boolean {
        return !SpHelper.instance.isConnectTestFilterDev(bleDevice.deviceMac)
    }

    private fun testScanStart() {
        if (isBleScannerFilterEmpty()) {
            ToastUtils.instance.toastInfo("请设置过滤条件!")
            launcherFilter?.launch(Intent(this, FilterActivity::class.java))
            return
        }
        if (isDevConnectTestIng) {
            return
        }
        isScanStart = true
        scanBleDevice = null
        binding.titleBar.setTitleProVisibility(true)
        binding.titleBar.setEndText(R.string.text_stop).build()

        isConnectIng = false
        BLEManager.getInstance().disConnectAllDevices()

        handler.removeCallbacks(testScanStartRunnable)
        handler.postDelayed(testScanStartRunnable, 500)
    }

    private val testScanStartRunnable = Runnable {
        bleScanner?.devListClear()
        bleScanner?.startScan(false)
    }

    private fun testStop(code: Int) {
        LOGUtils.d("testStop ==> code:$code")
        binding.titleBar.setTitleProVisibility(false)
        binding.titleBar.setEndText(R.string.text_start).build()

        scanBleDevice = null
        isStartTest = false
        isDevConnectTestIng = false
        bleScanner?.stopScan()

        isConnectIng = false
        BLEManager.getInstance().disConnectAllDevices()

        if (nowConnectTestBean.devMac != "") {
            if (nowConnectTestBean.testResult == ConnectTestBean.TEST_RESULT_TEST) {
                //删除未测试完成的数据
                SpHelper.instance.removeConnectTestBeanToList(nowConnectTestBean)
                SpHelper.instance.removeConnectTestFilters(mutableListOf(nowConnectTestBean.devMac))
                SpHelper.instance.reduceConnectTestGdIndex()

            }
            adapter?.update()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBottomUi() {
        binding.bottomItem.tvTestDevAll.text =
            "${getString(R.string.text_test_dev_size)}${adapter?.getTestDevSize()}"
        binding.bottomItem.tvTestSus.text =
            "${getString(R.string.text_success_p)}${adapter?.getDeviceSizeByResult(ConnectTestBean.TEST_RESULT_OK)}"
        binding.bottomItem.tvTestFail.text =
            "${getString(R.string.text_fail_p)}${adapter?.getDeviceSizeByResult(ConnectTestBean.TEST_RESULT_FAIL)}"
    }

    override fun onDestroy() {
        super.onDestroy()
        testStop(3)
        bleScanner?.removeScanDeviceCallback(this)
        BLEManager.getInstance().removeBleConnectStatusCallback(this)
    }
}