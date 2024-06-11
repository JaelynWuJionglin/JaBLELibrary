package com.linkiing.test.tool.utlis

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.linkiing.ble.BLEDevice
import com.linkiing.ble.log.LOGUtils
import com.linkiing.test.tool.bean.ConnectTestBean
import com.linkiing.test.tool.sp.SpHelper
import jxl.Workbook
import jxl.WorkbookSettings
import jxl.format.Alignment
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.Colour
import jxl.write.Label
import jxl.write.WritableCell
import jxl.write.WritableCellFormat
import jxl.write.WritableFont
import jxl.write.WritableWorkbook
import jxl.write.WriteException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Excel
 */
object ExcelUtils {
    private const val UTF8_ENCODING = "UTF-8"
    private var arial14font: WritableFont? = null
    private var arial14format: WritableCellFormat? = null
    private var arial10font: WritableFont? = null
    private var arial10format: WritableCellFormat? = null
    private var arial12font: WritableFont? = null
    private var arial12format: WritableCellFormat? = null
    private var testExcelPath = ""

    /**
     * 单元格的格式设置 字体大小 颜色 对齐方式、背景颜色等...
     */
    private fun format() {
        try {
            arial14font = WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD)
            arial14font!!.colour = Colour.LIGHT_BLUE
            arial14format = WritableCellFormat(arial14font)
            arial14format!!.alignment = Alignment.CENTRE
            arial14format!!.setBorder(Border.ALL, BorderLineStyle.THIN)
            arial14format!!.setBackground(Colour.VERY_LIGHT_YELLOW)
            arial10font = WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD)
            arial10format = WritableCellFormat(arial10font)
            arial10format!!.alignment = Alignment.CENTRE
            arial10format!!.setBorder(Border.ALL, BorderLineStyle.THIN)
            arial10format!!.setBackground(Colour.GRAY_25)
            arial12font = WritableFont(WritableFont.ARIAL, 10)
            arial12format = WritableCellFormat(arial12font)
            arial10format!!.alignment = Alignment.CENTRE //对齐格式
            arial12format!!.setBorder(Border.ALL, BorderLineStyle.THIN) //设置边框
        } catch (e: WriteException) {
            e.printStackTrace()
        }
    }

    private fun initExcel(file: File, colName: List<String>) {
        format()
        var workbook: WritableWorkbook? = null
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            workbook = Workbook.createWorkbook(file)
            val sheet = workbook!!.createSheet("error", 0)
            //创建标题栏
            sheet.addCell(Label(0, 0, file.path, arial14format) as WritableCell?)
            for (col in colName.indices) {
                sheet.addCell(Label(col, 0, colName[col], arial10format))
            }
            sheet.setRowView(0, 340) //设置行高
            workbook.write()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (workbook != null) {
                try {
                    workbook.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun writeObjListToExcel(strList: List<List<String>>, file: File) {
        if (strList.isNotEmpty()) {
            var writebook: WritableWorkbook? = null
            var `in`: InputStream? = null
            try {
                val setEncode = WorkbookSettings()
                setEncode.encoding = UTF8_ENCODING
                `in` = FileInputStream(file)
                val workbook: Workbook = Workbook.getWorkbook(`in`)
                writebook = Workbook.createWorkbook(file, workbook)
                val sheet = writebook.getSheet(0)

                for (j in strList.indices) {
                    val list = strList[j]
                    for (i in list.indices) {
                        sheet.addCell(Label(i, j + 1, list[i], arial12format))
                        if (list[i].length <= 5) {
                            sheet.setColumnView(i, list[i].length + 8) //设置列宽
                        } else {
                            sheet.setColumnView(i, list[i].length + 5) //设置列宽
                        }
                    }
                    sheet.setRowView(j + 1, 350) //设置行高
                }
                writebook.write()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                if (writebook != null) {
                    try {
                        writebook.close()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
                if (`in` != null) {
                    try {
                        `in`.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun initTestExcelPath(context: Context) {
        val parentDir =
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {

                //如果外部储存可用
                //获得外部存储路径,默认路径为 /sdcard/Android/data/appPackage/files/AppLog/xxxxx
                context.getExternalFilesDir(null)!!.path
            } else {

                //直接存在/data/data里，非root手机是看不到的
                context.filesDir.path
            }
        testExcelPath = "$parentDir/TestExcel/"

        val file = File(testExcelPath)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    /**
     * 导出ConnectTest数据
     */
    fun exportExcelConnectTest(context: Context, listener: (File) -> Unit) {
        Thread {
            val titleList = arrayListOf(
                "测试时间",
                "设备名",
                "Mac地址",
                "信号强度",
                "连接超时时间",
                "测试详情",
                "测试结果"
            )
            val strList = arrayListOf<ArrayList<String>>()
            for (bean in SpHelper.instance.getConnectTestList()) {
                val arrayList = arrayListOf<String>()
                arrayList.add(bean.startTestTime)
                arrayList.add(bean.devName)
                arrayList.add(bean.devMac)
                arrayList.add(bean.rssi.toString())
                arrayList.add("${bean.connectOutTime}s")
                arrayList.add("${bean.testConnectSusNumber}/${bean.connectTestNumber}次")
                arrayList.add(
                    when (bean.testResult) {
                        ConnectTestBean.TEST_RESULT_TEST -> {
                            "TEST"
                        }

                        ConnectTestBean.TEST_RESULT_OK -> {
                            "OK"
                        }

                        ConnectTestBean.TEST_RESULT_FAIL -> {
                            "NO"
                        }

                        ConnectTestBean.TEST_RESULT_CON_FAIL -> {
                            "C-ER"
                        }

                        else -> {
                            "--"
                        }
                    }
                )
                strList.add(arrayList)
            }

            initTestExcelPath(context)

            val file = File(testExcelPath, "ConnectTestExcel_share.xls")
            LOGUtils.d("ExcelUtils ==> 文件路径：${file.path}")
            if (file.exists() && file.isFile) {
                //文件存在，则先删除
                file.delete()
            }

            initExcel(file, titleList)
            writeObjListToExcel(strList, file)

            listener(file)
        }.start()
    }

    /**
     * 导出ScanDeviceTest数据
     */
    fun exportScanDeviceTest(context: Context,
                             list: MutableList<BLEDevice>,
                             startScanTime: Long,
                             listener: (File) -> Unit) {
        Thread {
            val titleList = arrayListOf(
                "设备名",
                "Mac地址",
                "信号强度",
                "次数",
                "开始时间:${MyUtils.long2TimeYMDHMS(startScanTime)}",
                "结束时间:${MyUtils.long2TimeHMS(System.currentTimeMillis())}",
                "设备总数:${list.size}"
            )
            val strList = arrayListOf<ArrayList<String>>()
            for (bean in list) {
                val arrayList = arrayListOf<String>()
                arrayList.add(bean.deviceName)
                arrayList.add(bean.deviceMac)
                arrayList.add(Gson().toJson(bean.rssiList))
                arrayList.add(bean.rssiList.size.toString())
                arrayList.add("")
                arrayList.add("")
                arrayList.add("")

                strList.add(arrayList)
            }

            initTestExcelPath(context)

            val file = File(testExcelPath, "ScanDeviceTestExcel_share.xls")
            LOGUtils.d("ExcelUtils ==> 文件路径：${file.path}")
            if (file.exists() && file.isFile) {
                //文件存在，则先删除
                file.delete()
            }

            initExcel(file, titleList)
            writeObjListToExcel(strList, file)

            listener(file)
        }.start()
    }
}