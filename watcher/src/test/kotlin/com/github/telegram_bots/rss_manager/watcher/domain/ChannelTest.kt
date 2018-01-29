package com.github.telegram_bots.rss_manager.watcher.domain

import com.github.telegram_bots.rss_manager.watcher.ChannelGenerator
import io.kotlintest.properties.forAll
import io.kotlintest.specs.FeatureSpec

class ChannelTest : FeatureSpec({
    feature("isNew") {
        scenario("should be true when lastPostId is empty") {
            forAll(ChannelGenerator(), { channel: Channel ->
                channel.copy(lastPostId = Channel.EMPTY_LAST_POST_ID).isNew()
            })
        }

        scenario("should be false in all other situations") {
            forAll(ChannelGenerator(), { channel: Channel ->
                !channel.isNew()
            })
        }
    }
})