package com.github.telegram_bots.telegram_rss_manager.updater.actor

import java.util.concurrent.TimeoutException

import akka.actor.{Actor, ActorRef, ReceiveTimeout}
import com.github.telegram_bots.telegram_rss_manager.core.actor.ReactiveActor
import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel
import com.github.telegram_bots.telegram_rss_manager.updater.actor.ProxyProvider.GetRequest
import com.github.telegram_bots.telegram_rss_manager.updater.actor.Worker.{Register, RequestWork, Work}
import com.github.telegram_bots.telegram_rss_manager.updater.actor.parser.ChannelParser
import com.github.telegram_bots.telegram_rss_manager.updater.actor.storage.{ChannelStorage, PostStorage}
import com.softwaremill.tagging.@@

import scala.concurrent.duration._
import scala.language.postfixOps

class Worker(
    master: ActorRef @@ Master,
    proxyProvider: ActorRef @@ ProxyProvider,
    channelParser: ActorRef @@ ChannelParser,
    channelStorage: ActorRef @@ ChannelStorage,
    postStorage: ActorRef @@ PostStorage
) extends Actor with ReactiveActor {
  override def preStart(): Unit = {
    master ! Register(self)
    master ! RequestWork
  }

  override def receive: Receive = waitingForWork

  def waitingForWork: Receive = {
    case Master.WorkAvailable =>
      requestWork()
    case Work =>
      channelStorage ! ChannelStorage.GetRequest
      context become waitingForGetChannelResponse
      context setReceiveTimeout (15 seconds)
  }

  def waitingForGetChannelResponse: Receive = {
    case ChannelStorage.GetResponse(channel) => channel match {
      case Some(it) =>
        proxyProvider ! GetRequest
        context become waitingForGetProxyResponse(it)
      case _ => requestWork()
    }
    case ReceiveTimeout =>
      handleError(new TimeoutException("Channel get timeout"))
      requestWork()
  }

  def waitingForGetProxyResponse(channel: Channel): Receive = {
    case ProxyProvider.GetResponse(proxy) =>
      channelParser ! ChannelParser.Start(channel, proxy)
      context become waitingForParserResponse(channel)
    case ReceiveTimeout =>
      handleError(new TimeoutException(s"Proxy get timeout $channel"))
      recoverChannel(channel)
      requestWork()
  }

  def waitingForParserResponse(oldChannel: Channel): Receive = {
    case ChannelParser.Complete(channel, lastPostId, posts) =>
      postStorage ! PostStorage.SaveRequest(channel.copy(lastPostId = lastPostId), posts)
      context become waitingForPostStorageResponse(channel)
    case ReceiveTimeout =>
      handleError(new TimeoutException(s"Parser timeout $oldChannel"))
      recoverChannel(oldChannel)
      requestWork()
  }

  def waitingForPostStorageResponse(oldChannel: Channel): Receive = {
    case PostStorage.SaveResponse(channel) =>
      channelStorage ! ChannelStorage.UpdateRequest(channel)
      context become waitingForChannelStorageResponse(channel)
    case ReceiveTimeout =>
      handleError(new TimeoutException(s"PostStorage timeout $oldChannel"))
      recoverChannel(oldChannel)
      requestWork()
  }

  def waitingForChannelStorageResponse(oldChannel: Channel): Receive = {
    case ChannelStorage.UpdateResponse(channel) =>
      log.info(s"Channel $channel was successfully updated")
      requestWork()
    case ReceiveTimeout =>
      handleError(new TimeoutException(s"ChannelStorage timeout $oldChannel"))
      recoverChannel(oldChannel)
      requestWork()
  }

  private def recoverChannel(oldChannel: Channel): Unit = {
    channelStorage ! ChannelStorage.UpdateRequest(oldChannel)
  }

  private def handleError(exception: Exception): Unit = {
    log.warning(s"Worker ${self.path} error: ${exception.getMessage}")
  }

  private def requestWork(): Unit = {
    context become waitingForWork
    context setReceiveTimeout Duration.Inf
    master ! RequestWork
  }
}

object Worker {
  object Work

  case class Register(worker: ActorRef)
  case object RequestWork
}