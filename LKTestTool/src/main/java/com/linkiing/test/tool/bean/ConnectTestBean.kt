package com.linkiing.test.tool.bean

import com.linkiing.test.tool.utlis.MyUtils

class ConnectTestBean(
    var devName: String = "",
    var devMac: String = "",
    var rssi: Int = 0,
    var connectOutTime: Long = 15, //单次连接超时时间
    var connectTestNumber: Int = 0 //测试成功，需要连接成功的测试次数
) {
    var startTestTime = MyUtils.long2TimeYMDHMS(System.currentTimeMillis())
    var testConnectSusNumber = 0 //当前成功连接的次数
    var testResult = TEST_RESULT_TEST

    companion object {
        const val TEST_RESULT_TEST = 0 //测试中
        const val TEST_RESULT_CON_FAIL = 1 //连接设备失败错误。
        const val TEST_RESULT_OK = 2 //测试通过
        const val TEST_RESULT_FAIL = 3 //测试不通过
    }

    fun copyData(data: ConnectTestBean) {
        this.devName = data.devName
        this.devMac = data.devMac
        this.rssi = data.rssi
        this.connectOutTime = data.connectOutTime
        this.connectTestNumber = data.connectTestNumber
        this.testConnectSusNumber = data.testConnectSusNumber
        this.startTestTime = data.startTestTime
        this.testResult = data.testResult
    }
}