package xyz.basskick.subscriptionconvert.process

import Parser
import xyz.basskick.subscriptionconvert.config.Config
import xyz.basskick.subscriptionconvert.config.SourceType

abstract class Process (
    protected val mappingConfig: Config.MappingConfig
) {
    /**
     * 开始处理
     * */
    abstract fun startProcess (): Process

    /**
     * 返回包含订阅信息的内容
     * */
    abstract fun getString (): String

    companion object {
        fun readMappingConfig (mappingConfig: Config.MappingConfig): Process =
            when (mappingConfig.sourceType) {
                SourceType.Meta -> MetaProcess(mappingConfig)
            }

        /**
         * 将配置文件中的命名表达式解析成完整字符串
         * */
        fun parseToName (format: String, name: String): String =
            Parser.setResource(mapOf("name" to name)).parse(format)
    }
}