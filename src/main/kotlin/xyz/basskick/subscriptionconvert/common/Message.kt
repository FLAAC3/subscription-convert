package xyz.basskick.subscriptionconvert.common

object Message {

    fun ok (string: String) = "\u001b[38;5;10m[Ciallo～]：\u001b[0m$string"

    fun err (string: String) = "\u001b[38;5;9m[Error～]：\u001b[0m$string"

    fun printlnOk (string: String) = println(ok(string))

    fun printlnErr (string: String) = println(err(string))
}