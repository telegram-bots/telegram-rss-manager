package com.github.telegram_bots.updater.actor

import akka.actor.{ActorRef, ActorSystem}
import akka.routing.SmallestMailboxPool
import com.github.telegram_bots.core.config.ConfigModule
import com.github.telegram_bots.updater.persistence.PersistenceModule
import com.softwaremill.macwire.akkasupport._
import com.softwaremill.tagging._

trait ActorModule { this: PersistenceModule with ConfigModule =>
  lazy val system: ActorSystem = ActorSystem(config.getString("akka.system-name"))

  val proxyProvider: ActorRef @@ ProxyProvider =
    wireActor[ProxyProvider](ProxyProvider.getClass.getSimpleName).taggedWith[ProxyProvider]

  val postParser: ActorRef @@ PostParser = system
    .actorOf(
      wireProps[PostParser]
        .withDispatcher("akka.actor.dispatcher.parser-dispatcher")
        .withRouter(new SmallestMailboxPool(25)),
      PostParser.getClass.getSimpleName
    )
    .taggedWith[PostParser]

  val channelParser: ActorRef @@ ChannelParser = system
    .actorOf(
      wireProps[ChannelParser]
        .withDispatcher("akka.actor.dispatcher.parser-dispatcher")
        .withRouter(new SmallestMailboxPool(5)),
      ChannelParser.getClass.getSimpleName
    )
    .taggedWith[ChannelParser]

  val postStorage: ActorRef @@ PostStorage = system
    .actorOf(
      wireProps[PostStorage]
        .withDispatcher("akka.actor.dispatcher.db-dispatcher")
        .withRouter(new SmallestMailboxPool(5)),
      PostStorage.getClass.getSimpleName
    )
    .taggedWith[PostStorage]

  val channelStorage: ActorRef @@ ChannelStorage = system
    .actorOf(
      wireProps[ChannelStorage]
        .withDispatcher("akka.actor.dispatcher.db-dispatcher")
        .withRouter(new SmallestMailboxPool(5)),
      ChannelStorage.getClass.getSimpleName
    )
    .taggedWith[ChannelStorage]

  val master: ActorRef @@ Master =
    wireActor[Master](Master.getClass.getSimpleName).taggedWith[Master]

  def createWorker: ActorRef @@ Worker =
    wireAnonymousActor[Worker].taggedWith[Worker]
}
