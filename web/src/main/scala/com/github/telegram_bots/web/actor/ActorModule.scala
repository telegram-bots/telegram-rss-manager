package com.github.telegram_bots.web.actor

import akka.actor.{ActorRef, ActorSystem}
import com.github.telegram_bots.core.config.ConfigModule
import com.github.telegram_bots.web.persistence.PersistenceModule
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.tagging._

trait ActorModule { this: PersistenceModule with ConfigModule =>
  lazy val system = ActorSystem(config.getString("akka.system-name"))

  val postStorage: ActorRef @@ PostStorage =
    wireActor[PostStorage](PostStorage.getClass.getName).taggedWith[PostStorage]

  val rssGenerator: ActorRef @@ RSSGenerator =
    wireActor[RSSGenerator](RSSGenerator.getClass.getName).taggedWith[RSSGenerator]
}
