package com.document

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@SpringBootApplication
class DocumentApplication

fun main(args: Array<String>) {
    runApplication<DocumentApplication>(*args)
}

@RestController
@RequestMapping("/document")
class DocumentRestController {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    @PostMapping
    fun upload(
            @RequestPart document: MultipartFile,
            @RequestPart details: DocumentDetails
    ): Map<String, String> {
        return mapOf("document" to document.originalFilename!!)
    }

}

data class DocumentDetails(
        val name: String,
        val size: Long
)
