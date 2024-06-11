package com.linkiing.test.tool.dialog

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import androidx.annotation.StringRes
import com.linkiing.test.tool.base.BaseFullDialog
import com.linkiing.test.tool.databinding.InputTextDialogLayoutBinding

class InputTextDialog(context: Context) :
    BaseFullDialog<InputTextDialogLayoutBinding>(context) {
    private var listener: (String) -> Unit = {}
    private var defText = ""
    private var titleText = ""
    private var maxInputLength = 0
    private var isTypeNumber = false

    /**
     * 设置按钮的显示内容和监听
     */
    fun setOnDialogListener(listener: (String) -> Unit): InputTextDialog {
        this.listener = listener
        return this
    }

    /**
     * 设置title text
     */
    fun setTitleText(@StringRes id: Int): InputTextDialog {
        titleText = context.resources.getString(id)
        binding.tvTitle.text = titleText
        return this
    }

    fun setTitleText(text: String): InputTextDialog {
        titleText = text
        binding.tvTitle.text = titleText
        return this
    }

    /**
     * 设置默认文字
     */
    fun setDefText(str: String): InputTextDialog {
        defText = str
        binding.etInput.setText(defText)
        binding.etInput.setSelection(binding.etInput.text?.length ?: 0)
        binding.etInput.requestFocus()
        return this
    }

    /**
     * 设置输入长度限制
     */
    fun setEditTextMaxInputLength(maxInputLength: Int): InputTextDialog {
        if (maxInputLength > 0) {
            this.maxInputLength = maxInputLength
        } else {
            this.maxInputLength = 0
        }
        setEditTextInputLengthFilter()
        return this
    }

    /**
     * 设置限制输入数字
     */
    fun setEditTextInputTypeNumber(isTypeNumber: Boolean): InputTextDialog{
        this.isTypeNumber = isTypeNumber
        if (isTypeNumber) {
            binding.etInput.inputType = InputType.TYPE_CLASS_NUMBER
        }
        return this
    }

    /**
     * show
     */
    fun showDialog() {
        dismissDialog()
        super.show()
    }

    fun dismissDialog() {
        if (isShowing) {
            super.dismiss()
        }
    }

    override fun initBind(): InputTextDialogLayoutBinding {
        return InputTextDialogLayoutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (titleText != "") {
            binding.tvTitle.text = titleText
        }

        setEditTextInputLengthFilter()

        if (isTypeNumber) {
            binding.etInput.inputType = InputType.TYPE_CLASS_NUMBER
        }

        if (defText != "") {
            binding.etInput.setText(defText)
            binding.etInput.setSelection(binding.etInput.text?.length ?: 0)
            binding.etInput.requestFocus()
        }

        //设置确定按钮被点击后，向外界提供监听
        binding.tvConfirm.setOnClickListener {
            dismissDialog()
            val text: String = binding.etInput.text?.toString() ?: ""
            listener(text)
        }
        //设置取消按钮被点击后，向外界提供监听
        binding.tvCancel.setOnClickListener {
            dismissDialog()
        }
    }

    private fun setEditTextInputLengthFilter(){
        if (maxInputLength > 0) {
            binding.etInput.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxInputLength))
        } else {
            binding.etInput.filters = arrayOf<InputFilter>()
        }
    }
}