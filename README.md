# subscription-convert

**1. 通过JAR包部署Spring Boot项目（需要使用JDK17）**
```
java -jar subscription-convert-1.0.0.jar -p 8080 -i config.yaml -context-path /sub
```
-p：指定项目监听端口号（默认8848），-i：外部项目配置文件的位置（默认在JAR同目录下查找），-context-path：指定项目监听根地址

**2. 外部项目配置文件config.yaml**
```
pathMap:
  a:
    sourceURL: 'https://ooo'
    sourceType: meta
    keepRegex: .*香港.*
  b:
    sourceURL: 'https://www'
    sourceType: meta
    deleteRegex: ~
    sortRegexArr:
      - .*香港.*
    renameMap:
      .*REJECT.*|.*DIRECT.*: $name
      .*: 我要去$name
```
- 访问`http://127.0.0.1:8848/sub/a`的时候，会编辑`https://ooo`中的节点，只保留名字中含有“香港”关键字的节点，返回新的订阅内容
- 访问`http://127.0.0.1:8848/sub/b`的时候，会编辑`https://www`中的节点，将名字中含有“香港”关键字的节点排序到第一位，其中名字中含有“REJECT”和“DIRECT”关键字的节点维持原名，剩下的节点名字都在前面加上“我要去”字符串，返回新的订阅内容
- `.*香港.*` `.*REJECT.*|.*DIRECT.*` `.*`均为正则表达式，表达式需要**完全匹配**节点的名字
- 如果节点名字匹配renameMap中多个正则表达式，会优先匹配第一个
- sourceType目前仅支持meta（clash）的yaml格式的订阅链接，不支持其他协议，不支持解码Base64
