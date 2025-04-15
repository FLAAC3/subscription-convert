package xyz.basskick.subscriptionconvert.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

object MySerializer {
    object RegexSerializer: KSerializer<Regex> {
        override val descriptor = serializer<String>().descriptor

        override fun deserialize (decoder: Decoder): Regex =
            Regex(decoder.decodeString())

        override fun serialize (encoder: Encoder, value: Regex) =
            encoder.encodeString(value.pattern)
    }
}