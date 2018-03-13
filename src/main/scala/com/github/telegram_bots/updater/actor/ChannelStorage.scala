package com.github.telegram_bots.updater.actor

import akka.actor.Actor
import com.github.telegram_bots.core.Implicits.ExtendedFuture
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.config.ConfigProperties
import com.github.telegram_bots.core.domain.Channel
import com.github.telegram_bots.updater.actor.ChannelStorage._
import com.github.telegram_bots.updater.persistence.ChannelRepository
import com.typesafe.config.Config
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class ChannelStorage(config: Config, repository: ChannelRepository) extends Actor with ReactiveActor {
  val props = new Properties(config)

  override def receive: Receive = {
    case GetRequest =>
      log.debug("Requested GetRequest")

      for (channel <- getAndLock().get) {
        log.debug(s"Respond with GetResponse($channel)")
        sender ! GetResponse(channel)
      }
    case UpdateRequest(channel) =>
      log.debug(s"Requested UpdateRequest for $channel")

      val updatedChannel = updateAndUnlock(channel).get
      log.debug(s"Respond with UpdateResponse($updatedChannel)")
      sender ! UpdateResponse(updatedChannel)
    case UnlockAllRequest =>
      log.debug("Requested UnlockAllRequest")

      unlockAll().get
      log.debug(s"Respond with UnlockAllResponse")
      sender ! UnlockAllResponse
  }

  private def getAndLock(): Future[Option[Channel]] = {
    val action = DBIO
      .from(
        for {
          channel <- repository.firstNonLocked()
          _ <- channel.map(repository.lock).getOrElse(Future.successful(Nil))
        } yield channel
      )
      .transactionally

    repository.run(action)
  }

  private def updateAndUnlock(channel: Channel): Future[Channel] = {
    val action = DBIO
      .from(
        for {
          _ <- repository.update(channel)
          _ <- repository.unlock(channel)
        } yield channel
      )
      .transactionally

    repository.run(action)
  }

  private def unlockAll(): Future[Int] = repository.unlockAll()
}

object ChannelStorage {
  case object GetRequest

  case class GetResponse(channel: Channel)

  case class UpdateRequest(channel: Channel)

  case class UpdateResponse(channel: Channel)

  case object UnlockAllRequest

  case object UnlockAllResponse

  class Properties(root: Config) extends ConfigProperties(root, "akka") {
    val systemName: String = self.getString("system-name")
  }
}