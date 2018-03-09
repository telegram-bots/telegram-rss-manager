package com.github.telegram_bots.updater.actor

import akka.actor.{Actor, Props}
import com.github.telegram_bots.core.ReactiveActor
import com.github.telegram_bots.core.domain.Channel
import com.github.telegram_bots.updater.actor.ChannelStorage._

import scala.collection.mutable

// Заглушка
class ChannelStorage extends Actor with ReactiveActor {
  val queue: mutable.Queue[Channel] = mutable.Queue(
    Channel("by_cotique", 1),
    Channel("vlast_zh", 1),
    Channel("clickordie", 1),
    Channel("dvachannel", 1),
    Channel("dev_rb", 1),
    Channel("mudrosti", 1),
    Channel("neuralmachine", 1)
  )

  override def receive: Receive = {
    case GetRequest =>
      log.info("Requested channel")
      sender ! GetResponse(queue.dequeue())
    case UpdateRequest(channel) =>
      log.info(s"Request channel unlock and update: $channel")
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