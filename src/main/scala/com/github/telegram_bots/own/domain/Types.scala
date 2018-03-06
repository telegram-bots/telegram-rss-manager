package com.github.telegram_bots.own.domain

object Types {
  type FileURL = Option[String]

  type Author = Option[String]

  case class Proxy(host: String, port: Int)

  object Proxy {
    val EMPTY = Proxy("127.0.0.1", 80)
  }
}
