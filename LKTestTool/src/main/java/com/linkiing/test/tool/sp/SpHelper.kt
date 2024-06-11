package com.linkiing.test.tool.sp

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.linkiing.ble.api.BLEConfig
import com.linkiing.ble.api.BLEManager
import com.linkiing.test.tool.bean.ConnectTestBean
import kotlin.math.max

class SpHelper private constructor() {
    private val gson = Gson()
    private var context: Context? = null

    companion object {
        private const val DEFAULT_NAME = "SP_HELPER_APP"
        private const val SP_FilterMac = "SP_FilterMac"
        private const val SP_FilterName = "SP_FilterName"
        private const val SP_FilterUUID = "SP_FilterUUID"
        private const val SP_FilterRecord1 = "SP_FilterRecord1"
        private const val SP_FilterRecord2 = "SP_FilterRecord2"
        private const val SP_FilterRecord3 = "SP_FilterRecord3"
        private const val SP_FilterRssi = "SP_FilterRssi"
        private const val SP_ConnectTestFilterDevList = "SP_ConnectTestFilterDevList"
        private const val SP_ConnectTestList = "SP_ConnectTestList"
        private const val SP_ConnectOutTime = "SP_ConnectOutTime"
        private const val SP_ConnectTestNumber = "SP_ConnectTestNumber"
        private const val SP_ConnectTestGdNumber = "SP_ConnectTestGdNumber"
        private const val SP_ConnectTestGdIndex = "SP_ConnectTestGdIndex"

        @JvmStatic
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SpHelper()
        }
    }

    fun initContext(context: Context?) {
        this.context = context
    }

    fun setFilterMac(str: String) {
        getSp()?.edit()?.putString(SP_FilterMac, str)?.apply()
    }

    fun getFilterMac(): String {
        return getSp()?.getString(SP_FilterMac, "") ?: ""
    }

    fun setFilterName(str: String) {
        getSp()?.edit()?.putString(SP_FilterName, str)?.apply()
    }

    fun getFilterName(): String {
        return getSp()?.getString(SP_FilterName, "") ?: ""
    }

    fun setFilterUUID(str: String) {
        getSp()?.edit()?.putString(SP_FilterUUID, str)?.apply()
    }

    fun getFilterUUID(): String {
        return getSp()?.getString(SP_FilterUUID, "") ?: ""
    }

    fun setFilterRecord1(str: String) {
        getSp()?.edit()?.putString(SP_FilterRecord1, str)?.apply()
    }

    fun getFilterRecord1(): String {
        return getSp()?.getString(SP_FilterRecord1, "") ?: ""
    }

    fun setFilterRecord2(str: String) {
        getSp()?.edit()?.putString(SP_FilterRecord2, str)?.apply()
    }

    fun getFilterRecord2(): String {
        return getSp()?.getString(SP_FilterRecord2, "") ?: ""
    }

    fun setFilterRecord3(str: String) {
        getSp()?.edit()?.putString(SP_FilterRecord3, str)?.apply()
    }

    fun getFilterRecord3(): String {
        return getSp()?.getString(SP_FilterRecord3, "") ?: ""
    }

    fun setFilterRssi(rssi: Int) {
        getSp()?.edit()?.putInt(SP_FilterRssi, rssi)?.apply()
    }

    fun getFilterRssi(): Int {
        return getSp()?.getInt(SP_FilterRssi, 100) ?: 100
    }

    fun setConnectOutTime(time: Long) {
        getSp()?.edit()?.putLong(SP_ConnectOutTime, time)?.apply()
    }

    fun getConnectOutTime(): Long {
        val time = getSp()?.getLong(SP_ConnectOutTime, 0) ?: 0
        return max(time, BLEManager.DEF_CONNECT_OUT_TIME)
    }

    fun setConnectTestNumber(number: Int) {
        getSp()?.edit()?.putInt(SP_ConnectTestNumber, number)?.apply()
    }

    fun getConnectTestNumber(): Int {
        val number = getSp()?.getInt(SP_ConnectTestNumber, 0) ?: 0
        return max(number, 1)
    }

    fun setConnectTestGdNumber(number: Int) {
        getSp()?.edit()?.putInt(SP_ConnectTestGdNumber, number)?.apply()
    }

    fun getConnectTestGdNumber(): Int {
        return getSp()?.getInt(SP_ConnectTestGdNumber, 0) ?: 0
    }

    fun addConnectTestGdIndex() {
        val gdNumber = getConnectTestGdNumber()
        var number = getConnectTestGdIndex()
        if (gdNumber <= 0) {
            number = 0
        } else {
            if (number > 0) {
                number++
            } else {
                number = 1
            }
            if (number > gdNumber) {
                number = 1
            }
        }
        getSp()?.edit()?.putInt(SP_ConnectTestGdIndex, number)?.apply()
    }

    fun reduceConnectTestGdIndex() {
        var number = getConnectTestGdIndex()
        number--
        if (number < 0) {
            number = 0
        }
        getSp()?.edit()?.putInt(SP_ConnectTestGdIndex, number)?.apply()
    }

    fun resetConnectTestGdIndex() {
        getSp()?.edit()?.putInt(SP_ConnectTestGdIndex, 0)?.apply()
    }

    fun getConnectTestGdIndex(): Int {
        return getSp()?.getInt(SP_ConnectTestGdIndex, 0) ?: 0
    }

    fun setConnectTestBeanToList(connectTestBean: ConnectTestBean) {
        val list = getConnectTestList()
        for (bean in list) {
            if (bean.devMac == connectTestBean.devMac && bean.testResult == ConnectTestBean.TEST_RESULT_TEST) {
                bean.copyData(connectTestBean)
                getSp()?.edit()?.putString(SP_ConnectTestList, gson.toJson(list))?.apply()
                return
            }
        }
        list.add(connectTestBean)
        getSp()?.edit()?.putString(SP_ConnectTestList, gson.toJson(list))?.apply()
    }

    fun removeConnectTestBeanToList(connectTestBean: ConnectTestBean) {
        val list = getConnectTestList()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val bean = iterator.next()
            if (bean.devMac == connectTestBean.devMac) {
                iterator.remove()
                break
            }
        }
        getSp()?.edit()?.putString(SP_ConnectTestList, gson.toJson(list))?.apply()
    }

    fun getConnectTestList(): MutableList<ConnectTestBean> {
        val str = getSp()?.getString(SP_ConnectTestList, "") ?: ""
        return if (TextUtils.isEmpty(str)) {
            mutableListOf()
        } else {
            gson.fromJson(str, object : TypeToken<MutableList<ConnectTestBean>>() {}.type)
        }
    }

    fun clearConnectTestData() {
        getSp()?.edit()?.remove(SP_ConnectTestList)?.apply()
    }

    fun addConnectTestFilterDev(macAddress: String) {
        if (TextUtils.isEmpty(macAddress)) {
            return
        }
        val list = getConnectTestFilterDevList()
        for (mac in list) {
            if (mac == macAddress) {
                return
            }
        }
        list.add(macAddress)
        getSp()?.edit()?.putString(SP_ConnectTestFilterDevList, gson.toJson(list))?.apply()
    }

    fun isConnectTestFilterDev(macAddress: String): Boolean {
        if (TextUtils.isEmpty(macAddress)) {
            return true
        }
        val list = getConnectTestFilterDevList()
        for (mac in list) {
            if (mac == macAddress) {
                return true
            }
        }
        return false
    }

    fun removeConnectTestFilters(macList: MutableList<String>) {
        if (macList.isEmpty()) {
            return
        }
        val list = getConnectTestFilterDevList()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val mac = iterator.next()
            val removeIterator = macList.iterator()
            while (removeIterator.hasNext()) {
                val removeMac = removeIterator.next()
                if (removeMac == mac) {
                    iterator.remove()
                    removeIterator.remove()
                    break
                }
            }
        }
        getSp()?.edit()?.putString(SP_ConnectTestFilterDevList, gson.toJson(list))?.apply()
    }

    fun clearConnectTestFilterDevList() {
        getSp()?.edit()?.remove(SP_ConnectTestFilterDevList)?.apply()
    }

    private fun getConnectTestFilterDevList(): MutableList<String> {
        val str = getSp()?.getString(SP_ConnectTestFilterDevList, "")
        return if (TextUtils.isEmpty(str)) {
            mutableListOf()
        } else {
            gson.fromJson(str, object : TypeToken<MutableList<String>>() {}.type)
        }
    }

    private fun getSp(): SharedPreferences? {
        return context?.getSharedPreferences(DEFAULT_NAME, Context.MODE_PRIVATE)
    }
}