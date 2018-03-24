package com.github.telegram_bots.bot

import com.github.telegram_bots.bot.service.ServiceModule
import com.github.telegram_bots.core.config.ConfigModule
import com.github.telegram_bots.core.persistence.PersistenceModule

object BotRunner extends App
  with ConfigModule
  with PersistenceModule
  with ServiceModule
{
  botService.run()
}
