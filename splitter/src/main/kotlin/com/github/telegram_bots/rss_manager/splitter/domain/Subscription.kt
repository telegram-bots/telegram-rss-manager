package com.github.telegram_bots.rss_manager.splitter.domain

import java.sql.ResultSet

data class Subscription(val name: String, val userId: Long) {
    constructor(rs: ResultSet) : this(
            name = rs.getString("name"),
            userId = rs.getLong("telegram_id")
    )
}