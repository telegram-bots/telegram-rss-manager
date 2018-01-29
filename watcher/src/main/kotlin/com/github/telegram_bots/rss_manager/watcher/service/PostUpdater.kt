package com.github.telegram_bots.rss_manager.watcher.service

import com.github.badoualy.telegram.tl.exception.RpcErrorException
import com.github.telegram_bots.rss_manager.watcher.config.properties.ProcessingProperties
import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import com.github.telegram_bots.rss_manager.watcher.domain.Post
import com.github.telegram_bots.rss_manager.watcher.service.job.DownloadPostsJob
import com.github.telegram_bots.rss_manager.watcher.service.job.ProcessPostJob
import com.github.telegram_bots.rss_manager.watcher.service.job.ResolveChannelLastPostIdJob
import com.github.telegram_bots.rss_manager.watcher.service.job.SendPostToQueueJob
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import mu.KLogging
import org.jsoup.nodes.Document
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Source
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy

@Service
@EnableBinding(Source::class)
class PostUpdater(
        private val props: ProcessingProperties,
        private val clientSupplier: TelegramClientSupplier,
        private val source: Source,
        private val repository: ChannelRepository
) : ApplicationListener<ApplicationReadyEvent> {
    companion object : KLogging()

    private val scheduler = Schedulers.from(Executors.newSingleThreadExecutor({ Thread(it, "posts-updater") }))
    private var disposable: Disposable? = null

    @PreDestroy
    fun onDestroy() {
        disposable?.dispose()
    }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        iterateChannels()
                .flatMapSingle(this::resolve)
                .flatMapSingle(this::download)
                .flatMap(this::process)
                .flatMapSingle(this::sendToQueue)
                .flatMapCompletable(this::markAsDownloaded)
                .doOnSubscribe(this::onSubscribe)
                .doOnTerminate(this::onTerminate)
                .doOnError(this::onError)
                .observeOn(scheduler)
                .repeatedTask()
    }

    private fun iterateChannels() = repository.list()
            .concatMap { Flowable.just(it).delay(5, TimeUnit.SECONDS) }
            .doOnNext { logger.info { "[PROCESS CHANNEL] $it" } }

    private fun resolve(channel: Channel) = Single.just(channel)
            .flatMap { ch ->
                if (!channel.isNew()) Single.just(ch)
                else Single.just(ch)
                        .flatMap(ResolveChannelLastPostIdJob(clientSupplier))
                        .flatMap { repository.update(it).andThen(Single.just(it)) }
                        .retry(this::retry)
                        .doOnSuccess { logger.info { "[RESOLVE CHANNEL] $ch" } }
            }

    private fun download(channel: Channel) = Single.just(channel)
            .retry(this::retry)
            .flatMapPublisher(DownloadPostsJob(
                    batchSize = props.downloadBatchSize,
                    maxConcurrency = props.downloadConcurrency,
                    scheduler = Schedulers.io()
            ))
            .toList()
            .zipWith(Single.just(channel), { msgs, ch -> ch to msgs })
            .doOnSuccess { (ch, msgs) -> logger.info { "[DOWNLOAD POSTS] ${msgs.size}x $ch" } }

    private fun process(pair: Pair<Channel, List<Document>>): Flowable<Pair<Channel, Post>> {
        val (channel, msgs) = pair

        return Flowable.fromIterable(msgs)
                .flatMapSingle(ProcessPostJob())
                .sorted { p1, p2 -> p1.id.compareTo(p2.id) }
                .zipWith(Single.just(channel).repeat())
                .map { (msg, ch) -> ch to msg }
                .doOnSubscribe { logger.info { "[PROCESS POSTS] ${msgs.size}x $channel" } }
    }

    private fun sendToQueue(pair: Pair<Channel, Post>): Single<Pair<Channel, Post>> {
        val (_, post) = pair

        return Single.just(post)
                .flatMapCompletable(SendPostToQueueJob(source))
                .doOnSubscribe { logger.debug { "[SEND TO QUEUE] $post" } }
                .andThen(Single.just(pair))
    }

    private fun markAsDownloaded(pair: Pair<Channel, Post>) = Single.just(pair)
            .map { (channel, post) -> channel.copy(lastPostId = post.id) }
            .doOnSuccess { logger.debug { "[MARK DOWNLOADED] $it" } }
            .flatMapCompletable(repository::update)

    private fun onSubscribe(disposable: Disposable) {
        this.disposable = disposable
        logger.info { "[START POST UPDATER]" }
    }

    private fun onTerminate() = logger.info { "[STOP POST UPDATER]" }

    private fun onError(throwable: Throwable) = logger.error("[ERROR]", throwable)

    private fun retry(count: Int, e: Throwable): Boolean {
        return when {
            count < 10 && e is RpcErrorException && e.code == 420 -> {
                TimeUnit.SECONDS.sleep((e.typeValue / 2).toLong())
                return true
            }
            else -> false
        }
    }

    private fun Completable.repeatedTask() {
        doOnComplete { this.repeatedTask() }.subscribe()
    }
}

