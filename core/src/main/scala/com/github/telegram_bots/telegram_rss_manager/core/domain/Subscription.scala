package com.github.telegram_bots.telegram_rss_manager.core.domain

import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel.ChannelID
import com.github.telegram_bots.telegram_rss_manager.core.domain.Subscription.SubscriptionName
import com.github.telegram_bots.telegram_rss_manager.core.domain.User.UserID

case class Subscription(userId: UserID, channelId: ChannelID, name: SubscriptionName)

object Subscription {
  type SubscriptionName = String
}