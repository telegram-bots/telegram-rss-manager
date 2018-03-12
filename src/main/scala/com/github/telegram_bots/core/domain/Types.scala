package com.github.telegram_bots.core.domain

object Types {
  type PostID = Int

  type ChannelURL = String

  type ID = Option[Int]

  case class Proxy(host: String, port: Int)

  object Proxy {
    val EMPTY = Proxy("127.0.0.1", 80)
  }
}
