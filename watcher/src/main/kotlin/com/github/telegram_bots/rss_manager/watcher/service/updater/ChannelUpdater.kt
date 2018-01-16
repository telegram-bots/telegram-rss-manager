package com.github.telegram_bots.rss_manager.watcher.service.updater

import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import com.github.telegram_bots.rss_manager.watcher.service.ChannelRepository
import com.github.telegram_bots.rss_manager.watcher.service.job.ScheduleChannelInfoUpdateJob
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import mu.KLogging
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

@Service
class ChannelUpdater(private val repository: ChannelRepository) : AbstractUpdater("channel-updater") {
    companion object : KLogging()

    override fun run(): Completable = iterateChannels()
            .flatMapCompletable(this::scheduleUpdate)
            .doOnSubscribe(this::onSubscribe)
            .doOnTerminate(this::onTerminate)
            .doOnComplete { TimeUnit.SECONDS.sleep(secondsUntilNextDay()) }

    private fun iterateChannels(): Flowable<Channel> {
        return repository.listNeedToUpdate()
    }

    private fun scheduleUpdate(channel: Channel): Completable {
        return Single.just(channel)
                .flatMapCompletable(ScheduleChannelInfoUpdateJob(repository))
                .doOnComplete { logger.info { "[SCHEDULED CHANNEL UPDATE] $channel" } }
    }

    private fun secondsUntilNextDay(): Long {
        return Duration
                .between(
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS)
                )
                .seconds
    }

    private fun onSubscribe(disposable: Disposable) = logger.info { "[START CHANNEL UPDATER]" }

    private fun onTerminate() = logger.info { "[STOP CHANNEL UPDATER]" }
}
