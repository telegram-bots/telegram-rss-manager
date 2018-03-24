package com.github.telegram_bots.telegram_rss_manager.bot.config

import com.github.telegram_bots.telegram_rss_manager.core.config.ConfigProperties
import com.typesafe.config.Config

case class Properties(config: Config) extends ConfigProperties(config, "bot") {
  val token: String = self.getString("token")

  val mode: String = self.getString("mode")

  val pollingInterval: Int = self.getInt("polling-interval")

  val webHookUrl: String = self.getString("webhook-url")

  val certificatePath: String = self.getString("certificate-path")

  val ip: String = self.getString("ip")

  val port: Int = self.getInt("port")

  val feedURL: String = self.getString("feed-url")
}
