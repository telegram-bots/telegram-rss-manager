package com.github.telegram_bots.telegram_rss_manager.core.domain

import com.github.telegram_bots.telegram_rss_manager.core.domain.Types.{ChannelURL, PostID}

case class Channel(id: Int, url: ChannelURL, name: String, lastPostId: PostID)