package com.github.telegram_bots.rss_manager.watcher.domain

typealias FileURL = String

data class Post(
        val id: Int,
        val type: PostType,
        val content: String,
        val fileURL: FileURL? = null,
        val date: Long,
        val author: String? = null,
        val channelLink: String,
        val channelName: String
)