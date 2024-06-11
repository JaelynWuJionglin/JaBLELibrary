package com.linkiing.test.tool.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.linkiing.test.tool.R
import com.linkiing.test.tool.utlis.ToastUtils

class RoundProcessDialog(private val context: Context) {
    private var mDialog: Dialog? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var toastMsgStr:String = ""

    private var timeOutCallbacks:(Boolean) -> Unit = {}
    private var isCallback:Boolean = false

    private var runnable: Runnable = Runnable {
        dismissDialog()
        if (toastMsgStr!=""){
            ToastUtils.instance.toastInfo(toastMsgStr)
        }
        if (isCallback){
            timeOutCallbacks(true)
        }
    }

    /**
     * @param isCanceled 点击返回键是否消失  ture 消失； false  不消失。
     */
    private fun showRoundProcessDialog(outTime: Long,textId: Int, isCanceled: Boolean) {
        showRoundProcessDialog(textId,isCanceled)

        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, outTime)
    }

    private fun showRoundProcessDialog(textId: Int, isCanceled: Boolean) {
        val contentView:View = View.inflate(context, R.layout.loading_process_dialog, null)
        val tvMsg:TextView = contentView.findViewById(R.id.tv_msg)
        tvMsg.text = context.resources.getString(textId)

        mDialog = AlertDialog.Builder(context, R.style.Dialog_bocop).create()
        if (isCanceled) {
            //dialog弹出后点击屏幕，dialog不消失；点击物理返回键dialog消失
            mDialog?.setCanceledOnTouchOutside(false)
        } else {
            //dialog弹出后点击屏幕或物理返回键，dialog不消失
            mDialog?.setCancelable(false)
        }
        if (mDialog?.isShowing!!){
            mDialog?.dismiss()
        }
        mDialog?.show()
        mDialog?.setContentView(contentView)
    }

    /**
     * showDialog
     */
    fun showDialog(textId: Int) {
        this.toastMsgStr = ""
        isCallback = false
        showRoundProcessDialog(textId,false)
    }

    fun showDialog(outTime: Int, textId: Int) {
        this.toastMsgStr = ""
        isCallback = false
        showRoundProcessDialog(outTime.toLong(),textId,false)
    }

    fun showDialog(outTime: Int, textId: Int, msgId: Int) {
        this.toastMsgStr = context.resources.getString(msgId)
        isCallback = false
        showRoundProcessDialog(outTime.toLong(),textId,false)
    }

    fun showDialog(outTime: Int, textId: Int, msgId: Int, isCanceled: Boolean) {
        this.toastMsgStr = context.resources.getString(msgId)
        isCallback = false
        showRoundProcessDialog(outTime.toLong(),textId,isCanceled)
    }

    fun showDialog(outTime: Int, textId: Int, isCanceled: Boolean) {
        this.toastMsgStr = ""
        isCallback = false
        showRoundProcessDialog(outTime.toLong(),textId,isCanceled)
    }

    fun showDialog(outTime: Int, textId: Int, msgId: Int, timeOutCallbacks:(Boolean) -> Unit) {
        this.toastMsgStr = context.resources.getString(msgId)
        this.timeOutCallbacks = timeOutCallbacks
        isCallback = true
        showRoundProcessDialog(outTime.toLong(),textId,false)
    }


    fun dismissDialog() {
        handler.removeCallbacks(runnable)
        if (mDialog!=null){
            if (mDialog!!.isShowing){
                mDialog!!.dismiss()
            }
        }
    }
}