package com.github.telegram_bots.rss_manager.watcher.domain

import com.github.telegram_bots.rss_manager.watcher.ChannelGenerator
import io.kotlintest.properties.forAll
import io.kotlintest.specs.FeatureSpec

class ChannelTest : FeatureSpec({
    feature("isEmpty") {
        scenario("should be true when telegramId is empty") {
            forAll(ChannelGenerator(), { channel: Channel ->
                channel.copy(telegramId = Channel.EMPTY_TG_ID).isEmpty()
            })
        }

        scenario("should be true when hash is empty") {
            forAll(ChannelGenerator(), { channel: Channel ->
                channel.copy(hash = Channel.EMPTY_HASH).isEmpty()
            })
        }

        scenario("should be true when lastPostId is empty") {
            forAll(ChannelGenerator(), { channel: Channel ->
                channel.copy(lastPostId = Channel.EMPTY_LAST_POST_ID).isEmpty()
            })
        }

        scenario("should be false in all other situations") {
            forAll(ChannelGenerator(), { channel: Channel ->
                !channel.isEmpty()
            })
        }
    }
})