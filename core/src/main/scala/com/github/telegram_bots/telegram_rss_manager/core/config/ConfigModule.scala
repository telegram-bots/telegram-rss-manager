package com.github.telegram_bots.telegram_rss_manager.core.config

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

trait ConfigModule {
  lazy val config: Config = ConfigFactory.parseFile(new File(sys.env.getOrElse("APP_CONF", ".")))
    .withFallback(ConfigFactory.load())
}
