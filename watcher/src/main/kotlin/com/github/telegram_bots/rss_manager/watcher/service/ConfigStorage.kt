package com.github.telegram_bots.rss_manager.watcher.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.badoualy.telegram.api.TelegramApiStorage
import com.github.badoualy.telegram.mtproto.auth.AuthKey
import com.github.badoualy.telegram.mtproto.auth.TempAuthKey
import com.github.badoualy.telegram.mtproto.model.DataCenter
import com.github.badoualy.telegram.mtproto.model.MTSession
import com.github.telegram_bots.rss_manager.watcher.config.properties.TelegramProperties
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Files.*
import java.nio.file.StandardOpenOption.*

@Component
class ConfigStorage(props: TelegramProperties, val mapper: ObjectMapper) : TelegramApiStorage {
    private val auth = props.storagePath.resolve("auth")

    init {
        createDirectories(auth.parent)
        if (!exists(auth)) {
            write(auth, System.lineSeparator().repeat(4).toByteArray(), CREATE, WRITE)
        }
    }

    override var authKey: AuthKey?
        get() = read(0, AuthKey::class.java)
        set(value) { write(0, value) }

    override var dataCenter: DataCenter?
        get() = read(1, DataCenter::class.java)
        set(value) { write(1, value) }

    override var session: MTSession?
        get() = read(2) {
            val params = mapper.readTree(it)

            MTSession(
                    dataCenter = mapper.treeToValue(params.get("dataCenter"), DataCenter::class.java),
                    id = mapper.treeToValue(params.get("id"), ByteArray::class.java),
                    salt = params.get("salt").longValue(),
                    contentRelatedCount = params.get("contentRelatedCount").intValue(),
                    lastMessageId = params.get("lastMessageId").longValue()
            )
        }
        set(value) { write(2, value) }

    override var tempAuthKey: TempAuthKey?
        get() = read(3, TempAuthKey::class.java)
        set(value) { write(3, value) }

    private fun write(pos: Int, content: Any?) {
        readAllLines(auth)
                .apply { this[pos] = if (content == null) "" else mapper.writeValueAsString(content)  }
                .run { write(auth, this, WRITE, TRUNCATE_EXISTING) }
    }

    private fun <T> read(pos: Int, clazz: Class<T>) = read(pos) { mapper.readValue(it, clazz) }

    private fun <T> read(pos: Int, mapper: (String) -> T) = Files.lines(auth)
            .skip(pos.toLong())
            .findFirst()
            .orElseGet { null }
            ?.let { if (it.isBlank()) null else it }
            ?.let(mapper)
}
