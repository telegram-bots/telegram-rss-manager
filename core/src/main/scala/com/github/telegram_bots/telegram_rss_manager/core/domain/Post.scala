package com.github.telegram_bots.telegram_rss_manager.core.domain

import java.time.LocalDateTime

import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel.ChannelURL
import com.github.telegram_bots.telegram_rss_manager.core.domain.Post.PostID
import com.github.telegram_bots.telegram_rss_manager.core.domain.Post.PostType.PostType

object Post {
  type PostID = Int

  object PostType extends Enumeration {
    type PostType = Value

    val TEXT = Value
    val IMAGE = Value
    val STICKER = Value
    val AUDIO = Value
    val VIDEO = Value
    val FILE = Value
    val GEO = Value
    val CONTACT = Value
  }
}

abstract sealed class Post(
  val id: PostID,
  val channelLink: ChannelURL
)

case class PresentPost(
  override val id: PostID,
  `type`: PostType,
  content: String,
  fileURL: Option[String] = Option.empty,
  date: LocalDateTime,
  author: Option[String] = Option.empty,
  override val channelLink: ChannelURL,
  channelName: String
) extends Post(id, channelLink)

case class EmptyPost(
  override val id: PostID,
  override val channelLink: ChannelURL
) extends Post(id, channelLink)
