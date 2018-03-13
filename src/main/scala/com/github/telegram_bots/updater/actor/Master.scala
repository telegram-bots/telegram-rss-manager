package com.github.telegram_bots.updater.actor

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import com.github.telegram_bots.core.Implicits.ExtendedFuture
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.domain.Types._
import com.github.telegram_bots.updater.actor.ChannelStorage.UnlockAllRequest
import com.github.telegram_bots.updater.actor.Master.{Next, Start}
import com.github.telegram_bots.updater.actor.ProxyProvider.Get
import com.softwaremill.tagging.@@

import scala.language.postfixOps

class Master(
    proxyProvider: ActorRef @@ ProxyProvider,
    channelParser: ActorRef @@ ChannelParser,
    channelStorage: ActorRef @@ ChannelStorage,
    postStorage: ActorRef @@ PostStorage
) extends Actor with ReactiveActor {
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

  private def getProxy: Proxy = (proxyProvider ? Get).get.asInstanceOf[Proxy]
}

object Master {
  object Start

  object Next
}