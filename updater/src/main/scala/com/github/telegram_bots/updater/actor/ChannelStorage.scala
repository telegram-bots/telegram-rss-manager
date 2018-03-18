package com.github.telegram_bots.updater.actor

import akka.actor.Actor
import akka.pattern.pipe
import com.github.telegram_bots.core.Implicits.ExtendedFuture
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.config.ConfigProperties
import com.github.telegram_bots.core.domain.Channel
import com.github.telegram_bots.updater.actor.ChannelStorage._
import com.github.telegram_bots.updater.persistence.ChannelRepository
import com.typesafe.config.Config

class ChannelStorage(config: Config, repository: ChannelRepository) extends Actor with ReactiveActor {
  val props = new Properties(config)

  override def receive: Receive = {
    case GetRequest =>
      log.debug("GetRequest")

      val response = repository.getAndLock(props.systemName)
        .map(GetResponse)
        .doOnNext(logResponse)
        .doOnError(e => log.error("GetRequest failed", e))

      pipe(response) to sender
    case UpdateRequest(channel) =>
      log.debug(s"UpdateRequest($channel)")

      val response = repository.updateAndUnlock(channel, props.systemName)
        .map(_ => UpdateResponse(channel))
        .doOnNext(logResponse)
        .doOnError(e => log.error(s"UpdateRequest($channel) failed", e))

      pipe(response) to sender
    case UnlockAllRequest =>
      log.debug("UnlockAllRequest")

      val response = repository.unlockAll(props.systemName)
        .map(_ => UnlockAllResponse)
        .doOnNext(logResponse)
        .doOnError(e => log.error("UnlockAllRequest failed", e))

      pipe(response) to sender
  }

  private def logResponse(response: Any): Unit = log.info(s"$response")
}

object ChannelStorage {
  case object GetRequest

  case class GetResponse(channel: Option[Channel])

  case class UpdateRequest(channel: Channel)

  case class UpdateResponse(channel: Channel)

  case object UnlockAllRequest

  case object UnlockAllResponse

  class Properties(root: Config) extends ConfigProperties(root, "akka") {
    val systemName: String = self.getString("system-name")
  }
}