package xyz.basskick.subscriptionconvert

import com.beust.jcommander.JCommander
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import xyz.basskick.subscriptionconvert.config.Config

@SpringBootApplication
class SubscriptionConvertApplication {
    companion object {
        @JvmStatic
        fun main (args: Array<String>) {
            val commandEntity = CommandEntity() //命令行数据类
            val jCommander = JCommander.newBuilder()
                .addObject(commandEntity)
                .build().apply {
                    parse(*args) //解析输入的命令行
                }
            if (commandEntity.help) { //打印帮助信息
                jCommander.usage();return
            }
            Config.initByFile(commandEntity.getYamlFile()) //读取配置，初始化Config
            val app = SpringApplication(SubscriptionConvertApplication::class.java)
            app.setDefaultProperties(HashMap<String, Any>().apply {
                this["server.port"] = commandEntity.port //设置监听端口号
            })
            app.run()
        }
    }
}