package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import com.github.telegram_bots.rss_manager.watcher.extension.RetryWithDelay
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class DownloadPostsJob(
        private val batchSize: Int,
        private val maxConcurrency: Int = Int.MAX_VALUE,
        private val scheduler: Scheduler = Schedulers.single()
) : Function<Channel, Flowable<Document>> {
    override fun apply(channel: Channel): Flowable<Document> = Flowable.range(channel.lastPostId + 1, batchSize)
            .window(5)
            .flatMapSingle {
                it.map { postId -> channel.toLink(postId) }
                        .flatMapMaybe({ it.download(scheduler) }, false, maxConcurrency)
                        .toList()
            }
            .takeWhile { it.isNotEmpty() }
            .flatMapIterable { it }
}

private fun Channel.toLink(postId: Int) = "https://t.me/$url/$postId?embed=1&single=1"

private fun String.connect() = Jsoup.connect(this)
        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
        .headers(mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
                "Accept-Encoding" to "gzip, deflate, br",
                "Accept-Language" to "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
                "Cache-Control" to "max-age=0",
                "Connection" to "keep-alive",
                "Host" to "t.me",
                "DNT" to "1",
                "Upgrade-Insecure-Requests" to "1"
        ))

private fun String.download(scheduler: Scheduler): Maybe<Document> = Maybe.just(this)
        .subscribeOn(scheduler)
        .map(String::connect)
        .map(Connection::get)
        .flatMap { data ->
            val error = data.select(".tgme_widget_message_error").text().trim()

            when {
                error == "Post not found" -> Maybe.empty()
                "Channel with username" in error -> Maybe.error(PostDownloadError(error))
                else -> Maybe.just(data)
            }
        }
        .retryWhen(RetryWithDelay(delay = 5 to TimeUnit.SECONDS))

class PostDownloadError(error: String) : RuntimeException(error, null, false, false)