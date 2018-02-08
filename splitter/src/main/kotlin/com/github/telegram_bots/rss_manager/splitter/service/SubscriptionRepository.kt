package com.github.telegram_bots.rss_manager.splitter.service

import com.github.telegram_bots.rss_manager.splitter.domain.Subscription
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class SubscriptionRepository(private val jdbc: JdbcTemplate) {
    fun getSubscriptions(channelLink: String, limit: Int, offset: Int): List<Subscription> = jdbc
        .query(
                """
                | SELECT s.name, u.telegram_id
                | FROM subscriptions AS s
                | JOIN channels AS c ON c.id = s.channel_id
                | JOIN users AS u ON u.id = s.user_id
                | WHERE c.url = ?
                | LIMIT ? OFFSET ?
                """.trimMargin(),
                arrayOf(channelLink, limit, offset),
                { rs: ResultSet, _ -> Subscription(rs) }
        )
}
