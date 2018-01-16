package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.badoualy.telegram.api.TelegramClient
import com.github.badoualy.telegram.tl.api.TLAbsMessage
import com.github.badoualy.telegram.tl.api.TLInputPeerChannel
import com.github.badoualy.telegram.tl.api.messages.TLAbsMessages
import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import io.reactivex.Single
import io.reactivex.functions.Function

class ResolveChannelLastPostIdJob(private val client: TelegramClient) : Function<Channel, Single<Channel>> {
    override fun apply(channel: Channel): Single<Channel> = Single.just(channel)
            .flatMap { ch ->
                if (ch.lastPostId != Channel.EMPTY_LAST_POST_ID) Single.just(ch)
                else {
                    Single.just(ch)
                            .map { TLInputPeerChannel(ch.telegramId, ch.hash) }
                            .flatMap { client.messagesGetHistory(it, 0, 0, 0,1, -1, 1) }
                            .flattenAsObservable(TLAbsMessages::messages)
                            .singleOrError()
                            .map(TLAbsMessage::id)
                            .map { ch.copy(lastPostId = it - 1) }
                }
            }
            .onErrorReturn { throw FailedToResolveLastPostIdException(channel) }
}

class FailedToResolveLastPostIdException(channel: Channel) : RuntimeException(
        "Failed to resolve lastPostId of $channel", null, false, false
)
