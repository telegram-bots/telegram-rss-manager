package com.github.telegram_bots.rss_manager.web.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.telegram_bots.rss_manager.web.component.TelegramFeedGenerator.outputAsString
import com.github.telegram_bots.rss_manager.web.domain.Post
import io.reactivex.Single
import mu.KLogging
import org.springframework.stereotype.Service
import com.github.telegram_bots.rss_manager.web.component.TelegramFeedGenerator.generate as generateFeed


@Service
class FeedRetriever(private val sink: DynamicSink, private val jsonMapper: ObjectMapper) {
    companion object : KLogging()

    fun retrieve(userId: Long, subscriptionName: String) = sink.consume("$subscriptionName.$userId")
            .map { (tag, body) -> tag to jsonMapper.readValue(body, Post::class.java) }
            .reduce(
                    0L to mutableListOf<Post>(),
                    { (maxTag, entries), (tag, entry) -> Math.max(maxTag, tag) to entries.apply { add(entry) } }
            )
            .map { (maxTag, entries) -> maxTag to generateFeed(subscriptionName, userId, entries).outputAsString() }
            .flatMap { (maxTag, feed) -> Single.just(feed).doAfterTerminate { sink.unacknowledge(maxTag) } }
}