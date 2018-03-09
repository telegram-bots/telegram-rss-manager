package com.github.telegram_bots.parser.actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.github.telegram_bots.parser.actor.ChannelStorage._
import com.github.telegram_bots.parser.domain.Channel

import scala.collection.mutable

// Заглушка
class ChannelStorage()(implicit timeout: Timeout) extends Actor with ActorLogging {
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
  def props()(implicit timeout: Timeout): Props = Props(new ChannelStorage())

  case object GetRequest

  case class GetResponse(channel: Channel)

  case class UpdateRequest(channel: Channel)

  case class UpdateResponse(channel: Channel)
}