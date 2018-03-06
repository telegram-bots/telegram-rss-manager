package com.github.telegram_bots.own.domain

import com.github.telegram_bots.own.domain.PostType.PostType
import com.github.telegram_bots.own.domain.Types.{Author, FileURL}

case class Post(
     id: Int,
     `type`: PostType,
     content: String,
     fileURL: FileURL = Option.empty,
     date: Long,
     author: Author = Option.empty,
     channelLink: String,
     channelName: String
)

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

case class ProcessedPost(channelUrl: String, batchId: Int, postId: Int, post: Option[Post]) {
  override def canEqual(that: Any): Boolean = that.isInstanceOf[ProcessedPost]

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: ProcessedPost => that.canEqual(this) &&
      channelUrl == that.channelUrl &&
      postId == that.postId
    case _ => false
  }

  override def hashCode(): Int = {
    val prime = 31
    var result = 1
    result = prime * result + postId
    result = prime * result + (if (channelUrl == null) 0 else channelUrl.hashCode)

    result
  }
}
