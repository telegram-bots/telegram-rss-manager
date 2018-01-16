package com.github.telegram_bots.rss_manager.watcher

import com.github.badoualy.telegram.tl.api.*
import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import io.kotlintest.properties.Gen
import java.time.Instant

class ChannelGenerator : Gen<Channel> {
    override fun generate(): Channel = Channel(
            id = Gen.int().generate(),
            telegramId = Gen.int().generate(),
            hash = Gen.long().generate(),
            url = Gen.string().generate(),
            name = Gen.string().generate(),
            lastPostId = Gen.int().generate(),
            lastSentId = Gen.int().generate(),
            lastInfoUpdate = Instant.now(),
            createdAt = Instant.now()
    )
}

class TLMessageGenerator : Gen<TLMessage> {
    override fun generate(): TLMessage = TLMessage().apply {
        id = Gen.int().generate()
        date = Gen.int().generate()
        postAuthor = Gen.string().generate()
        message = Gen.string().generate()
        media = TLMessageMediaGenerator().generate()
    }
}

class TLMessageMediaGenerator : Gen<TLAbsMessageMedia?> {
    override fun generate(): TLAbsMessageMedia? = Gen.oneOf(listOf(
            null,
            TLMessageMediaEmpty(),
            TLMessageMediaPhoto(photo = null, caption = Gen.string().generate(), ttlSeconds = Gen.int().generate()),
            TLMessageMediaDocument(document = null, caption = Gen.string().generate(), ttlSeconds = Gen.int().generate()),
            TLMessageMediaWebPage(),
            TLMessageMediaGeo(),
            TLMessageMediaGeoLive(),
            TLMessageMediaInvoice(),
            TLMessageMediaContact(),
            TLMessageMediaVenue(),
            TLMessageMediaGame(),
            TLMessageMediaUnsupported()
    )).generate()
}