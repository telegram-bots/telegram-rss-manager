package com.github.telegram_bots.telegram_rss_manager.bot.component

import com.github.telegram_bots.telegram_rss_manager.bot.config.Properties
import info.mukel.telegrambot4s.api.{Polling, Webhook}
import info.mukel.telegrambot4s.models.InputFile

trait Resolver extends Polling with Webhook {
  val props: Properties

  override def token: String = props.token

  override def pollingInterval: Int = props.pollingInterval

  override val webhookUrl: String = props.webHookUrl

  override def certificate: Option[InputFile] =
    if (props.certificatePath.isEmpty) None
    else Some(InputFile(props.certificatePath))

  override val interfaceIp: String = props.ip

  override val port: Int = props.port

  override def run(): Unit = {
    logger.info(s"Starting bot in ${props.mode} mode...")

    props.mode match {
      case "polling" => super[Polling].run()
      case "webhook" => super[Webhook].run()
      case _ => throw new IllegalArgumentException(s"Unknown mode: ${props.mode}")
    }
  }
}
