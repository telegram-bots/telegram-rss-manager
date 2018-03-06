package com.github.telegram_bots.own

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.github.telegram_bots.own.actor.ChannelParser.Start
import com.github.telegram_bots.own.actor.ProxyRetriever.GetList
import com.github.telegram_bots.own.actor.{ChannelParser, ProxyRetriever}
import com.github.telegram_bots.own.domain.Types._

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {
  val system = ActorSystem("parser")
  val proxyRetriever = system.actorOf(ProxyRetriever.props)
  val channelParser = system.actorOf(ChannelParser.props)

  time {
    val proxies = mutable.Queue(getProxies: _*)

    val channels = List(
      "by_cotique",
      "vlast_zh",
      "clickordie"
    )

    for (channel <- channels) {
      channelParser ! Start(channel, proxy = proxies.dequeue())
    }

    Await.result(system.whenTerminated, 10 minutes)
  }

  def getProxies = {
    implicit val timeout: Timeout = Timeout(50 seconds)
    Await.result(proxyRetriever ? GetList, timeout.duration).asInstanceOf[Seq[Proxy]]
  }

  def time[R](block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
    result
  }
}