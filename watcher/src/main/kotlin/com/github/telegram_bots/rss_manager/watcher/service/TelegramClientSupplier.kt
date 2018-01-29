package com.github.telegram_bots.rss_manager.watcher.service

import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramApiStorage
import com.github.badoualy.telegram.api.TelegramApp
import org.springframework.stereotype.Service

@Service
class TelegramClientSupplier(private val app: TelegramApp, private val cfgStorage: TelegramApiStorage) {
    fun getClient() = Kotlogram.getClient(app, cfgStorage)
}