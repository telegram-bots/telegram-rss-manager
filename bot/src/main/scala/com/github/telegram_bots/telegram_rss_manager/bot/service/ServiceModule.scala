package com.github.telegram_bots.telegram_rss_manager.bot.service

import com.github.telegram_bots.telegram_rss_manager.bot.config.ConfigModule
import com.github.telegram_bots.telegram_rss_manager.core.persistence.PersistenceModule
import com.softwaremill.macwire._

trait ServiceModule { this: ConfigModule with PersistenceModule =>
  lazy val subscriptionService: SubscriptionService = wire[SubscriptionService]

  lazy val botService: BotService = wire[BotService]
}
