package com.github.telegram_bots.rss_manager.watcher.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
@ConfigurationProperties("telegram")
data class TelegramProperties(
        var apiId: Int,
        var apiHash: String,
        var phoneNumber: String,
        var model: String,
        var appVersion: String,
        var sysVersion: String,
        var langCode: String,
        var storagePath: Path
)
