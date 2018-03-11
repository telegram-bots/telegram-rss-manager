package com.github.telegram_bots.updater.actor

import akka.actor.{Actor, Props}
import com.github.telegram_bots.core.ReactiveActor
import com.github.telegram_bots.core.domain.Channel
import com.github.telegram_bots.updater.actor.ChannelStorage._

import scala.collection.mutable

// Заглушка
class ChannelStorage extends Actor with ReactiveActor {
  val list = mutable.MutableList(
    (Channel("by_cotique", 1), false),
    (Channel("vlast_zh", 1), false),
    (Channel("clickordie", 1), false),
    (Channel("dvachannel", 1), false),
    (Channel("dev_rb", 1), false),
    (Channel("mudrosti", 1), false),
    (Channel("neuralmachine", 1), false)
  )

  override def receive: Receive = {
    case GetRequest =>
      log.info("Requested channel")

      val index = list.indexWhere(!_._2)
      val element = list(index)
      list(index) = element.copy(_2 = true)

      sender ! GetResponse(element._1)
    case UpdateRequest(channel) =>
      log.info(s"Request channel unlock and update: $channel")

      val index = list.indexWhere(_._1.url == channel.url)
      list(index) = (channel, false)

      sender ! UpdateResponse(channel)
  }
}

object ChannelStorage {
  def props: Props = Props[ChannelStorage]

  case object GetRequest

  case class GetResponse(channel: Channel)

  case class UpdateRequest(channel: Channel)

  case class UpdateResponse(channel: Channel)
}