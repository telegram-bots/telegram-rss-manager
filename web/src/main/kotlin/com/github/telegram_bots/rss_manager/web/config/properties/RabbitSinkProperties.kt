package com.github.telegram_bots.rss_manager.web.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("spring.cloud.stream.bindings.input")
data class RabbitSinkProperties(
        var destination: String
)