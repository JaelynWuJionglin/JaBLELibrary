package com.linkiing.test.tool.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.linkiing.ble.api.BLEScanner
import com.linkiing.ble.log.LOGUtils
import com.linkiing.test.tool.R
import com.linkiing.test.tool.bean.ConnectTestBean
import com.linkiing.test.tool.databinding.ConnectTestListItemBinding
import com.linkiing.test.tool.sp.SpHelper

class ConnectTestListAdapter(private var context: Context) :
    RecyclerView.Adapter<ConnectTestListAdapter.MyHolder>() {
    private var devList = mutableListOf<ConnectTestBean>()
    private var gdIndex = 0

    init {
        setAndReverseDevList(true)
        this.gdIndex = getGdIndex()
    }

    fun update() {
        setAndReverseDevList(false)
        this.gdIndex = getGdIndex()
        notifyDataSetChanged()
    }

    fun getDeviceSizeByResult(testResult: Int): Int {
        var size = 0
        for (bean in devList) {
            if (bean.testResult == testResult) {
                size++
            }
        }
        return size
    }

    fun getTestDevSize(): Int {
        return devList.distinctBy { it.devMac }.size
    }

    private fun getGdIndex(): Int {
        return SpHelper.instance.getConnectTestGdIndex() - 1
    }

    private fun setAndReverseDevList(isInit: Boolean) {
        devList.clear()
        val list = SpHelper.instance.getConnectTestList()

        if (isInit) {
            //删除正在测试中的设备
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                val bean = iterator.next()
                if (bean.testResult == ConnectTestBean.TEST_RESULT_TEST) {
                    SpHelper.instance.removeConnectTestFilters(mutableListOf(bean.devMac))
                    iterator.remove()
                    break
                }
            }
        }

        for (i in 0 until list.size) {
            devList.add(list[list.size - 1 - i])
        }
    }

    override fun getItemCount(): Int {
        return devList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            ConnectTestListItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val data = devList[position]

        holder.binding.tvName.text = data.devName
        holder.binding.tvMac.text = data.devMac
        holder.binding.tvRssi.text = data.rssi.toString()
        holder.binding.tvXq.text =
            "${data.testConnectSusNumber}/${data.connectTestNumber}次 | ${data.connectOutTime}秒"
        holder.binding.tvResult.text = when (data.testResult) {
            ConnectTestBean.TEST_RESULT_TEST -> {
                setItemTextColor(holder, R.color.color_blue_hint, R.color.color_blue_hint)
                setItemTextBold(holder, true)
                "TEST..."
            }

            ConnectTestBean.TEST_RESULT_OK -> {
                setItemTextColor(holder, R.color.text_color, R.color.color_green)
                setItemTextBold(holder, false)
                "OK"
            }

            ConnectTestBean.TEST_RESULT_FAIL -> {
                setItemTextColor(holder, R.color.color_red, R.color.color_red)
                setItemTextBold(holder, false)
                "NO"
            }

            ConnectTestBean.TEST_RESULT_CON_FAIL -> {
                setItemTextColor(holder, R.color.color_yellow, R.color.color_yellow)
                setItemTextBold(holder, false)
                "C-ER"
            }

            else -> {
                setItemTextColor(holder, R.color.color_yellow, R.color.color_yellow)
                setItemTextBold(holder, false)
                "--"
            }
        }

        if (position == gdIndex) {
            holder.binding.viewGeduan.visibility = View.VISIBLE
            holder.binding.tvGdIndex.text =
                if (gdIndex + 1 == SpHelper.instance.getConnectTestGdNumber()
                    && devList.isNotEmpty()
                    && devList[0].testResult != ConnectTestBean.TEST_RESULT_TEST
                ) {
                    holder.binding.tvGdIndex.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.color_green
                        )
                    )
                    "OK"
                } else {
                    holder.binding.tvGdIndex.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.color_blue_hint
                        )
                    )
                    (gdIndex + 1).toString()
                }
            holder.binding.tvGdReset.setOnClickListener {
                SpHelper.instance.resetConnectTestGdIndex()
                update()
            }
        } else {
            holder.binding.viewGeduan.visibility = View.GONE
            holder.binding.tvGdIndex.text = ""
            holder.binding.tvGdReset.setOnClickListener(null)
        }
    }

    private fun setItemTextColor(
        holder: MyHolder,
        @ColorRes colorId: Int,
        @ColorRes resColorId: Int
    ) {
        holder.binding.tvName.setTextColor(
            ContextCompat.getColor(
                context,
                colorId
            )
        )
        holder.binding.tvMac.setTextColor(
            ContextCompat.getColor(
                context,
                colorId
            )
        )
        holder.binding.tvRssi.setTextColor(
            ContextCompat.getColor(
                context,
                colorId
            )
        )
        holder.binding.tvXq.setTextColor(
            ContextCompat.getColor(
                context,
                colorId
            )
        )
        holder.binding.tvResult.setTextColor(
            ContextCompat.getColor(
                context,
                resColorId
            )
        )
    }

    private fun setItemTextBold(holder: MyHolder, isBold: Boolean) {
        if (isBold) {
            holder.binding.tvName.setTypeface(null, Typeface.BOLD)
            holder.binding.tvMac.setTypeface(null, Typeface.BOLD)
            holder.binding.tvRssi.setTypeface(null, Typeface.BOLD)
            holder.binding.tvXq.setTypeface(null, Typeface.BOLD)
            holder.binding.tvResult.setTypeface(null, Typeface.BOLD)
        } else {
            holder.binding.tvName.setTypeface(null, Typeface.NORMAL)
            holder.binding.tvMac.setTypeface(null, Typeface.NORMAL)
            holder.binding.tvRssi.setTypeface(null, Typeface.NORMAL)
            holder.binding.tvXq.setTypeface(null, Typeface.NORMAL)
            holder.binding.tvResult.setTypeface(null, Typeface.NORMAL)
        }
    }

    inner class MyHolder(val binding: ConnectTestListItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}