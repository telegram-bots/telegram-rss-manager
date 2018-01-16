package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.telegram_bots.rss_manager.watcher.ChannelGenerator
import com.github.telegram_bots.rss_manager.watcher.TLMessageGenerator
import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec

class ProcessPostJobTest : StringSpec({
    "should correctly process given post" {
        forAll(ChannelGenerator(), TLMessageGenerator(), { channel, message ->
            val post = ProcessPostJob(channel).apply(message).blockingGet()

            post.id shouldBe message.id
            post.author shouldBe message.postAuthor
            post.date shouldBe message.date.toLong()
            post.channelLink shouldBe channel.url
            post.channelName shouldBe channel.name
            post.content shouldBe message.formatContent()
            post.type shouldBe message.getType()

            true
        })
    }
})