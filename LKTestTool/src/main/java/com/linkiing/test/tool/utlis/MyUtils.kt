package com.linkiing.test.tool.utlis

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*


object MyUtils {

    /**
     * 时间戳转换日期
     */
    @SuppressLint("SimpleDateFormat")
    fun long2TimeYMDHMS(t: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return dateFormat.format(Date(t))
    }

    @SuppressLint("SimpleDateFormat")
    fun long2TimeHMS(t: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss")
        return dateFormat.format(Date(t))
    }

    /**
     * String转Int
     */
    fun strOrInt(str: String): Int {
        return try {
            str.toInt()
        } catch (n: NumberFormatException) {
            0
        }
    }

    /**
     * String转Long
     */
    fun strOrLong(str: String): Long {
        return try {
            str.toLong()
        } catch (n: NumberFormatException) {
            0L
        }
    }
}