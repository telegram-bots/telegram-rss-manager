package com.github.telegram_bots.telegram_rss_manager.web.actor

import akka.actor.{Actor, ActorRef, ReceiveTimeout}
import akka.http.scaladsl.model.HttpCharsets.`UTF-8`
import akka.http.scaladsl.model.MediaTypes.`application/rss+xml`
import akka.http.scaladsl.model.StatusCodes.{GatewayTimeout, OK}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse}
import com.github.telegram_bots.telegram_rss_manager.core.actor.ReactiveActor
import com.github.telegram_bots.telegram_rss_manager.core.domain.Subscription.SubscriptionName
import com.github.telegram_bots.telegram_rss_manager.core.domain.User.TelegramID
import com.github.telegram_bots.telegram_rss_manager.web.actor.FeedResponder._
import com.github.telegram_bots.telegram_rss_manager.web.component.ImperativeRequestContext
import com.softwaremill.tagging.@@

import scala.concurrent.duration._
import scala.language.postfixOps

class FeedResponder(
   storage: ActorRef @@ PostStorage,
   generator: ActorRef @@ RSSGenerator
) extends Actor with ReactiveActor {
  val contentType: ContentType.WithCharset = `application/rss+xml` withCharset `UTF-8`

  override def receive: Receive = waitingForRequest

  def waitingForRequest: Receive = {
    case GetRequest(requestContext, telegramID, subscriptionName, limit) =>
      storage ! PostStorage.GetLatestRequest(telegramID, subscriptionName, limit)

      context setReceiveTimeout (1 second)
      context become waitingForStorageResponse(requestContext, telegramID, subscriptionName)
  }

  def waitingForStorageResponse(
    requestContext: ImperativeRequestContext,
    telegramID: TelegramID,
    subscriptionName: SubscriptionName
  ): Receive = {
    case PostStorage.GetLatestResponse(posts) =>
      generator ! RSSGenerator.GenerateRequest(telegramID, subscriptionName, posts)

      context setReceiveTimeout (500 millis)
      context become waitingForGeneratorResponse(requestContext)
    case ReceiveTimeout =>
      respondWithTimeout(requestContext)
      context stop self
  }

  def waitingForGeneratorResponse(requestContext: ImperativeRequestContext): Receive = {
    case RSSGenerator.GenerateResponse(payload) =>
      requestContext complete HttpResponse(OK, entity = HttpEntity(contentType, payload))
      context stop self
    case ReceiveTimeout =>
      respondWithTimeout(requestContext)
      context stop self
  }

  private def respondWithTimeout(requestContext: ImperativeRequestContext): Unit = {
    requestContext complete HttpResponse(GatewayTimeout, entity = HttpEntity.empty(contentType))
  }
}

object FeedResponder {
  case class GetRequest(
     requestContext: ImperativeRequestContext,
     telegramID: TelegramID,
     subscriptionName: SubscriptionName,
     limit: Int
  )
}