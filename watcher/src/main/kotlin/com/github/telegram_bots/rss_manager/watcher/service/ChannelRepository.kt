package com.github.telegram_bots.rss_manager.watcher.service

import com.github.telegram_bots.rss_manager.watcher.domain.Channel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class ChannelRepository(private val jdbc: JdbcTemplate) {
    fun update(channel: Channel) = jdbc
        .update(
                """
                | UPDATE channels
                | SET
                |   last_post_id = CASE WHEN ? > last_post_id OR last_post_id IS NULL THEN ? ELSE last_post_id END,
                |   in_work = ?,
                |   graceful_stop = ?
                | WHERE id = ?
                """.trimMargin(),
                channel.lastPostId,
                channel.lastPostId,
                channel.inWork,
                channel.gracefulStop,
                channel.id
        )

    fun firstNonUpdated() = jdbc
        .query(
                """
                | SELECT * FROM channels
                | WHERE in_work = FALSE OR (in_work = TRUE AND graceful_stop = FALSE)
                | ORDER BY updated_at DESC
                | LIMIT 1
                """.trimMargin(),
                { rs: ResultSet, _ -> Channel(rs) }
        )
        .firstOrNull()
}
