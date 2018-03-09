package com.github.telegram_bots.parser

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.github.telegram_bots.parser.actor.ChannelParser.Start
import com.github.telegram_bots.parser.actor.ProxyRetriever.Get
import com.github.telegram_bots.parser.actor.{ChannelParser, ProxyRetriever}
import com.github.telegram_bots.parser.domain.Types._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {
  implicit val timeout: Timeout = Timeout(5 seconds)

  val system = ActorSystem("parser")
  val proxyRetriever = system.actorOf(ProxyRetriever.props(25, 5))
  val channelParser = system.actorOf(ChannelParser.props(5, 50))

  time {
    val channels = List(
      ("by_cotique", 1),
      ("vlast_zh", 1),
      ("clickordie", 1),
      ("dvachannel", 1),
      ("dev_rb", 1),
      ("mudrosti", 1),
      ("neuralmachine", 1)
    )

    for ((channel, postId) <- channels) {
      channelParser ! Start(channel, postId, proxy = getProxy)
    }

    Await.result(system.whenTerminated, 10 minutes)
  }

  private def getProxy: Proxy = Await.result(proxyRetriever ? Get, timeout.duration).asInstanceOf[Proxy]

  def time[R](block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
    result
  }
}