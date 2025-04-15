package xyz.basskick.subscriptionconvert

import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import xyz.basskick.subscriptionconvert.config.Config
import xyz.basskick.subscriptionconvert.process.Process

@Controller
@RequestMapping("/")
class Run {
    @GetMapping("/{path}")
    fun listen (@PathVariable path: String): ResponseEntity<String> {
        val errHeader = MultiValueMap.fromSingleValue(mapOf(
            "Content-Type" to "text/plain;charset=utf-8"
        ))
        val mappingConfig = Config.mappingConfigMap[path]
            ?: return ResponseEntity("{status: 404}", errHeader, HttpStatusCode.valueOf(404))
        val string = Process.readMappingConfig(mappingConfig).startProcess().getString()
        val okHeader = MultiValueMap.fromSingleValue(mapOf(
            "Type" to mappingConfig.sourceType.toString(),
            "Content-Type" to "text/plain;charset=utf-8"
        ))
        return ResponseEntity(string, okHeader, HttpStatusCode.valueOf(200))
    }
}