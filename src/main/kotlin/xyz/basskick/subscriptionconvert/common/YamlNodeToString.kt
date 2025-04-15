package xyz.basskick.subscriptionconvert.common

import com.charleskorn.kaml.*
import xyz.basskick.subscriptionconvert.common.YamlNodeToString.Companion.encode

/**
 * 将YamlNode对象序列化为yaml格式的字符串
 * */
class YamlNodeToString {
    private val strBuilder = StringBuilder() //构建序列化之后的YamlNode字符串
    private var spaceCharSize = 0 //设置缩进的数量

    @Throws(UnsupportedOperationException::class)
    private fun encodeYamlMap (yamlMap: YamlMap) {
        var index = 1 //处理到第几个键值对
        yamlMap.entries.forEach { (key, value) ->
            when (value) {
                is YamlMap -> {
                    strBuilder.appendLineSpaced(key.content + ":");more()
                    encodeYamlMap(value)
                }
                is YamlScalar -> {
                    strBuilder.appendLineSpaced(key.content + ": ${value.content.quoted()}")
                }
                is YamlList -> {
                    strBuilder.appendLineSpaced(key.content + ":");more()
                    encodeYamlList(value)
                }
                is YamlNull -> {
                    strBuilder.appendLineSpaced(key.content + ": ~")
                }
                else -> throw UnsupportedOperationException("含有不支持解析的Yaml元素")
            }
            if (index == yamlMap.entries.size) less()
            else index += 1
        }
    }

    @Throws(UnsupportedOperationException::class)
    private fun encodeYamlList (yamlList: YamlList) {
        var index = 1 //处理到第几个
        yamlList.items.forEach { value ->
            strBuilder.appendLineSpaced("-");more()
            when (value) {
                is YamlMap -> encodeYamlMap(value)
                is YamlScalar -> {
                    strBuilder.insert(strBuilder.length - 2, " ${value.content.quoted()}");less()
                }
                is YamlList -> encodeYamlList(value)
                is YamlNull -> {
                    strBuilder.insert(strBuilder.length - 2, " ~");less()
                }
                else -> throw UnsupportedOperationException("含有不支持解析的Yaml元素")
            }
            if (index == yamlList.items.size) less()
            else index += 1
        }
    }

    private fun more () { spaceCharSize += 2 } //增加缩进

    private fun less () { spaceCharSize -= 2 } //减少缩进

    private fun StringBuilder.appendLineSpaced (value: String?) {
        for (i in 0 until spaceCharSize) append(' ')
        append(value).append("\r\n")
    }

    companion object {
        private val specialCharReg = Regex("""^\s*[*&!,%@>|`\[\]{}]|#""") //检查有没有包含Yaml关键字符

        private fun String.quoted () =
            if (contains(specialCharReg)) "\"$this\"" else this

        @Throws(UnsupportedOperationException::class)
        fun encode (yamlNode: YamlNode): String =
            when (yamlNode) {
                is YamlMap -> YamlNodeToString().apply { encodeYamlMap(yamlNode) }.strBuilder.toString()
                is YamlList -> YamlNodeToString().apply { encodeYamlList(yamlNode) }.strBuilder.toString()
                is YamlScalar -> yamlNode.content.quoted()
                is YamlNull -> "~"
                else -> throw UnsupportedOperationException("含有不支持解析的Yaml元素")
            }
    }
}

/**
 * 拓展YamlNode类，添加序列化为Yaml格式字符串的方法
 * */
fun YamlNode.encodeToString (): String = encode(this)