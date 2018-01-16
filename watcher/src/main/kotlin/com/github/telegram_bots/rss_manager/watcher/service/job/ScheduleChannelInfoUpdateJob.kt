package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import com.github.telegram_bots.rss_manager.watcher.service.ChannelRepository
import io.reactivex.Completable
import io.reactivex.functions.Function

class ScheduleChannelInfoUpdateJob(private val repository: ChannelRepository) : Function<Channel, Completable> {
    override fun apply(channel: Channel) = repository.update(channel.copy(hash = Channel.EMPTY_HASH))
}
