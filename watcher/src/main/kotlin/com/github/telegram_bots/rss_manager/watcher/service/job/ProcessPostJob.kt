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

internal fun TLMessage.getType(): PostType = when {
    media is TLMessageMediaWebPage || entities?.isNotEmpty() ?: false -> HTML_TEXT
    media == null || media is TLMessageMediaEmpty || media is TLMessageMediaUnsupported -> RAW_TEXT
    media is TLMessageMediaPhoto -> IMAGE
    media is TLMessageMediaDocument -> when (media?.getAbsMediaInput()?.mimeType) {
        "image/jpeg" -> IMAGE
        "image/webp" -> STICKER
        "video/mp4" -> VIDEO
        "audio/mpeg" -> AUDIO
        else -> FILE
    }
    media is TLMessageMediaGeo -> GEO
    media is TLMessageMediaGeoLive -> LIVE_GEO
    media is TLMessageMediaInvoice -> INVOICE
    media is TLMessageMediaContact -> CONTACT
    media is TLMessageMediaVenue -> VENUE
    media is TLMessageMediaGame -> GAME
    else -> throw IllegalArgumentException(media.toString())
}

internal fun TLMessage.formatContent(): String = when {
    media is TLMessageMediaWebPage || entities?.isNotEmpty() ?: false -> PostMessageFormatter.format(this)
    media == null || media is TLMessageMediaEmpty || media is TLMessageMediaUnsupported -> message
    media is TLMessageMediaPhoto -> ((media as TLMessageMediaPhoto?)?.caption ?: "")
    media is TLMessageMediaDocument -> ((media as TLMessageMediaDocument?)?.caption ?: "")
    media is TLMessageMediaGeo -> message
    media is TLMessageMediaGeoLive -> message
    media is TLMessageMediaInvoice -> message
    media is TLMessageMediaContact -> message
    media is TLMessageMediaVenue -> message
    media is TLMessageMediaGame -> message
    else -> throw IllegalArgumentException(media.toString())
}