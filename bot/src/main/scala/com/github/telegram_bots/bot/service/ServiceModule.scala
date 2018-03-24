package com.github.telegram_bots.bot.service

import com.github.telegram_bots.core.config.ConfigModule
import com.github.telegram_bots.core.persistence.PersistenceModule
import com.softwaremill.macwire._

trait ServiceModule { this: ConfigModule with PersistenceModule =>
  lazy val subscriptionService: SubscriptionService = wire[SubscriptionService]

  lazy val botService: BotService = wire[BotService]
}
