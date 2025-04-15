import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

class Parser private constructor(
    private val dataMap: Map<String, String> //数据源
) {
    //根据key的长度从大到小排序
    private val keyList: List<String> = ArrayList<String>(dataMap.keys)
        .sortedByDescending { it.length }

    //设置过滤条件
    private var filter: ((key: String) -> Boolean)? = null

    fun setFilter (filter: ((key: String) -> Boolean)?): Parser {
        this.filter = filter
        return this
    }

    /**
     * 解析输入的表达式
     * */
    fun parse (format: String): String {
        //<<表达式起始index, 表达式结束index>, 这个表达式应该被替换成什么字符串>
        val expressionList = arrayListOf<Pair<Pair<Int, Int>, String>>()

        fun search (index: Int) {
            for (i in index..<format.length) {
                if (format[i] != '$') continue
                for (key in keyList) {
                    if (filter != null && filter!!(key)) continue
                    val endIndex = i + 1 + key.length
                    if (endIndex > format.length) continue
                    if (format.substring(i + 1, endIndex) == key) {
                        val search = SearchSubRange(endIndex, format)
                        val value = search.getSubString(dataMap[key]!!)
                        if (value == null) {
                            expressionList.add((i to endIndex - 1) to dataMap[key]!!)
                        } else {
                            expressionList.add((i to search.index - 1) to value)
                        }
                        return search(search.index)
                    }
                }
            }
        }
        search(0)
        val outStr = StringBuilder()
        var index = 0
        for (pair in expressionList) {
            outStr.append(format.substring(index, pair.first.first))
            outStr.append(pair.second)
            index = pair.first.second + 1
        }
        outStr.append(format.substring(index, format.length))
        return outStr.toString()
    }

    companion object {
        /**
         * 根据map来实例化
         * */
        fun setResource (dataMap: Map<String, String>) = Parser(dataMap)

        /**
         * 根据任意对象来实例化
         * */
        fun setResource (any: Any) = Parser(any.parseToMap())

        private val NUM_SET = setOf('0','1','2','3','4','5','6','7','8','9')

        private class SearchSubRange (
            var index: Int, //从哪个下标开始匹配，匹配失败后会指向无法识别的字符
            private val format: String //需要解析的字符串
        ) {
            private val start = StringBuilder() //匹配[start]或者[start,end]
            private val end = StringBuilder()
            private var success = false //是否成功匹配格式[?]或者[?,?]

            init {
                var find = 0x00000 //查找的状态
                var linkedNumber = true //是不是连续的数字，中间不能有空格
                while (index < format.length) {
                    if (find == 0) {
                        if (format[index] == '[') find = 0x10000
                        else if (format[index] != ' ') break
                    } else if (find == 0x10000) {
                        if (NUM_SET.contains(format[index])) {
                            find = 0x11000
                            start.append(format[index])
                        } else if (format[index] != ' ') break
                    } else if (find == 0x11000) {
                        if (linkedNumber && NUM_SET.contains(format[index])) {
                            start.append(format[index])
                        } else if (format[index] == ',') {
                            find = 0x11100
                            linkedNumber = true
                        } else if (format[index] == ' ') {
                            linkedNumber = false
                        } else if (format[index] == ']') {
                            success = true
                            index += 1
                            break
                        } else break
                    } else if (find == 0x11100) {
                        if (NUM_SET.contains(format[index])) {
                            find = 0x11110
                            end.append(format[index])
                        } else if (format[index] != ' ') break
                    } else {
                        if (linkedNumber && NUM_SET.contains(format[index])) {
                            end.append(format[index])
                        } else if (format[index] == ']') {
                            success = true
                            index += 1
                            break
                        } else if (format[index] == ' ') {
                            linkedNumber = false
                        } else break
                    }
                    index += 1
                }
            }

            /**
             * @param value 需要截取的值
             * @return 返回截取之后的子串（如果表达式正确）
             * */
            fun getSubString (value: String): String? {
                if (success) {
                    val sInt = start.toString().toInt()
                    val e = end.toString()
                    return if (e == "") {
                        if (sInt > value.length) null
                        else value.substring(sInt)
                    } else {
                        val eInt = e.toInt()
                        if (eInt >= sInt && eInt <= value.length) {
                            value.substring(sInt, eInt)
                        } else null
                    }
                }
                return null
            }
        }
    }
}

/**
 * 通过反射将任意对象的属性都映射到map里
 * */
fun Any.parseToMap (): Map<String, String> {
    val dataMap = hashMapOf<String, String>()
    for (kProperty in this::class.memberProperties) {
        kProperty.isAccessible = true
        val annotations = kProperty.javaField?.annotations?.map { it as Annotation }
            ?: kProperty.getter.annotations
        if (annotations.any { it.annotationClass == Ignore::class }) continue
        val nameAnnotation = annotations.find { it.annotationClass == Name::class }
        if (nameAnnotation == null) {
            dataMap[kProperty.name] = kProperty.getter.call(this).toString()
        } else {
            val nameProperty = nameAnnotation.annotationClass.memberProperties.first()
            dataMap[nameProperty.call(nameAnnotation).toString()] = kProperty.getter.call(this).toString()
        }
    }
    return dataMap
}