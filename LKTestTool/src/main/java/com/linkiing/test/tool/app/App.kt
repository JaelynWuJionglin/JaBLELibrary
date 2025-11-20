package com.linkiing.test.tool.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Process
import com.linkiing.ble.api.BLEConfig
import com.linkiing.ble.api.BLEManager
import com.linkiing.ble.log.LOGUtils
import com.linkiing.test.tool.sp.SpHelper
import com.linkiing.test.tool.ui.ScanActivity
import com.linkiing.test.tool.utlis.ToastUtils
import java.util.Stack
import kotlin.system.exitProcess

class App : Application() {
    private val activityStack = Stack<Activity>()

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

        //SpHelper
        SpHelper.instance.initContext(this)

        //BLE初始化
        BLEManager.getInstance()
            .setBleConfig(BLEConfig.getBLEConfig(1))
            .init(this)

        ToastUtils.instance.init(this)
    }

    fun addAct(act: Activity?) {
        if (!activityStack.contains(act)) {
            activityStack.add(act)
        }
    }

    fun removeAct(act: Activity?) {
        if (activityStack.contains(act)) {
            activityStack.remove(act)
        }
    }

    /**
     * 结束所有的activity
     */
    private fun finishAllActivity() {
        while (!activityStack.empty()) {
            val pop = activityStack.pop()
            if (pop != null && !pop.isFinishing && !pop.isDestroyed) {
                pop.finish()
            }
        }
    }

    /**
     * 重启APP
     */
    fun reStartApp() {
        finishAllActivity()
        val intent = Intent(this, ScanActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        this.startActivity(intent)

        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}