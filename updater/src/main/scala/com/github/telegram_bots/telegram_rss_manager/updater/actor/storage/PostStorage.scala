package com.github.telegram_bots.telegram_rss_manager.updater.actor.storage

import akka.actor.Actor
import akka.pattern.pipe
import com.github.telegram_bots.telegram_rss_manager.core.Implicits.ExtendedFuture
import com.github.telegram_bots.telegram_rss_manager.core.actor.ReactiveActor
import com.github.telegram_bots.telegram_rss_manager.core.domain.{Channel, Post}
import com.github.telegram_bots.telegram_rss_manager.core.persistence.PostRepository
import com.github.telegram_bots.telegram_rss_manager.updater.actor.storage.PostStorage.{SaveRequest, SaveResponse}

class PostStorage(repository: PostRepository) extends Actor with ReactiveActor {
  override def receive: Receive = {
    case SaveRequest(url, posts) =>
      log.debug(s"SaveRequest($url, ${posts.size})")

      val response = repository.saveAll(posts)
        .filter(_.contains(posts.size))
        .map(_ => SaveResponse(url))
        .doOnNext(response => log.info(s"$response"))
        .doOnError(e => log.error(s"SaveRequest($url, ${posts.map(_.id)}) failed", e))

      pipe(response) to sender
  }
}

object PostStorage {
  case class SaveRequest(channel: Channel, posts: Seq[Post])

  case class SaveResponse(channel: Channel)
}
