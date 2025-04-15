package xyz.basskick.subscriptionconvert.process

import com.charleskorn.kaml.*
import xyz.basskick.subscriptionconvert.common.encodeToString
import xyz.basskick.subscriptionconvert.config.Config
import kotlin.properties.Delegates

/**
 * 适用于Clash-Meta协议的处理器
 * */
class MetaProcess (mappingConfig: Config.MappingConfig): Process(mappingConfig) {

    private val srcYamlMap = Yaml.default.parseToYamlNode(mappingConfig.getURLContext()).yamlMap
    private val nodeGroupList = (srcYamlMap.get<YamlList>("proxy-groups")
        ?: throw IllegalArgumentException(
            "此地址的yaml配置文件中未包含proxy-groups字段：${mappingConfig.sourceURL}"
        )).items as ArrayList

    override fun getString (): String = srcYamlMap.encodeToString()

    override fun startProcess (): Process { serverProcess(); nodeProcess(); ruleProcess(); return this }

    private fun serverProcess () {
        val srcServerKey = srcYamlMap.getKey("proxies")
            ?: throw IllegalArgumentException("此地址的yaml配置文件中未包含proxies字段：${mappingConfig.sourceURL}")
        val srcLinkedMap = srcYamlMap.entries as LinkedHashMap
        val serverList = ServerList(srcLinkedMap[srcServerKey]!!.yamlList, mappingConfig)
        serverList.doKeep()
        serverList.doDelete()
        if (serverList.isEmpty()) {
            val yamlNull = YamlNull(srcLinkedMap[srcServerKey]!!.path)
            srcLinkedMap[srcServerKey] = yamlNull
        } else {
            serverList.doSort()
            serverList.doRename()
            serverList.savetoYamlList()
        }
    }

    private fun nodeProcess () {
        val removeNodeGroupSet = hashSetOf<YamlNode>()
        for (nodeGroup in nodeGroupList) {
            val nodeGroupYamlMap = nodeGroup.yamlMap
            val nodeList = NodeList(nodeGroupYamlMap["proxies"]!!, mappingConfig)
            nodeList.doKeep()
            nodeList.doDelete()
            if (nodeList.isEmpty()) {
                removeNodeGroupSet.add(nodeGroup)
            } else {
                nodeList.doSort()
                nodeList.doRename()
                nodeList.savetoYamlList()
            }
        }
        nodeGroupList.removeAll(removeNodeGroupSet)
        if (nodeGroupList.isEmpty()) {
            val srcGroupKey = srcYamlMap.getKey("proxy-groups")!!
            val yamlPath = srcYamlMap.get<YamlNode>("proxy-groups")!!.path
            (srcYamlMap.entries as LinkedHashMap)[srcGroupKey] = YamlNull(yamlPath)
        }
    }

    private fun ruleProcess () {
        val groupNameSet = hashSetOf<String>()
        for (nodeGroup in nodeGroupList) {
            val nodeGroupYamlMap = nodeGroup.yamlMap
            groupNameSet.add(nodeGroupYamlMap.get<YamlScalar>("name")!!.content)
        }
        val ruleYamlNode = srcYamlMap.get<YamlNode>("rules")
            ?: throw IllegalArgumentException(
                "此地址的yaml配置文件中未包含rules字段：${mappingConfig.sourceURL}"
            )
        if (groupNameSet.isEmpty()) {
            val srcRuleKey = srcYamlMap.getKey("rules")!!
            (srcYamlMap.entries as LinkedHashMap)[srcRuleKey] = YamlNull(ruleYamlNode.path)
        } else RuleList(ruleYamlNode.yamlList, groupNameSet).removeInYamlList()
    }

    companion object {
        private class Server (val yamlMap: YamlMap) {
            private val map = yamlMap.entries as LinkedHashMap
            private var nameKey by Delegates.notNull<YamlScalar>()

            init {
                for (key in map.keys) {
                    if (key.content == "name") {
                        nameKey = key;break
                    }
                }
            }

            var name = map[nameKey]!!.yamlScalar.content
                set (value) {
                    val yamlScalar = YamlScalar(value, map[nameKey]!!.path)
                    map[nameKey] = yamlScalar
                    field = value
                }
        }

        private class ServerList (
            private val yamlList: YamlList,
            private val mappingConfig: Config.MappingConfig
        ) {
            private val serverList = yamlList.items.map { Server(it.yamlMap) } as ArrayList

            fun isEmpty () = serverList.isEmpty()

            fun doKeep () {
                val removeServerSet = hashSetOf<Server>()
                for (server in serverList) {
                    if (!mappingConfig.checkNeedKeep(server.name)) removeServerSet.add(server)
                }
                serverList.removeAll(removeServerSet)
            }

            fun doDelete () {
                val removeServerSet = hashSetOf<Server>()
                for (server in serverList) {
                    if (mappingConfig.checkNeedDelete(server.name)) removeServerSet.add(server)
                }
                serverList.removeAll(removeServerSet)
            }

            fun doSort () {
                if (mappingConfig.sortRegexArr.isNullOrEmpty() || isEmpty()) return
                val headList = arrayListOf<Server>()
                for (regex in mappingConfig.sortRegexArr) {
                    val iterator = serverList.iterator()
                    while (iterator.hasNext()) {
                        val server = iterator.next()
                        if (server.name.matches(regex)) {
                            headList.add(server)
                            iterator.remove()
                        }
                    }
                }
                serverList.addAll(0, headList)
            }

            fun doRename () {
                val renameMap = mappingConfig.renameMap
                if (renameMap.isNullOrEmpty() || isEmpty()) return
                for (server in serverList) {
                    inner@for ((regex, nameFormat) in renameMap) {
                        if (server.name.matches(regex)) {
                            val name = parseToName(nameFormat, server.name)
                            server.name = name
                            break@inner
                        }
                    }
                }
            }

            fun savetoYamlList () {
                val list = yamlList.items as ArrayList
                list.clear()
                list.addAll(serverList.map { it.yamlMap })
            }
        }

        private class NodeList (
            private val yamlList: YamlList,
            private val mappingConfig: Config.MappingConfig
        ) {
            private val nodePairList = arrayListOf<Pair<String, YamlNode>>() //左边是节点的名字

            init {
                yamlList.items.forEach {
                    nodePairList.add(it.yamlScalar.content to it)
                }
            }

            fun isEmpty () = nodePairList.isEmpty()

            fun doKeep () {
                val removePairSet = hashSetOf<Pair<String, YamlNode>>()
                for (pair in nodePairList) {
                    if (!mappingConfig.checkNeedKeep(pair.first)) removePairSet.add(pair)
                }
                nodePairList.removeAll(removePairSet)
            }

            fun doDelete () {
                val removePairSet = hashSetOf<Pair<String, YamlNode>>()
                for (pair in nodePairList) {
                    if (mappingConfig.checkNeedDelete(pair.first)) removePairSet.add(pair)
                }
                nodePairList.removeAll(removePairSet)
            }

            fun doSort () {
                if (mappingConfig.sortRegexArr.isNullOrEmpty() || isEmpty()) return
                val headPairList = arrayListOf<Pair<String, YamlNode>>()
                for (regex in mappingConfig.sortRegexArr) {
                    val iterator = nodePairList.iterator()
                    while (iterator.hasNext()) {
                        val pair = iterator.next()
                        if (pair.first.matches(regex)) {
                            headPairList.add(pair)
                            iterator.remove()
                        }
                    }
                }
                nodePairList.addAll(0, headPairList)
            }

            fun doRename () {
                val renameMap = mappingConfig.renameMap
                if (renameMap.isNullOrEmpty() || isEmpty()) return
                for ((index, pair) in nodePairList.indices.zip(nodePairList)) {
                    inner@for ((regex, nameFormat) in renameMap) {
                        if (pair.first.matches(regex)) {
                            val newName = parseToName(nameFormat, pair.first)
                            val yamlScalar = YamlScalar(newName, pair.second.path)
                            nodePairList[index] = newName to yamlScalar
                            break@inner
                        }
                    }
                }
            }

            fun savetoYamlList () {
                val list = yamlList.items as ArrayList
                list.clear()
                list.addAll(nodePairList.map { it.second })
            }
        }

        private class RuleList (
            yamlList: YamlList,
            nameSet: Set<String>
        ) {
            private val ruleMap = linkedMapOf<String, ArrayList<YamlNode>>()
            private val ruleList = yamlList.items as ArrayList
            init {
                ruleList.forEach {
                    val strList = it.yamlScalar.content.split(',')
                    val ruleName = if (strList.size < 3) strList.last() else strList[2]
                    val yamlNodeList = ruleMap[ruleName]
                    if (yamlNodeList == null) ruleMap[ruleName] = arrayListOf(it)
                    else ruleMap[ruleName]!!.add(it)
                }
                val removeKeySet = hashSetOf<String>()
                for (key in ruleMap.keys) {
                    if (!nameSet.contains(key)) removeKeySet.add(key)
                }
                removeKeySet.forEach(ruleMap::remove)
            }

            fun removeInYamlList () {
                ruleList.clear()
                ruleList.addAll(ruleMap.values.flatMap { it.asIterable() })
            }
        }
    }
}