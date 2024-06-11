package com.linkiing.test.tool.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.linkiing.test.tool.R
import com.linkiing.test.tool.databinding.TitleBarLayoutBinding

class TitleBar(private val context: Context, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val binding = TitleBarLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    private var titleText = ""
    private var startText = ""
    private var endText = ""
    private var endIvId = 0
    private var startIvId = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TitleBar)
        titleText = typedArray.getString(R.styleable.TitleBar_titleText) ?: ""
        startText = typedArray.getString(R.styleable.TitleBar_startText) ?: ""
        endText = typedArray.getString(R.styleable.TitleBar_endText) ?: ""
        endIvId = typedArray.getResourceId(R.styleable.TitleBar_endIvId, 0)
        startIvId = typedArray.getResourceId(R.styleable.TitleBar_startIvId, 0)

        val bgColor = typedArray.getColor(R.styleable.TitleBar_bgColor, 0)
        if (bgColor != 0) {
            setBackgroundColor(bgColor)
        }

        typedArray.recycle()

        build()
    }

    fun setTitleText(text: String): TitleBar {
        this.titleText = text
        return this
    }

    fun setTitleText(textId: Int): TitleBar {
        this.titleText = context.resources.getString(textId)
        return this
    }

    fun setStartText(text: String): TitleBar {
        this.startText = text
        return this
    }

    fun setStartText(textId: Int): TitleBar {
        this.startText = context.resources.getString(textId)
        return this
    }

    fun setEndText(text: String): TitleBar {
        this.endText = text
        return this
    }

    fun setEndText(textId: Int): TitleBar {
        this.endText = context.resources.getString(textId)
        return this
    }

    fun setStartIv(ivId: Int): TitleBar {
        this.startIvId = ivId
        return this
    }

    fun setEndIv(ivId: Int): TitleBar {
        this.endIvId = ivId
        return this
    }

    fun setStartOnClickListener(onClickListener: OnClickListener): TitleBar {
        binding.lrStart.setOnClickListener(onClickListener)
        return this
    }

    fun setEndOnClickListener(onClickListener: OnClickListener): TitleBar {
        binding.lrEnd.setOnClickListener(onClickListener)
        return this
    }

    fun setTitleProVisibility(isVisibility: Boolean) {
        binding.titlePro.visibility = if (isVisibility) {
            VISIBLE
        } else {
            GONE
        }
    }

    fun build() {
        if (TextUtils.isEmpty(titleText)) {
            binding.tvTitle.text = ""
        } else {
            binding.tvTitle.text = titleText
        }

        if (TextUtils.isEmpty(startText)) {
            binding.tvStart.text = ""
        } else {
            binding.tvStart.text = startText
        }

        if (TextUtils.isEmpty(endText)) {
            binding.tvEnd.text = ""
        } else {
            binding.tvEnd.text = endText
        }

        if (startIvId == 0) {
            binding.ivStart.visibility = GONE
        } else {
            binding.ivStart.visibility = VISIBLE
            binding.ivStart.setBackgroundResource(startIvId)
        }

        if (endIvId == 0) {
            binding.ivEnd.visibility = GONE
        } else {
            binding.ivEnd.visibility = VISIBLE
            binding.ivEnd.setBackgroundResource(endIvId)
        }
    }
}