package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import com.github.telegram_bots.rss_manager.watcher.service.ChannelRepository
import io.reactivex.Maybe
import io.reactivex.rxkotlin.toMaybe
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Callable

open class GetChannelJob(
        private val repository: ChannelRepository, private val transaction: TransactionTemplate
) : Callable<Maybe<Channel>> {
    @Transactional
    override fun call(): Maybe<Channel> = Maybe
        .defer {
            val channel = transaction.execute {
                repository.firstNonUpdated()?.also { repository.lock(it) }
            }

            channel.toMaybe()
        }
}