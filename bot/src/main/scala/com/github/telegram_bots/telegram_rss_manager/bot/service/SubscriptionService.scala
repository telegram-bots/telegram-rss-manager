package com.github.telegram_bots.telegram_rss_manager.bot.service

import com.github.telegram_bots.telegram_rss_manager.bot.exception.{AlreadySubscribedException, NotSubscribedException}
import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel
import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel._
import com.github.telegram_bots.telegram_rss_manager.core.domain.User.TelegramID
import com.github.telegram_bots.telegram_rss_manager.core.persistence.{ChannelRepository, SubscriptionRepository, UserRepository}
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

  def subscribe(telegramID: TelegramID, channelURL: ChannelURL, channelName: ChannelName): Future[Channel] = {
    val action = DBIO.from {
      val userFuture = userRepository.getOrCreate(telegramID)
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

  def unsubscribe(telegramID: TelegramID, channelURL: ChannelName): Future[Channel] = {
    val action = DBIO.from {
      val userFuture = userRepository.find(telegramID)
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

  def list(telegramID: TelegramID): Future[Seq[Channel]] = {
    val action = DBIO.from {
      for {
        Some(user) <- userRepository.find(telegramID)
        channels <- subsRepository.list(user.id)
      } yield channels
    }

    db.run { action }
      .recover {
        case _: NoSuchElementException => Seq.empty
      }
  }
}
