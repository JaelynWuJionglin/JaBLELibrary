package com.linkiing.test.tool.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.linkiing.test.tool.R
import com.linkiing.test.tool.base.BaseActivity
import com.linkiing.test.tool.databinding.ActivityFilterBinding
import com.linkiing.test.tool.sp.SpHelper

class FilterActivity : BaseActivity<ActivityFilterBinding>(), OnSeekBarChangeListener {

    override fun initBind(): ActivityFilterBinding {
        return ActivityFilterBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.filterName.tvName.text = "Name:"
        binding.filterName.etFilter.hint = resources.getString(R.string.text_input_filter_name)

        binding.filterMac.tvName.text = "Mac:"
        binding.filterMac.etFilter.hint = resources.getString(R.string.text_input_filter_mac)

        binding.filterUuid.tvName.text = "UUID:"
        binding.filterUuid.etFilter.hint = resources.getString(R.string.text_input_filter_uuid)

        binding.filterRecord1.tvName.text = "Record:"
        binding.filterRecord1.etFilter.hint = "FFEE"
        binding.filterRecord2.tvName.text = "Record:"
        binding.filterRecord2.etFilter.hint = "FFDD"
        binding.filterRecord3.tvName.text = "Record:"
        binding.filterRecord3.etFilter.hint = "FFCC"

        updateData()
    }

    override fun initLister() {
        binding.rssiSeekbar.setOnSeekBarChangeListener(this)

        binding.titleBar.setStartOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        binding.titleBar.setEndOnClickListener {
            saveFilter()
            setResult(Activity.RESULT_OK)
            finish()
        }

        binding.btReset.setOnClickListener {
            SpHelper.instance.setFilterMac("")
            SpHelper.instance.setFilterName("")
            SpHelper.instance.setFilterUUID("")
            SpHelper.instance.setFilterRecord1("")
            SpHelper.instance.setFilterRecord2("")
            SpHelper.instance.setFilterRecord3("")
            SpHelper.instance.setFilterRssi(50 + 50)

            updateData()
        }

        binding.filterName.ivClear.setOnClickListener {
            SpHelper.instance.setFilterName("")
            updateData()
        }

        binding.filterMac.ivClear.setOnClickListener {
            SpHelper.instance.setFilterMac("")
            updateData()
        }

        binding.filterUuid.ivClear.setOnClickListener {
            SpHelper.instance.setFilterUUID("")
            updateData()
        }

        binding.filterRecord1.ivClear.setOnClickListener {
            SpHelper.instance.setFilterRecord1("")
            updateData()
        }

        binding.filterRecord2.ivClear.setOnClickListener {
            SpHelper.instance.setFilterRecord2("")
            updateData()
        }

        binding.filterRecord3.ivClear.setOnClickListener {
            SpHelper.instance.setFilterRecord3("")
            updateData()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        binding.tvRssi.text = "-${progress + 50}"
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        if (seekBar != null) {
            val progress = seekBar.progress
            SpHelper.instance.setFilterRssi(progress + 50)
        }
    }

    private fun saveFilter() {
        val name = binding.filterName.etFilter.text?.toString() ?: ""
        SpHelper.instance.setFilterName(name)

        val macStr = binding.filterMac.etFilter.text?.toString() ?: ""
        SpHelper.instance.setFilterMac(macStr)

        val uuid = binding.filterUuid.etFilter.text?.toString() ?: ""
        SpHelper.instance.setFilterUUID(uuid)

        val record1 = binding.filterRecord1.etFilter.text?.toString() ?: ""
        SpHelper.instance.setFilterRecord1(record1)

        val record2 = binding.filterRecord2.etFilter.text?.toString() ?: ""
        SpHelper.instance.setFilterRecord2(record2)

        val record3 = binding.filterRecord3.etFilter.text?.toString() ?: ""
        SpHelper.instance.setFilterRecord3(record3)
    }

    @SuppressLint("SetTextI18n")
    private fun updateData(){
        binding.filterName.etFilter.setText(SpHelper.instance.getFilterName())
        binding.filterMac.etFilter.setText(SpHelper.instance.getFilterMac())
        binding.filterUuid.etFilter.setText(SpHelper.instance.getFilterUUID())
        binding.filterRecord1.etFilter.setText(SpHelper.instance.getFilterRecord1())
        binding.filterRecord2.etFilter.setText(SpHelper.instance.getFilterRecord2())
        binding.filterRecord3.etFilter.setText(SpHelper.instance.getFilterRecord3())

        binding.rssiSeekbar.progress = SpHelper.instance.getFilterRssi() - 50
        binding.tvRssi.text = "-${SpHelper.instance.getFilterRssi()}"
    }
}