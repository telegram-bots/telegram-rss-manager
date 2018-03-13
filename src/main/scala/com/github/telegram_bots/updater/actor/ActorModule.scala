package com.github.telegram_bots.updater.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.SmallestMailboxPool
import com.github.telegram_bots.core.config.ConfigModule
import com.github.telegram_bots.updater.persistence.PersistenceModule
import com.softwaremill.macwire._
import com.softwaremill.tagging._

trait ActorModule { this: PersistenceModule with ConfigModule =>
  lazy val system = ActorSystem(config.getConfig("akka").getString("system-name"))

  def createProxyProvider: ActorRef @@ ProxyProvider =
    system.actorOf(Props(wire[ProxyProvider]), ProxyProvider.getClass.getName).taggedWith[ProxyProvider]

  def createPostParser: ActorRef @@ PostParser = system
    .actorOf(
      Props(wire[PostParser])
        .withDispatcher("akka.actor.dispatcher.post-dispatcher")
        .withRouter(new SmallestMailboxPool(25)),
      PostParser.getClass.getName
    )
    .taggedWith[PostParser]

  def createChannelParser: ActorRef @@ ChannelParser = system
      .actorOf(
        Props(wire[ChannelParser]).withDispatcher("akka.actor.dispatcher.channel-dispatcher"),
        ChannelParser.getClass.getName
      )
      .taggedWith[ChannelParser]

  def createPostStorage: ActorRef @@ PostStorage =
    system.actorOf(Props(wire[PostStorage]), PostStorage.getClass.getName).taggedWith[PostStorage]

  def createChannelStorage: ActorRef @@ ChannelStorage =
    system.actorOf(Props(wire[ChannelStorage]), ChannelStorage.getClass.getName).taggedWith[ChannelStorage]

  def createMaster: ActorRef @@ Master =
    system.actorOf(Props(wire[Master]), Master.getClass.getName).taggedWith[Master]
}
