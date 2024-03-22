package com.ble.demo.app

import android.app.Application
import com.linkiing.ble.api.BLEConfig
import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.log.LOGUtils

class App : Application() {

    companion object {
        private lateinit var mThis: App

        fun getInstance(): App {
            return mThis
        }
    }

    override fun onCreate() {
        super.onCreate()
        mThis = this

        //LOGUtils 初始化
        LOGUtils.init(this, isLog = true, isSave = true)

        //BLE初始化
        BLEManager.getInstance()
            .setBleConfig(BLEConfig.getBLEConfig(1))
            .setConnectOutTime(15 * 1000)
            .init(this)
    }
}