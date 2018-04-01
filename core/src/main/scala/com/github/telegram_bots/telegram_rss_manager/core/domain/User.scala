package com.github.telegram_bots.telegram_rss_manager.core.domain

import com.github.telegram_bots.telegram_rss_manager.core.domain.User.{TelegramID, UserID}

case class User(id: UserID, telegramId: TelegramID)

object User {
  type UserID = Int
  type TelegramID = Long
}