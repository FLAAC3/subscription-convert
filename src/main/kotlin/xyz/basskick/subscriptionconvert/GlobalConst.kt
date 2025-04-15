package xyz.basskick.subscriptionconvert

import okhttp3.OkHttpClient
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

object GlobalConst

private val jarPathWithFile = Paths.get(System.getProperty("java.class.path").run {
    for (i in indices) {
        if (this[i] == ';' && (this[i + 1] == '/' || this[i + 2] == ':')) {
            return@run this.substring(0, i)
        }
    }
    this
}).toAbsolutePath()

val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(30)) //设置连接超时时间
        .readTimeout(Duration.ofSeconds(60)) //设置读超时时间
        .writeTimeout(Duration.ofSeconds(60)) //设置写超时时间
        .callTimeout(Duration.ofSeconds(120)) //设置完整请求超时时间
        .build()

/**
 * JAR包所在的文件夹目录
 * */
fun getJARPath (): Path = jarPathWithFile.parent