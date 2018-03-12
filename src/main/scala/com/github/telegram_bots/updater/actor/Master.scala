package com.github.telegram_bots.updater.actor

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import com.github.telegram_bots.core.Implicits.ExtendedFuture
import com.github.telegram_bots.core.ReactiveActor
import com.github.telegram_bots.core.domain.Types._
import com.github.telegram_bots.updater.actor.ChannelStorage.UnlockAllRequest
import com.github.telegram_bots.updater.actor.Master.{Next, Start}
import com.github.telegram_bots.updater.actor.ProxyProvider.Get
import com.github.telegram_bots.updater.repository.ChannelRepository
import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api._

import scala.language.postfixOps

class Master extends Actor with ReactiveActor {
  val repository = new ChannelRepository(Database.forConfig("db", ConfigFactory.load))

  val proxyRetriever: ActorRef = context.actorOf(ProxyProvider.props(25, 5), ProxyProvider.getClass.getSimpleName)
  val channelParser: ActorRef = context.actorOf(ChannelParser.props(5), ChannelParser.getClass.getSimpleName)
  val channelStorage: ActorRef = context.actorOf(ChannelStorage.props(repository), ChannelStorage.getClass.getSimpleName)
  val postStorage: ActorRef = context.actorOf(PostStorage.props, PostStorage.getClass.getSimpleName)

  override def receive: Receive = {
    case Start =>
      channelStorage ! UnlockAllRequest

    case ChannelStorage.UnlockAllResponse =>
      for (_ <- 1 to 5) self ! Next

    case Next =>
      channelStorage ! ChannelStorage.GetRequest

    case ChannelStorage.GetResponse(channel) =>
      channelParser ! ChannelParser.Start(channel, getProxy)

    case ChannelParser.Complete(channel, lastPostId, posts) =>
      val newLastPostId = lastPostId.getOrElse(channel.lastPostId)
      postStorage ! PostStorage.SaveRequest(channel.copy(lastPostId = newLastPostId), posts)

    case PostStorage.SaveResponse(channel) =>
      channelStorage ! ChannelStorage.UpdateRequest(channel)

    case ChannelStorage.UpdateResponse(channel) =>
      log.info(s"Channel $channel was successfully updated")
  }

  private def getProxy: Proxy = (proxyRetriever ? Get).get.asInstanceOf[Proxy]
}

object Master {
  def props: Props = Props[Master]

  object Start

  object Next
}