package com.github.telegram_bots.telegram_rss_manager.core.config

import com.typesafe.config.{Config, ConfigFactory}

trait ConfigModule {
  lazy val config: Config = ConfigFactory.load()
}
