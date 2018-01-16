package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.telegram_bots.rss_manager.watcher.domain.Post
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.Function
import org.springframework.cloud.stream.messaging.Source
import org.springframework.messaging.support.MessageBuilder

class SendPostToQueueJob(private val source: Source) : Function<Post, Completable> {
    override fun apply(post: Post): Completable = Single.just(post)
            .map { MessageBuilder.withPayload(it).build() }
            .map(source.output()::send)
            .doOnSuccess { if (!it) throw FailedToSendToQueueException(post) }
            .toCompletable()
}

class FailedToSendToQueueException(post: Post) :
        RuntimeException("Failed to send to queue $post", null, false, false)
