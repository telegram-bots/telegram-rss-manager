package com.github.telegram_bots.rss_manager.web

import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.core.env.Environment
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files.*
import java.nio.file.Path

@SpringBootApplication
@RestController
class WebApplication(private val env: Environment) : ApplicationRunner {
    @Value("\${storage.path}")
    private lateinit var path: Path

    @GetMapping("feed/{userId}/{subscriptionName}", produces = [MediaType.APPLICATION_RSS_XML_VALUE])
    fun get(@PathVariable userId: Long, @PathVariable subscriptionName: String): ResponseEntity<Any> {
        val file = path.resolve("${userId}_$subscriptionName.xml")
        return when {
            !exists(file) -> ResponseEntity.notFound().build()
            else -> ResponseEntity.ok()
                    .contentLength(size(file))
                    .body(InputStreamResource(newInputStream(file)))
        }
    }

    override fun run(args: ApplicationArguments) {
        logger.info { env }
    }

    companion object : KLogging() {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(WebApplication::class.java, *args)
        }
    }
}
