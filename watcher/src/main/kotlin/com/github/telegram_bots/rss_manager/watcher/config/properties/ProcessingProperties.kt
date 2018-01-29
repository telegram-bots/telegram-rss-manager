package com.github.telegram_bots.rss_manager.watcher.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("processing")
data class ProcessingProperties(
        var channelProcessingDelay: Long,
        var downloadBatchSize: Int,
        var downloadConcurrency: Int
)
