package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.badoualy.telegram.tl.api.*
import com.github.telegram_bots.rss_manager.watcher.component.PostMessageFormatter
import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import com.github.telegram_bots.rss_manager.watcher.domain.Post
import com.github.telegram_bots.rss_manager.watcher.domain.PostType
import com.github.telegram_bots.rss_manager.watcher.domain.PostType.*
import io.reactivex.Single
import io.reactivex.functions.Function

class ProcessPostJob(private val channel: Channel) : Function<TLMessage, Single<Post>> {
    override fun apply(message: TLMessage): Single<Post> = Single.fromCallable {
        Post(
            id = message.id,
            type = message.getType(),
            content = message.formatContent(),
            date = message.date.toLong(),
            author = message.postAuthor,
            channelLink = channel.url,
            channelName = channel.name
        )
    }
}

internal fun TLMessage.getType(): PostType = when (media) {
    null, is TLMessageMediaEmpty, is TLMessageMediaUnsupported -> TEXT
    is TLMessageMediaPhoto -> PHOTO
    is TLMessageMediaDocument -> DOCUMENT
    is TLMessageMediaWebPage -> WEB_PAGE
    is TLMessageMediaGeo -> GEO
    is TLMessageMediaGeoLive -> LIVE_GEO
    is TLMessageMediaInvoice -> INVOICE
    is TLMessageMediaContact -> CONTACT
    is TLMessageMediaVenue -> VENUE
    is TLMessageMediaGame -> GAME
    else -> throw IllegalArgumentException(media.toString())
}

internal fun TLMessage.formatContent(): String = when (media) {
    null, is TLMessageMediaEmpty, is TLMessageMediaUnsupported -> PostMessageFormatter.format(this)
    is TLMessageMediaPhoto -> ((media as TLMessageMediaPhoto?)?.caption ?: "")
    is TLMessageMediaDocument -> ((media as TLMessageMediaDocument?)?.caption ?: "")
    is TLMessageMediaWebPage -> message
    is TLMessageMediaGeo -> message
    is TLMessageMediaGeoLive -> message
    is TLMessageMediaInvoice -> message
    is TLMessageMediaContact -> message
    is TLMessageMediaVenue -> message
    is TLMessageMediaGame -> message
    else -> throw IllegalArgumentException(media.toString())
}