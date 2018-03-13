package com.github.telegram_bots.updater.actor

import akka.actor.Actor
import akka.pattern.pipe
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.domain.{Channel, Post}
import com.github.telegram_bots.updater.actor.PostStorage.{SaveRequest, SaveResponse}
import com.github.telegram_bots.updater.persistence.PostRepository

class PostStorage(repository: PostRepository) extends Actor with ReactiveActor {
  override def receive: Receive = {
    case SaveRequest(url, posts) =>
      log.debug(s"Requested SaveRequest($url, ${posts.size})")

      val response = repository.saveAll(posts)
        .filter(_.contains(posts.size))
        .map(_ => SaveResponse(url))

      pipe(response) to sender
  }
}

object PostStorage {
  case class SaveRequest(channel: Channel, posts: Seq[Post])

  case class SaveResponse(channel: Channel)
}
