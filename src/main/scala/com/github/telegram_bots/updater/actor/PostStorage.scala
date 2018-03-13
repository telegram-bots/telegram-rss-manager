package com.github.telegram_bots.updater.actor

import akka.actor.Actor
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.domain.{Channel, Post}
import com.github.telegram_bots.updater.actor.PostStorage.{SaveRequest, SaveResponse}

// Заглушка
class PostStorage extends Actor with ReactiveActor {
  override def receive: Receive = {
    case SaveRequest(url, posts) =>
      log.info(s"Post storage received $url ${posts.size}")
      sender ! SaveResponse(url)
  }
}

object PostStorage {
  case class SaveRequest(channel: Channel, posts: Seq[Post])

  case class SaveResponse(channel: Channel)
}
