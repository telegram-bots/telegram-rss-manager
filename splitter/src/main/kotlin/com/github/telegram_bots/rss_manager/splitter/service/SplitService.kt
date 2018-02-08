package com.github.telegram_bots.rss_manager.splitter.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.telegram_bots.rss_manager.splitter.domain.Message
import com.github.telegram_bots.rss_manager.splitter.domain.Post
import com.github.telegram_bots.rss_manager.splitter.domain.Subscription
import com.github.telegram_bots.rss_manager.splitter.service.job.IterateSubscriptionsJob
import com.github.telegram_bots.rss_manager.splitter.service.job.MakeMessageJob
import com.github.telegram_bots.rss_manager.splitter.service.job.SendMessageToQueueJob
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.zipWith
import mu.KLogging
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.cloud.stream.messaging.Sink
import org.springframework.stereotype.Service
import javax.annotation.PreDestroy

@Service
@EnableBinding(Sink::class)
class SplitService(
        private val source: DynamicSource,
        private val repository: SubscriptionRepository,
        private val jsonMapper: ObjectMapper
) {
    companion object : KLogging()

    private var disposable: Disposable? = null

    @StreamListener(Sink.INPUT)
    fun subscribe(post: Post) {
        enableExceptionPropagate()

        Single.just(post)
                .flatMapPublisher(this::getSubscribers)
                .flatMapSingle(this::mapToMessage)
                .flatMapCompletable(this::sendToQueue)
                .doOnSubscribe(this::onSubscribe)
                .doOnError(this::onError)
                .blockingAwait()
    }

    @PreDestroy
    fun onDestroy() {
        disposable?.dispose()
    }

    private fun getSubscribers(post: Post): Flowable<Pair<Subscription, Post>> =
            IterateSubscriptionsJob(repository, 100)
                .apply(post)
                .zipWith(Flowable.just(post))

    private fun mapToMessage(pair: Pair<Subscription, Post>): Single<Message> = MakeMessageJob(jsonMapper).apply(pair)

    private fun sendToQueue(message: Message): Completable = SendMessageToQueueJob(source)
            .apply(message)
            .doOnSubscribe { logger.debug { "[SEND TO QUEUE] $message" } }

    private fun onSubscribe(disposable: Disposable) {
        this.disposable = disposable
    }

    private fun onError(throwable: Throwable) = logger.error("[ERROR]", throwable)

    private fun enableExceptionPropagate() = Thread.currentThread().setUncaughtExceptionHandler { _, e -> throw e }
}