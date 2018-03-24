package com.github.telegram_bots.telegram_rss_manager.bot

import com.github.telegram_bots.telegram_rss_manager.bot.config.ConfigModule
import com.github.telegram_bots.telegram_rss_manager.bot.service.ServiceModule
import com.github.telegram_bots.telegram_rss_manager.core.persistence.PersistenceModule

object BotRunner extends App
  with ConfigModule
  with PersistenceModule
  with ServiceModule
{
  botService.run()
}
