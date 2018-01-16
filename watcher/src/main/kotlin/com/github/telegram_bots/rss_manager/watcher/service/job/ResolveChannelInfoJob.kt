package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.tl.api.TLChannel
import com.github.badoualy.telegram.tl.api.contacts.TLResolvedPeer
import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import io.reactivex.Single
import io.reactivex.functions.Function
import java.time.Instant

class ResolveChannelInfoJob(private val client: TelegramClient) : Function<Channel, Single<Channel>> {
    override fun apply(channel: Channel): Single<Channel> = Single.just(client)
            .flatMap { it.contactsResolveUsername(channel.url) }
            .flattenAsObservable(TLResolvedPeer::chats)
            .ofType(TLChannel::class.java)
            .filter { channel.url.equals(it.username, true) }
            .map {
                Channel(
                    id = channel.id,
                    telegramId = it.id,
                    hash = it.accessHash!!,
                    url = it.username!!.toLowerCase() ,
                    name = it.title,
                    lastPostId = channel.lastPostId,
                    lastSentId = channel.lastSentId,
                    lastInfoUpdate = Instant.now(),
                    createdAt = Instant.now()
                )
            }
            .singleOrError()
            .onErrorReturn { throw NoSuchChannelException(channel) }

    class NoSuchChannelException(channel: Channel) : RuntimeException("No such channel $channel", null, false, false)
}
