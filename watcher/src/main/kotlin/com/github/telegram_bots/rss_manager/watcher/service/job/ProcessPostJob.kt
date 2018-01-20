package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.badoualy.telegram.api.utils.getAbsMediaInput
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
    null, is TLMessageMediaEmpty, is TLMessageMediaUnsupported -> RAW_TEXT
    is TLMessageMediaWebPage -> HTML_TEXT
    is TLMessageMediaPhoto -> IMAGE
    is TLMessageMediaDocument -> when (media?.getAbsMediaInput()?.mimeType) {
        "image/jpeg" -> IMAGE
        "image/webp" -> STICKER
        "video/mp4" -> VIDEO
        "audio/mpeg" -> AUDIO
        else -> FILE
    }
    is TLMessageMediaGeo -> GEO
    is TLMessageMediaGeoLive -> LIVE_GEO
    is TLMessageMediaInvoice -> INVOICE
    is TLMessageMediaContact -> CONTACT
    is TLMessageMediaVenue -> VENUE
    is TLMessageMediaGame -> GAME
    else -> throw IllegalArgumentException(media.toString())
}

internal fun TLMessage.formatContent(): String = when (media) {
    null, is TLMessageMediaEmpty, is TLMessageMediaUnsupported -> message
    is TLMessageMediaWebPage -> PostMessageFormatter.format(this)
    is TLMessageMediaPhoto -> ((media as TLMessageMediaPhoto?)?.caption ?: "")
    is TLMessageMediaDocument -> ((media as TLMessageMediaDocument?)?.caption ?: "")
    is TLMessageMediaGeo -> message
    is TLMessageMediaGeoLive -> message
    is TLMessageMediaInvoice -> message
    is TLMessageMediaContact -> message
    is TLMessageMediaVenue -> message
    is TLMessageMediaGame -> message
    else -> throw IllegalArgumentException(media.toString())
}