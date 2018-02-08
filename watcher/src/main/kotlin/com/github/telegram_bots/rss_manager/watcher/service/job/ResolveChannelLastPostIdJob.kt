package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.tl.api.TLAbsMessage
import com.github.badoualy.telegram.tl.api.TLChannel
import com.github.badoualy.telegram.tl.api.TLInputPeerChannel
import com.github.badoualy.telegram.tl.api.contacts.TLResolvedPeer
import com.github.badoualy.telegram.tl.api.messages.TLAbsMessages
import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import io.reactivex.Single
import io.reactivex.functions.Function

class ResolveChannelLastPostIdJob(private val client: TelegramClient) : Function<Channel, Single<Channel>> {
    override fun apply(channel: Channel): Single<Channel> =  Single.just(channel)
            .flatMap { client.contactsResolveUsername(it.url) }
            .flattenAsObservable(TLResolvedPeer::chats)
            .ofType(TLChannel::class.java)
            .filter { channel.url.equals(it.username, true) }
            .map { TLInputPeerChannel(it.id, it.accessHash!!) }
            .flatMapSingle { client.messagesGetHistory(it, 0, 0, 0,1, -1, 1) }
            .flatMapIterable(TLAbsMessages::messages)
            .singleOrError()
            .map(TLAbsMessage::id)
            .map { channel.copy(lastPostId = it - 1) }
            .doOnSubscribe { client.accountUpdateStatus(false) }
            .doOnSuccess { client.accountUpdateStatus(true) }
            .doAfterTerminate(client::close)
            .onErrorReturn { throw FailedToResolveLastPostIdException(channel) }
}

class FailedToResolveLastPostIdException(channel: Channel) : RuntimeException(
        "Failed to resolve lastPostId of $channel", null, false, false
)
