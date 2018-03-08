package com.github.telegram_bots.own.domain

import com.github.telegram_bots.own.domain.PostType.PostType
import com.github.telegram_bots.own.domain.Types.{Author, ChannelURL, FileURL, PostID}

abstract sealed class Post(
  val channelLink: ChannelURL,
  val id: PostID
)

case class PresentPost(
  override val id: PostID,
  `type`: PostType,
  content: String,
  fileURL: FileURL = Option.empty,
  date: Long,
  author: Author = Option.empty,
  override val channelLink: ChannelURL,
  channelName: String
) extends Post(channelLink, id)

case class EmptyPost(
  override val id: PostID,
  override val channelLink: ChannelURL
) extends Post(channelLink, id)

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
