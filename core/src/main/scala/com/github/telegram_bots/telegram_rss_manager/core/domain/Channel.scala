package com.github.telegram_bots.telegram_rss_manager.core.domain

import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel._
import com.github.telegram_bots.telegram_rss_manager.core.domain.Post.PostID

case class Channel(id: ChannelID, url: ChannelURL, name: ChannelName, lastPostId: PostID)

object Channel {
  type ChannelID = Int
  type ChannelURL = String
  type ChannelName = String
  type Worker = String
}