package com.github.telegram_bots.rss_manager.splitter.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("spring.cloud.stream.bindings.output")
data class RabbitSourceProperties(
        var destination: String,
        var contentType: String,
        var producer: Map<String, Any>
)