package com.github.telegram_bots.own.component

import com.github.telegram_bots.own.domain.ProcessedPost

import scala.collection.mutable.ListBuffer

class PostStorage {
  private val internalStorage = ListBuffer[ProcessedPost]()

  def getAndRemove(channelUrl: String): List[ProcessedPost] = {
    val posts = internalStorage.filter(_.channelUrl == channelUrl).sortBy(_.postId).toList

    internalStorage --= posts

    posts
  }

  def append(post: ProcessedPost): Unit = {
    internalStorage += post
  }

  def count(channelUrl: String): Int = internalStorage.count(_.channelUrl == channelUrl)

  def countEmpty(channelUrl: String, batchId: Int): Int =
    internalStorage.count(p => p.channelUrl == channelUrl && p.batchId == batchId && p.post.isEmpty)
}
