package com.linkiing.test.tool.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.linkiing.ble.BLEDevice
import com.linkiing.ble.api.BLEScanner
import com.linkiing.ble.log.LOGUtils
import com.linkiing.test.tool.R
import com.linkiing.test.tool.databinding.BleDevListItemBinding
import com.linkiing.test.tool.utlis.ByteUtils

class BleListAdapter(private var context: Context) :
    RecyclerView.Adapter<BleListAdapter.MyHolder>() {
    val devList = mutableListOf<BLEDevice>()
    private var sortTime = 0L
    private var btListener: (BLEDevice) -> Unit = {}
    private var itemListener: (BLEDevice) -> Unit = {}

    init {
//        setAndReverseDevList()
        addDevList()
        sortItem {}
    }

    fun update() {
//        setAndReverseDevList()
        addDevList()
        sortItem {
            notifyDataSetChanged()
        }
    }

    fun clearItem() {
        BLEScanner.getInstance().devListClear()
        devList.clear()
        notifyDataSetChanged()
    }

    fun setOnBtListener(btListener: (BLEDevice) -> Unit) {
        this.btListener = btListener
    }

    fun setOnItemListener(itemListener: (BLEDevice) -> Unit) {
        this.itemListener = itemListener
    }

    private fun setAndReverseDevList() {
        devList.clear()
        val list = BLEScanner.getInstance().allDevList
        for (i in 0 until list.size) {
            devList.add(list[list.size - 1 - i])
        }
    }

    private fun addDevList() {
        devList.clear()
        devList.addAll(BLEScanner.getInstance().allDevList)
    }

    //排序
    private fun sortItem(endListener: () -> Unit) {
        val time = System.currentTimeMillis()
        if (time - sortTime > 500L || (itemCount == 0 && devList.isNotEmpty())) {
            devList.sortByDescending { it.rssi }
            connectDevTop()
            sortTime = time
            endListener()
        }
    }

    //已连接设备置顶
    private fun connectDevTop() {
        val listConnectedDev = mutableListOf<BLEDevice>()
        val iterator = devList.iterator()
        while (iterator.hasNext()) {
            val device = iterator.next()
            if (device.isConnected) {
                iterator.remove()
                listConnectedDev.add(device)
            }
        }
        devList.addAll(0, listConnectedDev)
    }

    override fun getItemCount(): Int {
        return devList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(BleDevListItemBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val data = devList[position]
        myHolderBind(holder, data)
    }

    /**
     * 数据绑定
     */
    private fun myHolderBind(holder: MyHolder, data: BLEDevice) {
        holder.binding.tvDevName.text = if (data.deviceName.equals("")) {
            holder.binding.tvDevName.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.color_gray_
                )
            )
            "Null"
        } else {
            holder.binding.tvDevName.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.text_color
                )
            )
            data.deviceName
        }

        holder.binding.tvMac.text = data.deviceMac
        holder.binding.tvRiss.text = data.rssi.toString()
        if (data.isConnected) {
            holder.binding.tvBleStatus.text = context.resources.getText(R.string.connection_success)
            holder.binding.tvBleStatus.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.color_green
                )
            )
            holder.binding.btConnect.setBackgroundResource(R.drawable.shape_round_bt_bg1)
            holder.binding.btConnect.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.color_red
                )
            )
            holder.binding.btConnect.text = context.getString(R.string.text_disconnect)
        } else {
            holder.binding.tvBleStatus.text = context.resources.getText(R.string.connection_dis)
            holder.binding.tvBleStatus.setTextColor(ContextCompat.getColor(context, R.color.black))
            holder.binding.btConnect.setBackgroundResource(R.drawable.shape_round_bt_bg)
            holder.binding.btConnect.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.binding.btConnect.text = context.getString(R.string.text_connect)
        }

        var uuidStr = ""
        for (parcelUuid in data.parcelUuids) {
            val uuidString = parcelUuid.toString()
            if (uuidString.length > 8) {
                val str = uuidString.substring(4, 8)
                uuidStr = if (uuidStr == "") {
                    str
                } else {
                    "$uuidStr,$str"
                }
            }
        }
        holder.binding.tvBleUuids.text = if (uuidStr == "") {
            ""
        } else {
            "uuids:$uuidStr"
        }

        holder.binding.btConnect.setOnClickListener {
            btListener(data)
        }

        holder.binding.rlItem.setOnClickListener {
            itemListener(data)
        }
    }

    inner class MyHolder(val binding: BleDevListItemBinding) : RecyclerView.ViewHolder(binding.root)
}