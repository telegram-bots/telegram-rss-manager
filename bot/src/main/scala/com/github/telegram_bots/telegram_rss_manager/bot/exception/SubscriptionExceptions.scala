package com.github.telegram_bots.telegram_rss_manager.bot.exception

class GenericSubscriptionException(message: String) extends Exception(message, null, false, false)

case object AlreadySubscribedException
  extends GenericSubscriptionException("You have already subscribed to this channel!")

case object ChannelNotFoundException
  extends GenericSubscriptionException("Channel not found!")

case object NotSubscribedException
  extends GenericSubscriptionException("You are not subscribed to this channel!")
