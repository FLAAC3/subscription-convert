package xyz.basskick.subscriptionconvert

import com.beust.jcommander.Parameter
import okio.FileNotFoundException
import java.io.File

class CommandEntity {
    @Parameter(names = ["-p"], description = "指定项目监听的端口号")
    var port: Int = 8848 //默认端口号

    @Parameter(names = ["-context-path"], description = "项目的监听根地址")
    var contextPath: String = "/"

    @Parameter(names = ["-i"], description = "项目的yaml配置文件地址")
    private var yamlPathStr: String? = null

    @Parameter(names = ["-h"], description = "查看帮助信息", help = true)
    var help = false

    /**
     * 获取项目Yaml配置文件路径
     * */
    @Throws(FileNotFoundException::class)
    fun getYamlFile (): File {
        val out = if (yamlPathStr == null) getJARPath().resolve("config.yaml").toFile()
        else if (yamlPathStr!!.contains(File.pathSeparatorChar)) File(yamlPathStr!!)
        else getJARPath().resolve(yamlPathStr!!).toFile()
        if (out.exists() && out.isFile) return out
        else throw FileNotFoundException("无法读取到配置文件！")
    }
}