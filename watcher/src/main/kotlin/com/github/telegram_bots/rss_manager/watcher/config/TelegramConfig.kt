package com.github.telegram_bots.rss_manager.watcher.config

import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramApiStorage
import com.github.badoualy.telegram.api.TelegramApp
import com.github.badoualy.telegram.api.TelegramClient
import com.github.telegram_bots.rss_manager.watcher.config.properties.TelegramProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TelegramConfig {
    @Bean
    fun telegramApp(props: TelegramProperties) = TelegramApp(
            apiId = props.apiId,
            apiHash = props.apiHash,
            deviceModel = props.model,
            systemVersion = props.sysVersion,
            appVersion = props.appVersion,
            langCode = props.langCode,
            systemLangCode= props.langCode
    )

    @Bean
    fun telegram(app: TelegramApp, cfgStorage: TelegramApiStorage): TelegramClient = Kotlogram.getClient(app, cfgStorage)
}
