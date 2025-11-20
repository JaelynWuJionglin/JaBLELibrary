package com.linkiing.ble.log

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.text.TextUtils
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.exitProcess

object LOGUtils {
    private const val TAG = "LK_BLE"
    private const val LINE_MAX = 3 * 1024
    private const val MAX_LOG_FILE_SIZE = 5 //最大日志保存数量，最小为1
    private var logPath: String = "" //日志文件保存路径
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    private val erInfoMap: MutableMap<String, String> = HashMap() //用来存储设备信息和异常信息
    private var sExecutorService: ExecutorService? = null//线程池
    private var mApplication: Application? = null
    private var isLog = true
    private var isSave = true

    /**
     * 初始化
     * @param app    Application
     * @param isLog 是否打印日志
     * @param isSave 是否保存日志文件
     */
    @JvmStatic
    fun init(app: Application, isLog: Boolean, isSave: Boolean) {
        mApplication = app
        LOGUtils.isLog = isLog
        LOGUtils.isSave = isSave
        if (isSave) {
            initExecutors()
            logFileInit(app)

            //crash 日志
            //获取系统默认的UncaughtException处理器
            mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            //设置该CrashHandler为程序的默认处理器
            Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler)
        }
    }

    //分享App日志
    @SuppressLint("SimpleDateFormat")
    fun shareAppLogFile(listener: (File?) -> Unit) {
        if (TextUtils.isEmpty(logPath) || mApplication == null) {
            return
        }
        Thread {
            val shareDir = "${FileJaUtils.getParentFilePath(mApplication!!)}${File.separator}LOGZip"
            val shareFile = File(shareDir)
            if (!shareFile.exists()) {
                shareFile.mkdirs()
            }

            val shareName =
                "AppLog_${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(System.currentTimeMillis()))}.zip"
            val sharePath = "$shareDir${File.separator}$shareName"
            //压缩文件夹为zip
            FileJaUtils.zip(logPath, sharePath)

            //删除之前的文件
            val files = shareFile.listFiles()
            if (files != null && files.isNotEmpty()) {
                for (file in files) {
                    if (file.name.endsWith(".zip") && !file.name.equals(shareName)) {
                        file?.delete()
                    }
                }
            }

            listener(File(sharePath))
        }.start()
    }

    /**
     * 日志文件保存路径
     */
    @JvmStatic
    fun getLogFilePath(): String {
        return logPath
    }

    @JvmStatic
    fun v(msg: String) {
        logWrap("v", TAG, msg)
    }

    @JvmStatic
    fun d(msg: String) {
        logWrap("d", TAG, msg)
    }

    @JvmStatic
    fun i(msg: String) {
        logWrap("i", TAG, msg)
    }

    @JvmStatic
    fun e(msg: String) {
        logWrap("e", TAG, msg)
    }

    @JvmStatic
    fun w(msg: String) {
        logWrap("w", TAG, msg)
    }

    @JvmStatic
    fun iGson(jsonTag: String, jsonStr: String) {
        logCat("i", jsonTag)
        logCat("i", format(jsonStr))
    }

    /**
     * 打印UUID服务通道
     */
    @JvmStatic
    fun logUUID(gatt: BluetoothGatt) {
        val services = gatt.services
        if (services != null) {
            for (gattService in services) {
                d("服务:${gattService.uuid}")
                val chars = gattService.characteristics
                for (char in chars) {
                    d("==>通道:${char.uuid}")
                }
            }
        } else {
            e("logUUID() ==> Error! services != null")
        }
    }

    /**
     * 采用分段打印,防止log太长打印不全
     * @param leveStr 日志级别（v,d,i,w,e）
     * @param msg     日志内容
     */
    @JvmStatic
    fun logWrap(leveStr: String, tag: String, msg: String) {
        if (TextUtils.isEmpty(msg)) {
            logCat(leveStr, tag, msg)
            return
        }
        try {
            if (msg.length > LINE_MAX) {
                var i = 0
                while (i < msg.length) {
                    if (i + LINE_MAX < msg.length) {
                        logCat(leveStr, tag, msg.substring(i, i + LINE_MAX))
                    } else {
                        logCat(leveStr, tag, msg.substring(i))
                    }
                    i += LINE_MAX
                }
            } else {
                logCat(leveStr, tag, msg)
            }
        } catch (e: Exception) {
            logCat("w", e.toString())
        }
    }

    //打印日志
    private fun logCat(leveStr: String, tag: String, msg: String) {
        if (isLog) {
            when (leveStr) {
                "v" -> Log.v(tag, msg)
                "d" -> Log.d(tag, msg)
                "i" -> Log.i(tag, msg)
                "e" -> Log.e(tag, msg)
                "w" -> Log.w(tag, msg)
            }
        }

        try {
            //保存日志到文件
            pointLogToFile(tag, msg)
        } catch (e: Exception) {
            logCat("w", tag, e.toString())
        }
    }

    //打印日志(本地TAG)
    private fun logCat(leveStr: String, msg: String) {
        logCat(leveStr, TAG, msg)
    }

    //Json format
    private fun format(mJson: String): String {
        if (TextUtils.isEmpty(mJson)) {
            return ""
        }
        val s = StringBuilder(mJson)
        var offset = 0 //目标字符串插入空格偏移量
        var bOffset = 0 //空格偏移量
        for (i in mJson.indices) {
            when (mJson[i]) {
                '{', '[' -> {
                    bOffset += 4
                    s.insert(
                        i + offset + 1, """
             
             ${generateBlank(bOffset)}
             """.trimIndent()
                    )
                    offset += bOffset + 1
                }

                ',' -> {
                    s.insert(
                        i + offset + 1, """
             
             ${generateBlank(bOffset)}
             """.trimIndent()
                    )
                    offset += bOffset + 1
                }

                '}', ']' -> {
                    bOffset -= 4
                    s.insert(
                        i + offset, """
             
             ${generateBlank(bOffset)}
             """.trimIndent()
                    )
                    offset += bOffset + 1
                }
            }
        }
        return s.toString()
    }

    private fun generateBlank(num: Int): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until num) {
            stringBuilder.append(" ")
        }
        return stringBuilder.toString()
    }

    //创建一个大小固定为1的线程池
    private fun initExecutors() {
        sExecutorService = Executors.newFixedThreadPool(1)
    }


    //日志保存初始化
    private fun logFileInit(context: Context) {
        val parentDir = FileJaUtils.getParentFilePath(context)
        logPath = "$parentDir/AppLog/"

        val file = File(logPath)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    //将log信息写入文件中
    private fun pointLogToFile(tag: String, msg: String) {
        if (!isSave) {
            return
        }

        if (TextUtils.isEmpty(logPath) || sExecutorService == null) {
            return
        }
        if (sExecutorService!!.isShutdown) {
            initExecutors()
        }
        sExecutorService!!.submit {
            val date = Date()
            val dateFormat =
                SimpleDateFormat("", Locale.SIMPLIFIED_CHINESE)
            dateFormat.applyPattern("yyyy-MM-dd")
            val path =
                logPath + dateFormat.format(date) + ".log"
            dateFormat.applyPattern("[yyyy-MM-dd HH:mm:ss.SSS]")
            val time = dateFormat.format(date)
            val file = File(path)
            if (!file.exists()) {
                createDipPath(path)
            }
            var out: BufferedWriter? = null
            try {
                out = BufferedWriter(
                    OutputStreamWriter(
                        FileOutputStream(
                            file,
                            true
                        )
                    )
                )
                out.write("$time $tag $msg\r\n")
            } catch (e: Exception) {
                logCat("w", e.toString())
            } finally {
                try {
                    out?.close()
                } catch (e: IOException) {
                    logCat("w", e.toString())
                }
            }
        }
    }

    //根据文件路径 递归创建文件
    private fun createDipPath(file: String) {
        val parentFile = file.substring(0, file.lastIndexOf("/"))
        val parent = File(parentFile)

        //日志文件数量限制
        while (true) {
            val files = parent.listFiles() ?: break
            if (files.size <= 1.coerceAtLeast(MAX_LOG_FILE_SIZE)) break
            removeOneOldLogFile(files)
        }
        val file1 = File(file)
        if (!file1.exists()) {
            parent.mkdirs()
            try {
                file1.createNewFile()
            } catch (e: IOException) {
                logCat("w", e.toString())
            }
        }
    }

    //删除一个最老的日志文件
    private fun removeOneOldLogFile(files: Array<File>?) {
        if (!files.isNullOrEmpty()) {
            //超过最大文件数量，删除最老的记录
            val dateFormat = SimpleDateFormat("", Locale.SIMPLIFIED_CHINESE)
            dateFormat.applyPattern("yyyy-MM-dd")
            try {
                val nowDate = dateFormat.parse(dateFormat.format(System.currentTimeMillis()))
                val nowDay = nowDate?.time ?: 0L
                var minTime: Long = 0
                var minFile: File? = null
                for (f in files) {
                    if (f.isFile) {
                        val name = f.name
                        val date = dateFormat.parse(name)
                        val time = date?.time ?: 0L
                        if (time <= 0) {
                            continue
                        }
                        if (time == nowDay) {
                            //当前log文件跳过
                            continue
                        }
                        if (minTime == 0L) {
                            minTime = time
                            minFile = f
                        } else if (time < minTime) {
                            minTime = time
                            minFile = f
                        }
                    }
                }
                deleteFileOrDir(minFile)
            } catch (e: Exception) {
                logCat("w", e.toString())
            }
        }
    }

    //递归删除文件和文件夹
    private fun deleteFileOrDir(file: File?) {
        if (file == null) {
            return
        }
        if (file.isFile) {
            file.delete()
            return
        }
        if (file.isDirectory) {
            val childFile = file.listFiles()
            if (childFile == null || childFile.isEmpty()) {
                file.delete()
                return
            }
            for (f in childFile) {
                deleteFileOrDir(f)
            }
            file.delete()
        }
    }

    /***********************************************************************************************
     * Error Log   *******************************************************************************
     */
    //当UncaughtException发生时会转入该函数来处理
    private val mUncaughtExceptionHandler =
        Thread.UncaughtExceptionHandler { thread: Thread?, throwable: Throwable? ->
            if (thread == null || throwable == null) {
                logCat("w", "mUncaughtExceptionHandler thread == null || throwable == null")
                return@UncaughtExceptionHandler
            }
            try {
                if (!handleException(
                        thread,
                        throwable
                    ) && mDefaultHandler != null
                ) {
                    //如果用户没有处理则让系统默认的异常处理器来处理
                    mDefaultHandler!!.uncaughtException(thread, throwable)
                } else {
                    try {
                        Thread.sleep(3000)
                    } catch (e: InterruptedException) {
                        logCat("w", e.toString())
                    }
                    //退出程序
                    Process.killProcess(Process.myPid())
                    exitProcess(1)
                }
            } catch (e: Exception) {
                logCat("w", e.toString())
            }
        }

    private fun handleException(thread: Thread, throwable: Throwable): Boolean {
        //收集设备参数信息
        collectDeviceInfo(mApplication)
        //保存日志文件
        saveCrashInfo2File(thread, throwable)
        return true
    }

    //收集设备参数信息
    private fun collectDeviceInfo(ctx: Context?) {
        try {
            val pm = ctx!!.packageManager
            val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(ctx.packageName, PackageManager.GET_ACTIVITIES)
            }
            if (pi != null) {
                val versionName = if (pi.versionName == null) "null" else pi.versionName
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pi.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    pi.versionCode.toString()
                }
                erInfoMap["versionName"] = versionName ?: ""
                erInfoMap["versionCode"] = versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            logCat("w", "an error occured when collect package info \n $e")
        }
        val fields = Build::class.java.declaredFields
        for (field in fields) {
            if (field != null) {
                try {
                    field.isAccessible = true
                    erInfoMap[field.name] = (field[null]?.toString() ?: "null") + ""
                    //logCat("d", field.name + " : " + field[null])
                } catch (e: Exception) {
                    logCat("w", "an error occured when collect crash info \n $e")
                }
            }
        }
    }

    //保存错误信息到文件中
    private fun saveCrashInfo2File(thread: Thread, throwable: Throwable) {
        val sb = StringBuilder()
        for ((key, value) in erInfoMap) {
            sb.append(key)
                .append("=")
                .append(value)
                .append("\n")
        }
        val dateFormat = SimpleDateFormat("", Locale.SIMPLIFIED_CHINESE)
        dateFormat.applyPattern("[yyyy-MM-dd HH:mm:ss.SSS]")
        val time = dateFormat.format(Date())
        sb.append(time)
        if (mApplication != null) {
            sb.append("FATAL EXCEPTION:")
                .append(thread.name)
                .append("\n")
                .append("Process:")
                .append(mApplication!!.packageName)
                .append(", PID:")
                .append(Process.myPid())
        }
        sb.append("\n")
        val writer: Writer = StringWriter()
        val printWriter = PrintWriter(writer)
        throwable.printStackTrace(printWriter)
        var cause = throwable.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        sb.append(result)

        //打印并保存日志
        logCat("e", sb.toString())
    }
}