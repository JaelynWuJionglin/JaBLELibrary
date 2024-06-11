package com.linkiing.test.tool.ui

import android.annotation.SuppressLint
import android.widget.SeekBar
import com.linkiing.ble.log.FileJaUtils
import com.linkiing.ble.log.LOGUtils
import com.linkiing.test.tool.BuildConfig
import com.linkiing.test.tool.R
import com.linkiing.test.tool.base.BaseActivity
import com.linkiing.test.tool.databinding.ActivitySettingBinding
import com.linkiing.test.tool.dialog.InputTextDialog
import com.linkiing.test.tool.sp.SpHelper
import com.linkiing.test.tool.utlis.ExcelUtils
import com.linkiing.test.tool.utlis.MyUtils
import com.linkiing.test.tool.utlis.ToastUtils

class SettingActivity : BaseActivity<ActivitySettingBinding>() {
    private var mInputTextDialog: InputTextDialog? = null

    override fun initBind(): ActivitySettingBinding {
        return ActivitySettingBinding.inflate(layoutInflater)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        binding.tvAppVer.text = "Linkiing Tool (V${BuildConfig.VERSION_NAME})"
        binding.btConnectTestNumber.text =
            "${getString(R.string.text_set_gd_number)}${SpHelper.instance.getConnectTestGdNumber()}"
        updateConUi()

        mInputTextDialog = InputTextDialog(this)
    }

    @SuppressLint("SetTextI18n")
    override fun initLister() {
        binding.titleBar.setStartOnClickListener {
            finish()
        }

        binding.btResetConnectTestData.setOnClickListener {
            SpHelper.instance.clearConnectTestData()
            SpHelper.instance.clearConnectTestFilterDevList()
            SpHelper.instance.resetConnectTestGdIndex()
            ToastUtils.instance.toastInfo(getString(R.string.text_reset_sus))
        }

        binding.btShareTestFile.setOnClickListener {
            binding.btResetConnectTestData.isEnabled = false
            ExcelUtils.exportExcelConnectTest(this) {
                runOnUiThread {
                    FileJaUtils.shareFile(this, it, getString(R.string.test_share_test_data))
                    binding.btResetConnectTestData.isEnabled = true
                }
            }
        }

        binding.btShareLog.setOnClickListener {
            binding.btShareLog.isEnabled = false
            LOGUtils.shareAppLogFile {
                runOnUiThread {
                    FileJaUtils.shareFile(this, it, getString(R.string.text_share_log))
                    binding.btShareLog.isEnabled = true
                }
            }
        }

        binding.btConnectTestNumber.setOnClickListener {
            mInputTextDialog?.setTitleText(getString(R.string.text_input_gd_number))
                ?.setDefText(SpHelper.instance.getConnectTestGdNumber().toString())
                ?.setEditTextInputTypeNumber(true)
                ?.setOnDialogListener {
                    val num = MyUtils.strOrInt(it)
                    SpHelper.instance.setConnectTestGdNumber(num)
                    binding.btConnectTestNumber.text = "${getString(R.string.text_set_gd_number)}$num"
                }
                ?.showDialog()

        }

        binding.seekBarNumber.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val number = 1 + progress
                binding.tvNumber.text = "${number}次"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            @SuppressLint("SetTextI18n")
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    val number = 1 + seekBar.progress
                    SpHelper.instance.setConnectTestNumber(number)
                    binding.tvNumber.text = "${number}次"
                }

            }
        })

        binding.seekBarTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val outTime = progress + 15
                binding.tvTime.text = "${outTime}秒"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            @SuppressLint("SetTextI18n")
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    val outTime = seekBar.progress + 15
                    SpHelper.instance.setConnectOutTime(outTime * 1000L)
                    binding.tvTime.text = "${outTime}秒"
                }

            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateConUi() {
        val testNumber = SpHelper.instance.getConnectTestNumber()
        binding.tvNumber.text = "${testNumber}次"
        binding.seekBarNumber.progress = testNumber - 1

        val connectOutTime = (SpHelper.instance.getConnectOutTime()) / 1000
        binding.tvTime.text = "${connectOutTime}秒"
        binding.seekBarTime.progress = connectOutTime.toInt() - 15
    }
}