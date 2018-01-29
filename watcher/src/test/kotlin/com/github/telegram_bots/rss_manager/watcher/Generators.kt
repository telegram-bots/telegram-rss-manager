package com.github.telegram_bots.rss_manager.watcher

import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import io.kotlintest.properties.Gen

class ChannelGenerator : Gen<Channel> {
    override fun generate(): Channel = Channel(
            id = Gen.int().generate(),
            url = Gen.string().generate(),
            name = Gen.string().generate(),
            lastPostId = Gen.int().generate()
    )
}