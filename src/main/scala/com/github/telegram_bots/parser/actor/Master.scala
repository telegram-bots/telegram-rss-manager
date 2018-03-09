package com.github.telegram_bots.parser.actor

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.github.telegram_bots.parser.actor.Master.{Complete, Next, Start}
import com.github.telegram_bots.parser.actor.ProxyRetriever.Get
import com.github.telegram_bots.parser.domain.Channel
import com.github.telegram_bots.parser.domain.Types._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

class Master extends Actor with ActorLogging {
  implicit val system: ActorSystem = context.system
  implicit val timeout: Timeout = Timeout(5 seconds)
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val proxyRetriever = system.actorOf(ProxyRetriever.props(25, 5))
  val channelParser = system.actorOf(ChannelParser.props(5, 50))
  val channelStorage = system.actorOf(ChannelStorage.props())
  val postStorage = system.actorOf(PostStorage.props())

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