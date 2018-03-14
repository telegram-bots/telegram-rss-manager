package com.github.telegram_bots.web.actor

import akka.actor.Actor
import akka.pattern.pipe
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.domain.Post
import com.github.telegram_bots.web.persistence.PostRepository
import com.github.telegram_bots.web.actor.PostStorage.{GetLatestRequest, GetLatestResponse}

class PostStorage(repository: PostRepository) extends Actor with ReactiveActor {
  override def receive: Receive = {
    case GetLatestRequest(userId, subscriptionName, limit) =>
      log.debug(s"Requested GetLatestRequest($userId, $subscriptionName, $limit)")

      val response = repository.getLatest(userId, subscriptionName, limit)
        .map(GetLatestResponse)

      pipe(response) to sender
  }
}

object PostStorage {
  case class GetLatestRequest(userId: Long, subscriptionName: String, limit: Int)

  case class GetLatestResponse(posts: Seq[Post])
}
