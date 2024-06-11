package com.linkiing.test.tool.utlis

import java.util.Locale
import kotlin.experimental.and

/**
 * ByteUtils
 */
object ByteUtils {
    /**
     * int到byte[] 由高位到低位
     * @param i 需要转换为byte数组的整行值。
     * @return byte数组
     */
    fun intToByteArray(i: Int): ByteArray {
        val result = ByteArray(4)
        result[0] = (i shr 24 and 0xFF).toByte()
        result[1] = (i shr 16 and 0xFF).toByte()
        result[2] = (i shr 8 and 0xFF).toByte()
        result[3] = (i and 0xFF).toByte()
        return result
    }

    /**
     * byte[]转int
     * @param bytes 需要转换成int的数组
     * @return int值
     */
    fun byteArray4ToInt(bytes: ByteArray): Int {
        var value = 0
        val size = bytes.size - 1
        for (i in 0..size) {
            val shift = (size - i) * 8
            value += bytes[i].toInt() and 0xFF shl shift
        }
        return value
    }

    fun byteArrayToInt(bytes: ByteArray): Int {
        var value = 0
        val size = bytes.size - 1
        for (i in 0..size) {
            val shift = (size - i) * 8
            value += bytes[i].toInt() and 0xFF shl shift
        }
        return value
    }

    /**
     * byteArrayToIntArray
     */
    fun byteArrayToIntArray(bytes: ByteArray): IntArray {
        val size = bytes.size
        val intArray = IntArray(size)
        for (i in 0 until size) {
            intArray[i] = byteToInt(bytes[i])
        }
        return intArray
    }

    /**
     * Byte转无符号Int
     */
    fun byteToInt(byte:Byte): Int {
        return (byte.toInt() and 0xFF)
    }

    /** 计算校验位 ，返回十六进制校验位  */
    private fun makeCheckSum(dataArray: ByteArray): Byte {
        var checkSumHex = 0
        for (data in dataArray) {
            checkSumHex += data
        }
        return checkSumHex.toByte()
    }

    /**
     * 字节数组转成16进制表示格式的字符串
     *
     * @param byteArray 需要转换的字节数组
     * @return 16进制表示格式的字符串, <separator>分割
     */
    fun toHexString(byteArray: ByteArray,separator: String): String {
        val hexString = StringBuilder()
        for (i in byteArray.indices) {
            if ((byteArray[i].toInt() and 0xff) < 0x10){//0~F前面不零
                hexString.append("0")
            }
            hexString.append(Integer.toHexString(0xFF and byteArray[i].toInt()) + separator)
        }
        return hexString.toString().uppercase(Locale.getDefault())
    }

    /**
     * 字节数组转成16进制表示格式的字符串
     *
     * @param byteArray 需要转换的字节数组
     * @return 16进制表示格式的字符串，无分割
     */
    fun toHexString(byteArray: ByteArray): String {
        val hexString = StringBuilder()
        for (i in byteArray.indices) {
            if ((byteArray[i].toInt() and 0xff) < 0x10){//0~F前面不零
                hexString.append("0")
            }
            hexString.append(Integer.toHexString(0xFF and byteArray[i].toInt()))
        }
        return hexString.toString().uppercase(Locale.getDefault())
    }

    /**
     * IntArray转成16进制表示格式的字符串
     */
    fun toHexString(intArray: IntArray): String {
        val hexString = StringBuilder()
        for (i in intArray.indices) {
            if ((intArray[i] and 0xff) < 0x10){//0~F前面不零
                hexString.append("0")
            }
            hexString.append(Integer.toHexString(0xFF and intArray[i]))
        }
        return hexString.toString().uppercase(Locale.getDefault())
    }

    /**
     * hex string转byte[]
     */
    fun hexStringToBytes(hex: String): ByteArray {
        val len = hex.length / 2
        val result = ByteArray(len)
        val aChar = hex.toCharArray()
        for (i in 0 until len) {
            val pos = i * 2
            result[i] = (toByte(aChar[pos]) shl 4 or toByte(aChar[pos + 1])).toByte()
        }
        return result
    }

    private fun toByte(c: Char): Int {
        return "0123456789ABCDEF".indexOf(c)
    }

    /**
     * getHeightByte
     */
    fun getHeightByte(value: Int): Byte {
        return (value and 0xFF00 shr 8).toByte()
    }

    /**
     * getLowByte
     */
    fun getLowByte(value: Int): Byte {
        return (value and 0xFF).toByte()
    }

    /**
     * getByteByLowAndHeight
     */
    fun getByteByLowAndHeight(height: Byte, low: Byte): Int {
        return (height and 0xFF.toByte()).toInt() shl 8 and 0xFF00 or (low and 0xFF.toByte()).toInt() and 0x00FF
    }
}