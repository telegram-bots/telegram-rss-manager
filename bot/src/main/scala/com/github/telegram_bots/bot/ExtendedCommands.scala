package com.github.telegram_bots.bot

import com.github.telegram_bots.bot.exception.GenericSubscriptionException
import com.github.telegram_bots.core.domain.Types.ChannelURL
import info.mukel.telegrambot4s.api.Extractors.commandArguments
import info.mukel.telegrambot4s.api.declarative.{Action, Commands}
import info.mukel.telegrambot4s.models.ChatType.ChatType
import info.mukel.telegrambot4s.models.Message

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ExtendedCommands extends Commands {
  private val pattern = """(?:.*)(?:t.me\/|@)([^\/]*)(?:\/?)""".r

  def withChannelURL(action: Action[Option[ChannelURL]])(implicit msg: Message): Unit = {
    val channelURL = commandArguments(msg)
      .map(_.mkString)
      .filter(_.matches(pattern.regex))
      .flatMap(pattern.findFirstMatchIn(_))
      .map(_.group(1))

    action(channelURL)
  }

  def withChatTypeFilter(action: Action[ChatType])(implicit msg: Message): Unit = {
    action(msg.chat.`type`)
  }

  def handle[T](future: Future[T])(onSuccess: T => Unit)(implicit msg: Message): Unit = {
    future.onComplete {
      case Success(value) => onSuccess(value)
      case Failure(error) => error match {
        case e: GenericSubscriptionException =>
          reply(e.getMessage)
        case e =>
          logger.error("Unhandled exception", e)
          reply("Unknown error. Please try again later.")
      }
    }
  }
}