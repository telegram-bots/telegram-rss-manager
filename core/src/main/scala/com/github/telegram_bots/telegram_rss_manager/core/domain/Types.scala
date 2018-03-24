package com.github.telegram_bots.telegram_rss_manager.core.domain

object Types {
  type PostID = Int

  type ChannelURL = String

  case class Proxy(host: String, port: Int)

  object Proxy {
    val EMPTY = Proxy("127.0.0.1", 80)
  }
}