package com.github.telegram_bots.rss_manager.watcher.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("cloud")
data class CloudProperties(
        var url: String
)