package com.github.telegram_bots.telegram_rss_manager.bot.config

import com.github.telegram_bots.telegram_rss_manager.core.config.{ConfigModule => CoreConfigModule}
import com.softwaremill.macwire._

trait ConfigModule extends CoreConfigModule {
  lazy val props: Properties = wire[Properties]
}
