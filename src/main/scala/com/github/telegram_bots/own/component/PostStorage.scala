package com.github.telegram_bots.own.component

import com.github.telegram_bots.own.domain.{EmptyPost, Post, PresentPost}

import scala.collection.mutable.ListBuffer

class PostStorage {
  private val internalStorage = ListBuffer[ProcessedPost]()

  def getAndRemove(channelUrl: String): List[Post] = {
    val posts = internalStorage.filter(_.channelUrl == channelUrl).sortBy(_.postId).toList

    internalStorage --= posts

    posts.filter(_.post.isInstanceOf[PresentPost]).map(_.post)
  }

  def append(batchId: Int, post: Post): Unit = {
    internalStorage += ProcessedPost(post.channelLink, batchId, post.id, post)
  }

  def count(channelUrl: String): Int = internalStorage.count(_.channelUrl == channelUrl)

  def countEmpty(channelUrl: String, batchId: Int): Int =
    internalStorage.count(p => p.channelUrl == channelUrl && p.batchId == batchId && p.post.isInstanceOf[EmptyPost])

  private case class ProcessedPost(channelUrl: String, batchId: Int, postId: Int, post: Post) {
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
}
