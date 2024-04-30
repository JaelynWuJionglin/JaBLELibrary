package com.linkiing.ble.utils

import com.linkiing.ble.log.LOGUtils

object CRC8 {

    /**
     * crc
     */
    @JvmStatic
    fun getCrc8(bytes: ByteArray): Int {
        var crc = 0
        for (datum in bytes) {
            crc = datum.toInt() xor crc and 0xFF
            for (j in 0..7) {
                if (crc and 0x80 != 0) {
                    crc = crc shl 1 and 0xFF
                    crc = crc xor 0x07 and 0xFF
                } else {
                    crc = crc shl 1 and 0xFF
                }
            }
        }
        //LOGUtils.d("getCrc8() ==> ${ByteUtils.toHexString(bytes, ",")}  crc:${crc.toString(16)}")
        return crc
    }

}