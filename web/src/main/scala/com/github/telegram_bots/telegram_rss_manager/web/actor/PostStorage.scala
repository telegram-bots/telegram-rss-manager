package com.github.telegram_bots.telegram_rss_manager.web.actor

import akka.actor.Actor
import akka.pattern.pipe
import com.github.telegram_bots.telegram_rss_manager.core.actor.ReactiveActor
import com.github.telegram_bots.telegram_rss_manager.core.domain.Post
import com.github.telegram_bots.telegram_rss_manager.core.domain.Subscription.SubscriptionName
import com.github.telegram_bots.telegram_rss_manager.core.domain.User.TelegramID
import com.github.telegram_bots.telegram_rss_manager.core.persistence.PostRepository
import com.github.telegram_bots.telegram_rss_manager.web.actor.PostStorage.{GetLatestRequest, GetLatestResponse}

class PostStorage(repository: PostRepository) extends Actor with ReactiveActor {
  override def receive: Receive = {
    case GetLatestRequest(telegramID, subscriptionName, limit) =>
      log.debug(s"Requested GetLatestRequest($telegramID, $subscriptionName, $limit)")

      val response = repository.getLatest(telegramID, subscriptionName, limit)
        .map(GetLatestResponse)

      pipe(response) to sender
  }
}

object PostStorage {
  case class GetLatestRequest(telegramID: TelegramID, subscriptionName: SubscriptionName, limit: Int)

  case class GetLatestResponse(posts: Seq[Post])
}
