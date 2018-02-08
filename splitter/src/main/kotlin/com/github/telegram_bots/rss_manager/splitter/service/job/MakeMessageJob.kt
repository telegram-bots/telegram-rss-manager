package com.github.telegram_bots.rss_manager.splitter.service.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.telegram_bots.rss_manager.splitter.domain.Message
import com.github.telegram_bots.rss_manager.splitter.domain.Post
import com.github.telegram_bots.rss_manager.splitter.domain.Subscription
import io.reactivex.Single
import io.reactivex.functions.Function

class MakeMessageJob(private val jsonMapper: ObjectMapper) : Function<Pair<Subscription, Post>, Single<Message>> {
    override fun apply(pair: Pair<Subscription, Post>): Single<Message> =
            Single.just(pair).map { (sub, post) ->
                Message(
                        id = "${sub.name}.${sub.userId}",
                        content = jsonMapper.writeValueAsString(post)
                )
            }
}