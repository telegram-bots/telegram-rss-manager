package com.github.telegram_bots.rss_manager.splitter.service.job

import com.github.telegram_bots.rss_manager.splitter.domain.Message
import com.github.telegram_bots.rss_manager.splitter.service.DynamicSource
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.Function

class SendMessageToQueueJob(private val source: DynamicSource) : Function<Message, Completable> {
    override fun apply(message: Message): Completable = Single.just(message)
            .map(source::send)
            .doOnSuccess { if (!it) throw FailedToSendToQueueException(message) }
            .toCompletable()
}

class FailedToSendToQueueException(message: Any)
    : RuntimeException("Failed to send to queue $message", null, false, false)
