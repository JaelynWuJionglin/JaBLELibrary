package com.linkiing.test.tool.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.linkiing.test.tool.app.App
import com.linkiing.test.tool.bean.AcExtra
import java.io.Serializable

abstract class BaseActivity<B : ViewBinding> : AppCompatActivity() {
    protected val binding: B by lazy { initBind() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.getInstance().addAct(this)
        setContentView(binding.root)
        initView()
        initLister()
    }

    /**
     * 初始化UI绑定类
     * @return xxxBind
     */
    protected abstract fun initBind(): B

    abstract fun initView()

    abstract fun initLister()

    protected fun goActivity(cls: Class<*>, isFinish: Boolean) {
        startActivity(Intent(this, cls))
        if (isFinish) {
            finish()
        }
    }

    protected fun goActivity(
        cls: Class<*>,
        isFinish: Boolean,
        vararg acExtras: AcExtra<String, Any>
    ) {
        val intent = Intent(this, cls)
        for (extra: AcExtra<String, Any> in acExtras) {
            when (extra.value) {
                is Int -> {
                    intent.putExtra(extra.key, (extra.value as Int))
                }

                is Long -> {
                    intent.putExtra(extra.key, (extra.value as Long))
                }

                is Float -> {
                    intent.putExtra(extra.key, (extra.value as Float))
                }

                is Double -> {
                    intent.putExtra(extra.key, (extra.value as Double))
                }

                is String -> {
                    intent.putExtra(extra.key, (extra.value as String))
                }

                is Serializable -> {
                    intent.putExtra(extra.key, extra.value as Serializable)
                }
            }

        }

        startActivity(intent)
        if (isFinish) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.getInstance().removeAct(this)
    }
}