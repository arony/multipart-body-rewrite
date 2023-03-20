package com.legacy

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@SpringBootApplication
class LegacyApplication

fun main(args: Array<String>) {
    runApplication<LegacyApplication>(*args)
}

@RestController
@RequestMapping("/file")
class LegacyFileRestController {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    @PostMapping
    fun upload(
            @RequestPart(required = false) file: MultipartFile?,
            @RequestPart(required = false) document: MultipartFile?
    ): Map<String, String> {
        if (document != null) {
            return mapOf("documentname" to document.originalFilename!!)
        }

        if (file != null) {
            return mapOf("filename" to file.originalFilename!!)
        }

        return mapOf("message" to "nothing found")

    }

}