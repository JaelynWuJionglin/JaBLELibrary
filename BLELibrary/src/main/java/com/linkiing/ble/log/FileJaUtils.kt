package com.linkiing.ble.log

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object FileJaUtils {

    /**
     * 获取app外部存储跟目录
     */
    fun getParentFilePath(context: Context): String {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {

            //如果外部储存可用
            //获得外部存储路径,默认路径为 /sdcard/Android/data/appPackage/files/AppLog/xxxxx
            context.getExternalFilesDir(null)!!.path
        } else {

            //直接存在/data/data里，非root手机是看不到的
            context.filesDir.path
        }
    }

    /**
     * 分享文件
     */
    fun shareFile(context: Context, file: File, titleMsg: String) {
        if (file.exists()) {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, context.packageName + ".fileProvider", file)
            } else {
                Uri.fromFile(file)
            }
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(Intent.createChooser(intent, titleMsg))
        }
    }

    /**
     * 压缩文件
     * @param src 源文件夹
     * @param dest 目标文件
     */
    fun zip(src: String, dest: String) {
        //提供了一个数据项压缩成一个ZIP归档输出流
        var out: ZipOutputStream? = null
        try {
            val outFile = File(dest) //源文件或者目录
            val fileOrDirectory = File(src) //压缩文件路径
            out = ZipOutputStream(FileOutputStream(outFile))
            //如果此文件是一个文件，否则为false。
            if (fileOrDirectory.isFile) {
                zipFileOrDirectory(out, fileOrDirectory, "")
            } else {
                //返回一个文件或空阵列。
                val entries = fileOrDirectory.listFiles()
                if (entries != null) {
                    for (i in entries.indices) {
                        // 递归压缩，更新curPaths
                        zipFileOrDirectory(out, entries[i], "")
                    }
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            //关闭输出流
            if (out != null) {
                try {
                    out.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
    }


    private fun zipFileOrDirectory(
        out: ZipOutputStream,
        fileOrDirectory: File, curPath: String
    ) {
        //从文件中读取字节的输入流
        var fileInputStream: FileInputStream? = null
        try {
            //如果此文件是一个目录，否则返回false。
            if (!fileOrDirectory.isDirectory) {
                // 压缩文件
                val buffer = ByteArray(4096)
                var byteRead: Int
                fileInputStream = FileInputStream(fileOrDirectory)
                //实例代表一个条目内的ZIP归档
                val entry = ZipEntry(
                    curPath
                            + fileOrDirectory.name
                )
                //条目的信息写入底层流
                out.putNextEntry(entry)
                while (fileInputStream.read(buffer).also { byteRead = it } != -1) {
                    out.write(buffer, 0, byteRead)
                }
                out.closeEntry()
            } else {
                // 压缩目录
                val entries = fileOrDirectory.listFiles()
                if (entries != null) {
                    for (i in entries.indices) {
                        // 递归压缩，更新curPaths
                        zipFileOrDirectory(
                            out, entries[i], curPath
                                    + fileOrDirectory.name + "/"
                        )
                    }
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            // throw ex;
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    /**
     * 解压文件
     */
    fun unzip(zipFileName: String?, outputDirectory: String) {
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(zipFileName)
            val e: Enumeration<*> = zipFile.entries()
            var zipEntry: ZipEntry?
            val dest = File(outputDirectory)
            dest.mkdirs()
            while (e.hasMoreElements()) {
                zipEntry = e.nextElement() as ZipEntry
                val entryName = zipEntry.name
                var `in`: InputStream? = null
                var out: FileOutputStream? = null
                try {
                    if (zipEntry.isDirectory) {
                        var name = zipEntry.name
                        name = name.substring(0, name.length - 1)
                        val f = File(
                            outputDirectory + File.separator
                                    + name
                        )
                        f.mkdirs()
                    } else {
                        var index = entryName.lastIndexOf("\\")
                        if (index != -1) {
                            val df = File(
                                outputDirectory + File.separator
                                        + entryName.substring(0, index)
                            )
                            df.mkdirs()
                        }
                        index = entryName.lastIndexOf("/")
                        if (index != -1) {
                            val df = File(
                                outputDirectory + File.separator
                                        + entryName.substring(0, index)
                            )
                            df.mkdirs()
                        }
                        val f = File(
                            outputDirectory + File.separator
                                    + zipEntry.name
                        )
                        // f.createNewFile();
                        `in` = zipFile.getInputStream(zipEntry)
                        out = FileOutputStream(f)
                        var c: Int
                        val by = ByteArray(1024)
                        while (`in`.read(by).also { c = it } != -1) {
                            out.write(by, 0, c)
                        }
                        out.flush()
                    }
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    throw IOException("解压失败：$ex")
                } finally {
                    if (`in` != null) {
                        try {
                            `in`.close()
                        } catch (ex: IOException) {
                        }
                    }
                    if (out != null) {
                        try {
                            out.close()
                        } catch (ex: IOException) {
                        }
                    }
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            throw IOException("解压失败：$ex")
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close()
                } catch (ex: IOException) {
                }
            }
        }
    }
}