package com.github.telegram_bots.bot.service

import com.github.telegram_bots.bot.exception.{AlreadySubscribedException, NotSubscribedException}
import com.github.telegram_bots.core.domain.Channel
import com.github.telegram_bots.core.domain.Types.ChannelURL
import com.github.telegram_bots.core.persistence.{ChannelRepository, SubscriptionRepository, UserRepository}
import com.typesafe.scalalogging.LazyLogging
import org.postgresql.util.PSQLException
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionService(
  db: Database,
  subsRepository: SubscriptionRepository,
  userRepository: UserRepository,
  channelRepository: ChannelRepository
) extends LazyLogging {
  private implicit val executionContext: ExecutionContext = db.ioExecutionContext

  def subscribe(chatId: Long, channelURL: ChannelURL, channelName: String): Future[Channel] = {
    val action = DBIO.from {
      val userFuture = userRepository.getOrCreate(chatId)
      val channelFuture = channelRepository.getOrCreate(channelURL, channelName)

      for {
        user <- userFuture
        channel <- channelFuture
        _ <- subsRepository.subscribe(user.id, channel.id)
      } yield channel
    }

    db.run { action.transactionally }
      .recover {
        case e: PSQLException if e.getSQLState == "23505" => throw AlreadySubscribedException
      }
  }

  def unsubscribe(chatId: Long, channelURL: ChannelURL): Future[Channel] = {
    val action = DBIO.from {
      val userFuture = userRepository.find(chatId)
      val channelFuture = channelRepository.find(channelURL)

      for {
        Some(user) <- userFuture
        Some(channel) <- channelFuture
        result <- subsRepository.unsubscribe(user.id, channel.id)
        if result == 1
      } yield channel
    }

    db.run { action.transactionally }
        .recover {
          case _: NoSuchElementException => throw NotSubscribedException
        }
  }

  def list(chatId: Long): Future[Seq[Channel]] = {
    val action = DBIO.from {
      for {
        Some(user) <- userRepository.find(chatId)
        channels <- subsRepository.list(user.id)
      } yield channels
    }

    db.run { action }
      .recover {
        case _: NoSuchElementException => Seq.empty
      }
  }
}
