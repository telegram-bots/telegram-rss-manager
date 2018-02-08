package com.github.telegram_bots.rss_manager.splitter.service.job

import com.github.telegram_bots.rss_manager.splitter.domain.Post
import com.github.telegram_bots.rss_manager.splitter.domain.Subscription
import com.github.telegram_bots.rss_manager.splitter.service.SubscriptionRepository
import io.reactivex.Flowable
import io.reactivex.functions.Function

class IterateSubscriptionsJob(private val repository: SubscriptionRepository, private val maxPerBatch: Int)
    : Function<Post, Flowable<Subscription>> {
    override fun apply(post: Post): Flowable<Subscription> =
            Flowable.range(0, Int.MAX_VALUE)
                    .window(maxPerBatch.toLong())
                    .flatMapMaybe { it.firstElement() }
                    .map { repository.getSubscriptions(post.channelLink, maxPerBatch, it) }
                    .takeWhile { it.isNotEmpty() }
                    .flatMapIterable { it }
}