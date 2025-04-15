package xyz.basskick.subscriptionconvert.config

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.*
import okhttp3.CacheControl
import okhttp3.Request
import okio.source
import xyz.basskick.subscriptionconvert.common.MySerializer
import xyz.basskick.subscriptionconvert.okHttpClient
import java.io.File
import kotlin.properties.Delegates

object Config {
    var mappingConfigMap by Delegates.notNull<HashMap<String, MappingConfig>>() //配置文件

    @Serializable
    private class PathMap (
        val pathMap: HashMap<String, MappingConfig>
    )

    @Serializable
    class MappingConfig (
        val sourceURL: String,
        @Serializable(SourceType.Companion.SourceTypeSerializer::class)
        val sourceType: SourceType,
        @Serializable(MySerializer.RegexSerializer::class)
        private val keepRegex: Regex? = null,
        @Serializable(MySerializer.RegexSerializer::class)
        private val deleteRegex: Regex? = null,
        @SerialName("sortRegexArr")
        private val sortRegexStrArr: Array<String>? = null,
        @SerialName("renameMap")
        private val renameStrMap: Map<String, String>? = null
    ) {
        @Transient
        val sortRegexArr = sortRegexStrArr?.map(::Regex)
        @Transient
        val renameMap = renameStrMap?.mapKeys { Regex(it.key) }

        /**
         * 根据URL发起网络请求，返回订阅的文本内容
         * */
        @Throws(IllegalArgumentException::class)
        fun getURLContext (): String = let { _->
            val request = Request.Builder().get().url(sourceURL)
                .cacheControl(CacheControl.FORCE_NETWORK) //不使用缓存
                .addHeader("Accept", "text/plain")
                .addHeader("User-Agent", sourceType.toString())
                .build()
            okHttpClient.newCall(request).execute().body?.string()
                ?: throw IllegalArgumentException("配置文件中的此链接无效：${sourceURL}")
        }

        fun checkNeedKeep (string: String): Boolean = keepRegex?.let {
            string.matches(it)
        } ?: true

        fun checkNeedDelete (string: String): Boolean = deleteRegex?.let {
            string.matches(it)
        } ?: false
    }

    /**
     * @param file 配置文件
     * 调用这个函数初始化 mappingConfigMap
     * */
    fun initByFile (file: File) {
        mappingConfigMap = Yaml.default.decodeFromSource(PathMap.serializer(), file.source()).pathMap
    }
}