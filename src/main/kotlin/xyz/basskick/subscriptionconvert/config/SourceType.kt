package xyz.basskick.subscriptionconvert.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

/**
 * 订阅链接的协议类型
 * */
sealed class SourceType {
    /**
     * Clash-Meta订阅协议
     * */
    data object Meta: SourceType() {
        override fun toString (): String = "meta"
    }

    /**
     * 取消默认的toString实现，具体逻辑需要在密封子类override
     * */
    abstract override fun toString (): String

    companion object {
        /**
         * 根据字符串解析密封类对象
         * */
        @Throws(IllegalArgumentException::class)
        fun of (string: String): SourceType = when (string) {
            Meta.toString() -> Meta
            else -> throw IllegalArgumentException("sourceType字段解析失败，非法参数：$string")
        }

        /**
         * 用于配置文件中的sourceType字段
         * */
        object SourceTypeSerializer: KSerializer<SourceType> {
            private val serializer = serializer<String>() //获取默认的序列化器
            override val descriptor: SerialDescriptor = serializer.descriptor

            override fun serialize (encoder: Encoder, value: SourceType) =
                serializer.serialize(encoder, value.toString())

            override fun deserialize (decoder: Decoder): SourceType =
                of(decoder.decodeString())
        }
    }
}