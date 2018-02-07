package com.github.telegram_bots.rss_manager.watcher.config

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.net.URI
import javax.sql.DataSource

@Configuration
class DBConfig {
    @Bean
    fun dataSource(env: Environment): DataSource = DataSourceBuilder
        .create()
        .url(env.getProperty("spring.datasource.url").normalizeURI())
        .build()

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

        return "jdbc:${uri.scheme}://${uri.host}:${uri.port}${uri.path}$query"
    }
}