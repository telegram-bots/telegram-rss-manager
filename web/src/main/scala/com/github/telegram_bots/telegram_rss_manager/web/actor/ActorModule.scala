package com.github.telegram_bots.telegram_rss_manager.web.actor

import akka.actor.{ActorRef, ActorSystem}
import com.github.telegram_bots.telegram_rss_manager.core.config.ConfigModule
import com.github.telegram_bots.telegram_rss_manager.core.persistence.PersistenceModule
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.tagging._

trait ActorModule { this: PersistenceModule with ConfigModule =>
  implicit lazy val system: ActorSystem = ActorSystem(config.getString("akka.system-name"))

  def createPostStorage: ActorRef @@ PostStorage =
    wireAnonymousActor[PostStorage].taggedWith[PostStorage]

  def createRSSGenerator: ActorRef @@ RSSGenerator =
    wireAnonymousActor[RSSGenerator].taggedWith[RSSGenerator]

  def createFeedResponder: ActorRef @@ FeedResponder =
    wireAnonymousActor[FeedResponder].taggedWith[FeedResponder]
}
