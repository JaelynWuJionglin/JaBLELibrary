package com.linkiing.test.tool.ui

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.linkiing.ble.callback.BLEPermissionCallback
import com.linkiing.ble.utils.BLEPermissionsUtils
import com.linkiing.test.tool.base.BaseActivity
import com.linkiing.test.tool.databinding.ActivityStartBinding
import com.linkiing.test.tool.utlis.ToastUtils

class StartActivity : BaseActivity<ActivityStartBinding>(), Handler.Callback {
    private val myHandler = Handler(Looper.getMainLooper(), this)

    override fun initBind(): ActivityStartBinding {
        return ActivityStartBinding.inflate(layoutInflater)
    }

    override fun initView() {
        val intent = intent
        if (!this.isTaskRoot) {
            if (intent != null) {
                val action = intent.action
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                    && Intent.ACTION_MAIN == action
                ) {
                    finish()
                    return
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        //BLEPermissionsUtils.setCheckGps(false)
        BLEPermissionsUtils.blePermissions(this, object : BLEPermissionCallback {
            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                myHandler.sendEmptyMessageDelayed(1, 1000)
            }

            override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                super.onDenied(permissions, never)
                ToastUtils.instance.toastInfo("Permission Denied。")
                finish()
            }

            override fun onOpenGpsDialogCancel() {
                super.onOpenGpsDialogCancel()
                finish()
            }
        })
    }

    override fun initLister() {}

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            1 -> {
                startActivity(Intent(this, ScanActivity::class.java))
                finish()
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        myHandler.removeCallbacksAndMessages(null)
    }
}