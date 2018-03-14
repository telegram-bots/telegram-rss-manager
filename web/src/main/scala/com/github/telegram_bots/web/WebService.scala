package com.github.telegram_bots.web

import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.util.Timeout
import com.github.telegram_bots.core.config.ConfigModule
import com.github.telegram_bots.web.actor.ActorModule
import com.github.telegram_bots.web.actor.PostStorage.{GetLatestRequest, GetLatestResponse}
import com.github.telegram_bots.web.actor.RSSGenerator.Generate
import com.github.telegram_bots.web.persistence.PersistenceModule

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object WebService extends App
  with ConfigModule
  with PersistenceModule
  with ActorModule
{
  implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)

  val userId = 100500
  val subscriptionName = "main"
  val posts = Await.result(postStorage ? GetLatestRequest(userId, subscriptionName, 500), timeout.duration)
    .asInstanceOf[GetLatestResponse]
    .posts
  val rss = Await.result(rssGenerator ? Generate(userId, subscriptionName, posts), timeout.duration)

  println(posts.size)
  println(rss)

  Await.result(system.whenTerminated, Duration.Inf)
}