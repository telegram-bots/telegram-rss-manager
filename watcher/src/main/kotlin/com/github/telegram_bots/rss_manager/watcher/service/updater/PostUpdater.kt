package com.github.telegram_bots.rss_manager.watcher.service.updater

import com.cloudinary.Cloudinary
import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.tl.api.TLMessage
import com.github.badoualy.telegram.tl.exception.RpcErrorException
import com.github.telegram_bots.rss_manager.watcher.config.properties.ProcessingProperties
import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import com.github.telegram_bots.rss_manager.watcher.domain.Post
import com.github.telegram_bots.rss_manager.watcher.service.ChannelRepository
import com.github.telegram_bots.rss_manager.watcher.service.job.*
import com.github.telegram_bots.rss_manager.watcher.extension.randomDelay
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.zipWith
import mu.KLogging
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Source
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.annotation.PreDestroy

@Service
@EnableBinding(Source::class)
class PostUpdater(
        private val props: ProcessingProperties,
        private val client: TelegramClient,
        private val cloud: Cloudinary,
        private val source: Source,
        private val repository: ChannelRepository
) : AbstractUpdater("posts-updater") {
    companion object : KLogging()

    @PreDestroy
    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }

    override fun run(): Completable {
        return iterateChannels()
                .flatMapSingle(this::resolve)
                .flatMapMaybe(this::download)
                .flatMap(this::process)
                .flatMapSingle(this::sendToQueue)
                .flatMapCompletable(this::markAsDownloaded)
                .doOnSubscribe(this::onSubscribe)
                .doOnTerminate(this::onTerminate)
    }

    private fun iterateChannels(): Flowable<Channel> {
        return repository.list()
                .concatMap {
                    Single.just(it)
                            .randomDelay(
                                    delayMin = props.postsIntervalMin,
                                    delayMax = props.postsIntervalMax,
                                    unit = props.postsIntervalTimeUnit,
                                    scheduler = scheduler
                            )
                            .toFlowable()
                }
                .doOnNext { logger.info { "[PROCESS CHANNEL] $it" } }
    }

    private fun resolve(channel: Channel): Single<Channel> {
        return Single.just(channel)
                .flatMap { ch ->
                    if (!channel.isEmpty()) Single.just(ch)
                    else {
                        Single.just(ch)
                                .randomDelay(
                                        delayMin = props.channelsIntervalMin,
                                        delayMax = props.channelsIntervalMax,
                                        unit = props.channelsIntervalTimeUnit,
                                        scheduler = scheduler
                                )
                                .flatMap(ResolveChannelInfoJob(client))
                                .flatMap(ResolveChannelCreationDateJob(client))
                                .flatMap(ResolveChannelLastPostIdJob(client))
                                .flatMap { repository.update(it).andThen(Single.just(it)) }
                                .retry(this::retry)
                                .doOnSubscribe { client.accountUpdateStatus(false) }
                                .doOnSuccess { client.accountUpdateStatus(true) }
                                .doOnSuccess { logger.info { "[RESOLVE CHANNEL] $ch" } }
                    }
                }
    }

    private fun download(channel: Channel): Maybe<Pair<Channel, List<TLMessage>>> {
        return Single.just(channel)
                .retry(this::retry)
                .flatMapPublisher(DownloadPostsJob(client, props.postsBatchSize))
                .toList()
                .filter(List<TLMessage>::isNotEmpty)
                .zipWith(Maybe.just(channel), { msgs, ch -> ch to msgs })
                .doOnSuccess { (ch, msgs) -> logger.info { "[DOWNLOAD POSTS] ${msgs.size}x $ch" } }
                .doOnSubscribe { client.accountUpdateStatus(false) }
                .doOnComplete { client.accountUpdateStatus(true) }
    }

    private fun process(pair: Pair<Channel, List<TLMessage>>): Flowable<Pair<Channel, Post>> {
        val (channel, msgs) = pair

        return Flowable.fromIterable(msgs)
                .flatMapSingle {
                    Singles.zip(
                        ProcessPostJob(channel).apply(it),
                        UploadPostMediaJob(client, cloud).apply(it),
                        { post, fileURL -> post.copy(fileURL = fileURL.orElse(null)) }
                    )
                }
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

    private fun markAsDownloaded(pair: Pair<Channel, Post>): Completable {
        return Single.just(pair)
                .map { (channel, post) -> channel.copy(lastPostId = post.id) }
                .doOnSuccess { logger.debug { "[MARK DOWNLOADED] $it" } }
                .flatMapCompletable(repository::update)
    }

    private fun onSubscribe(disposable: Disposable) = logger.info { "[START POST UPDATER]" }

    private fun onTerminate() = logger.info { "[STOP POST UPDATER]" }

    private fun retry(count: Int, e: Throwable): Boolean {
        return when {
            count < 10 && e is RpcErrorException && e.code == 420 -> {
                TimeUnit.SECONDS.sleep((e.typeValue / 2).toLong())
                return true
            }
            else -> false
        }
    }
}

