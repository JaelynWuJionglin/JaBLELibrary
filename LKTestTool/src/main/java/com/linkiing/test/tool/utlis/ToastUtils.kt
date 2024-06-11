package com.linkiing.test.tool.utlis

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.widget.Toast
import androidx.annotation.StringRes

class ToastUtils {
    private lateinit var context: Context
    private lateinit var handler: Handler
    private var mToast: Toast? = null

    companion object {
        val instance: ToastUtils by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ToastUtils()
        }
    }

    fun init(context: Context) {
        this.context = context
        this.handler = Handler(Looper.getMainLooper())
    }

    fun toastInfo(@StringRes msgId: Int) {
        handler.post {
            setToast(context.resources.getString(msgId))?.show()
        }
    }

    fun toastInfo(msgStr: String) {
        handler.post {
            setToast(msgStr)?.show()
        }
    }

    private fun setToast(msgStr: String): Toast? {
        if (TextUtils.isEmpty(msgStr)) {
            return null
        }
        if (mToast != null) {
            mToast!!.cancel()
        }
        mToast = Toast.makeText(context, msgStr, Toast.LENGTH_SHORT)
        return mToast
    }
}