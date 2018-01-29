package com.github.telegram_bots.rss_manager.watcher.service

import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import io.reactivex.Completable
import io.reactivex.Flowable
import org.davidmoten.rx.jdbc.Database
import org.springframework.stereotype.Repository

@Repository
class ChannelRepository(private val db: Database) {
    fun update(channel: Channel): Completable = db
            .update(
                """
                | UPDATE channels
                | SET last_post_id = CASE WHEN ? > last_post_id OR last_post_id IS NULL THEN ? ELSE last_post_id END
                | WHERE id = ?
                """.trimMargin()
            )
            .parameters(
                    channel.lastPostId,
                    channel.lastPostId,
                    channel.id
            )
            .complete()

    fun list(): Flowable<Channel> =
            db.select("""SELECT * FROM channels ORDER BY last_post_id DESC""").get(::Channel)
}
