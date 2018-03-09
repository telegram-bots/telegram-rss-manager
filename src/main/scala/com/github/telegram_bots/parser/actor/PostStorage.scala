package com.github.telegram_bots.parser.actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.github.telegram_bots.parser.actor.PostStorage.{SaveRequest, SaveResponse}
import com.github.telegram_bots.parser.domain.{Channel, Post}

// Заглушка
class PostStorage()(implicit timeout: Timeout) extends Actor with ActorLogging {
  override def receive: Receive = {
    case SaveRequest(url, posts) =>
      log.info(s"Post storage received $url ${posts.size}")
      sender ! SaveResponse(url)
  }
}

object PostStorage {
  def props()(implicit timeout: Timeout): Props = Props(new PostStorage())

  case class SaveRequest(channel: Channel, posts: Seq[Post])

  case class SaveResponse(channel: Channel)
}
