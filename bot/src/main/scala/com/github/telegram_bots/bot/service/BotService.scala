package com.github.telegram_bots.bot.service

import com.github.telegram_bots.bot.ExtendedCommands
import com.github.telegram_bots.bot.exception.ChannelNotFoundException
import com.github.telegram_bots.bot.service.BotService.Properties
import com.github.telegram_bots.core.config.ConfigProperties
import com.typesafe.config.Config
import info.mukel.telegrambot4s.api.{Polling, TelegramApiException, TelegramBot}
import info.mukel.telegrambot4s.methods.GetChat
import info.mukel.telegrambot4s.models.{ChatId, ChatType}

import scala.io.Source

class BotService(config: Config, subs: SubscriptionService) extends TelegramBot with Polling with ExtendedCommands {
  private val props = new Properties(config)
  lazy val token: String = config.getString("bot.token")

  onCommand("start") { implicit msg => }

  onCommand("help") { implicit msg =>
    val help = Source.fromResource("help.md").getLines.mkString(System.lineSeparator)

    replyMd(help)
  }

  onCommand("subscribe") { implicit msg =>
    withChatTypeFilter {
      case ChatType.Private =>
        withChannelURL {
          case Some(channelURL) =>
            val action = client(GetChat(ChatId.fromChannel(s"@$channelURL")))
              .recover {
                case e: TelegramApiException if e.errorCode == 400 => throw ChannelNotFoundException
              }
              .flatMap(channel => subs.subscribe(msg.chat.id, channel.username.get, channel.title.getOrElse("")))

            handle(action) { channel =>
              reply(
                s"""Successfully subscribed to ${channel.name} (@${channel.url}).
                   |Your feed url is ${props.feedURL}/${msg.chat.id}/main""".stripMargin
              )
            }
          case _ =>
            reply("Illegal channel url! Type /help")
        }
      case _ =>
        reply("Only private chat supported!")
    }
  }

  onCommand("unsubscribe") { implicit msg =>
    withChatTypeFilter {
      case ChatType.Private =>
        withChannelURL {
          case Some(channelURL) =>
            handle(subs.unsubscribe(msg.chat.id, channelURL)) { channel =>
              reply(s"Successfully unsubscribed from ${channel.name} (@${channel.url})")
            }
          case _ =>
            reply("Illegal channel url! Type /help")
        }
      case _ =>
        reply("Only private chat supported!")
    }
  }

  onCommand("list") { implicit msg =>
    withChatTypeFilter {
      case ChatType.Private =>
        handle(subs.list(msg.chat.id)) { channels =>
          val message = channels match {
            case Nil => "You don't have active subscriptions."
            case xs => xs.zipWithIndex
              .map { case (channel, i) => s"${i + 1}. *${channel.name}* ([@${channel.url}](https://t.me/${channel.url}))" }
              .mkString(System.lineSeparator)
          }

          replyMd(message, disableWebPagePreview = Some(true))
        }
      case _ =>
        reply("Only private chat supported!")
    }
  }
}

object BotService {
  class Properties(config: Config) extends ConfigProperties(config, "bot") {
    val token: String = self.getString("token")

    val feedURL: String = self.getString("feed-url")
  }
}