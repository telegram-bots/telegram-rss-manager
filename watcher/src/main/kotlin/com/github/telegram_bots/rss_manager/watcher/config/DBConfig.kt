package com.github.telegram_bots.rss_manager.watcher.config

import org.davidmoten.rx.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.net.URI

@Configuration
class DBConfig {
    @Bean
    fun database(env: Environment): Database =
            Database.from(env.getProperty("spring.datasource.url").normalizeURI(), 10)

    private fun String.normalizeURI(): String {
        val uri = let(::URI)
        val query = uri.userInfo?.split(":")
                ?.let {
                    listOf(
                            it.getOrNull(0)?.let { "user=$it" },
                            it.getOrNull(1)?.let { "password=$it" }
                    )
                }
                ?.filterNotNull()
                ?.joinToString(separator = "&", prefix = "?")
                ?: ""

        return "jdbc:${uri.scheme}://${uri.host}:${uri.port}${uri.path}$query";
    }
}