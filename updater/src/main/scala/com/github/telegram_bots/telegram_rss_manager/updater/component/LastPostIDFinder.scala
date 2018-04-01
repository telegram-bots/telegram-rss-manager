package com.github.telegram_bots.telegram_rss_manager.updater.component

import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel.ChannelURL
import com.github.telegram_bots.telegram_rss_manager.core.domain.Post.PostID

import scala.annotation.tailrec

object LastPostIDFinder {
  type PostExists = Boolean
  type Path = List[(PostID, PostExists)]

  def findLastPostID(
    channelURL: ChannelURL,
    getter: (ChannelURL, PostID) => PostExists,
    startPostID: PostID = 2000
  ): (PostID, Path) = {
    def diff(bounds: Range) = ((bounds.start.toLong + bounds.end) / 2).toInt

    @tailrec
    def loop(currentPostID: PostID, bounds: Range, path: Path): (PostID, Path) = {
      val success = getter(channelURL, currentPostID)
      val newPath = path :+ (currentPostID, success)

      success match {
        case s if s && bounds.end - currentPostID == 1 =>
          (currentPostID, newPath)
        case true =>
          val newBounds = currentPostID to bounds.end
          val next = if (newBounds.end == Int.MaxValue) currentPostID * 2 else diff(newBounds)
          loop(next, newBounds, newPath)
        case false =>
          val newBounds = (bounds.start min currentPostID) to currentPostID
          val next = if (newBounds.start == currentPostID) currentPostID / 2 else diff(newBounds)
          loop(next, newBounds, newPath)
      }
    }

    loop(startPostID, startPostID to Int.MaxValue, List())
  }
}
