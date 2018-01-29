package com.github.telegram_bots.rss_manager.watcher.domain

import java.sql.ResultSet

data class Channel(
        val id: Int,
        val url: String,
        val name: String,
        val lastPostId: Int
) {
    companion object {
        const val EMPTY_LAST_POST_ID = -1
    }

    constructor(rs: ResultSet) : this(
            id = rs.getInt("id"),
            url = rs.getString("url"),
            name = rs.getString("name"),
            lastPostId = rs.getObject("last_post_id")?.let { it as? Int } ?: EMPTY_LAST_POST_ID
    )

    fun isNew() = lastPostId == EMPTY_LAST_POST_ID
}
