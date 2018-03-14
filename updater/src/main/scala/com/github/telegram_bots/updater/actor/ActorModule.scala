package com.github.telegram_bots.updater.actor

import akka.actor.{ActorRef, ActorSystem}
import akka.routing.SmallestMailboxPool
import com.github.telegram_bots.core.config.ConfigModule
import com.github.telegram_bots.updater.persistence.PersistenceModule
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.tagging._

trait ActorModule { this: PersistenceModule with ConfigModule =>
  implicit lazy val system: ActorSystem = ActorSystem(config.getString("akka.system-name"))

  val proxyProvider: ActorRef @@ ProxyProvider =
    wireActor[ProxyProvider](ProxyProvider.getClass.getName).taggedWith[ProxyProvider]

  val postParser: ActorRef @@ PostParser = system
    .actorOf(
      wireProps[PostParser]
        .withDispatcher("akka.actor.dispatcher.post-dispatcher")
        .withRouter(new SmallestMailboxPool(25)),
      PostParser.getClass.getName
    )
    .taggedWith[PostParser]

  val channelParser: ActorRef @@ ChannelParser = system
      .actorOf(
        wireProps[ChannelParser].withDispatcher("akka.actor.dispatcher.channel-dispatcher"),
        ChannelParser.getClass.getName
      )
      .taggedWith[ChannelParser]

  val postStorage: ActorRef @@ PostStorage =
    wireActor[PostStorage](PostStorage.getClass.getName).taggedWith[PostStorage]

  val channelStorage: ActorRef @@ ChannelStorage =
    wireActor[ChannelStorage](ChannelStorage.getClass.getName).taggedWith[ChannelStorage]

  val master: ActorRef @@ Master =
    wireActor[Master](Master.getClass.getName).taggedWith[Master]
}
