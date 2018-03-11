package com.github.telegram_bots.updater.actor

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import com.github.telegram_bots.core.ReactiveActor
import com.github.telegram_bots.core.domain.Channel
import com.github.telegram_bots.core.domain.types._
import com.github.telegram_bots.updater.actor.Master.{Complete, Next, Start}
import com.github.telegram_bots.updater.actor.ProxyProvider.Get

import scala.concurrent.Await
import scala.language.postfixOps

class Master extends Actor with ReactiveActor {
  val proxyRetriever: ActorRef = context.actorOf(ProxyProvider.props(25, 5), ProxyProvider.getClass.getSimpleName)
  val channelParser: ActorRef = context.actorOf(ChannelParser.props(5, 50), ChannelParser.getClass.getSimpleName)
  val channelStorage: ActorRef = context.actorOf(ChannelStorage.props, ChannelStorage.getClass.getSimpleName)
  val postStorage: ActorRef = context.actorOf(PostStorage.props, PostStorage.getClass.getSimpleName)

  override def receive: Receive = {
    case Start =>
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
      self ! Complete(channel)

    case Complete(channel) =>
      log.info(s"Channel $channel was successfully updated")
  }

  private def getProxy: Proxy = Await.result(proxyRetriever ? Get, timeout.duration).asInstanceOf[Proxy]
}

object Master {
  def props: Props = Props[Master]

  object Start

  object Next

  case class Complete(channel: Channel)
}