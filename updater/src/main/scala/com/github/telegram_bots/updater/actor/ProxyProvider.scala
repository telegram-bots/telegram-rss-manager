package com.github.telegram_bots.updater.actor

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import akka.NotUsed
import akka.actor.Actor
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{Sink, Source}
import com.github.telegram_bots.core.Implicits._
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.config.ConfigProperties
import com.github.telegram_bots.core.domain.Types._
import com.github.telegram_bots.updater.actor.ProxyProvider._
import com.github.telegram_bots.updater.component.HttpClient
import com.typesafe.config.Config

import scala.concurrent.{Future, TimeoutException}

class ProxyProvider(config: Config) extends Actor with ReactiveActor {
  val props = new Properties(config)
  val proxies: BlockingQueue[Proxy] = new LinkedBlockingQueue()
  var running: Boolean = false

  override def receive: Receive = {
    case GetRequest =>
      if (!running && proxies.size() <= props.minSize) {
        running = true
        downloadProxies
          .runWith(Sink.foreach(proxies.offer(_)))
          .doOnComplete { _ => running = false }
      }

      val proxy = proxies.poll(timeout.duration._1, timeout.duration._2)
      if (proxy != null) sender ! GetResponse(proxy)
  }

  private def downloadProxies: Source[Proxy, Future[NotUsed]] = {
    val uri = s"http://pubproxy.com/api/proxy?format=txt&type=http&limit=${props.downloadSize}&level=anonymous&https=true&user_agent=true"

    Source.lazilyAsync { () => HttpClient.get(uri) }
      .flatMapConcat(parseResponse)
      .log("downloaded")
      .mapAsyncUnordered(props.downloadSize)(proxy => Future.successful(proxy).zip(checkWorking(proxy)))
      .recover { case _: TimeoutException => (Proxy.EMPTY, false) }
      .filter(_._2)
      .map(_._1)
      .log("checked")
  }

  private def parseResponse(response: HttpResponse): Source[Proxy, Any] = {
    response.getBody
      .mapConcat(_.split(System.lineSeparator()).toStream)
      .map(line => {
        val Array(host, port) = line.split(":")
        Proxy(host, port.toInt)
      })
  }

  private def checkWorking(proxy: Proxy): Future[Boolean] = {
    HttpClient.get("https://t.me/by_cotique/6", proxy, timeout)
      .recover { case _ => HttpResponse(status = StatusCodes.Forbidden) }
      .flatMap(_.getBody.runWith(Sink.head))
      .map(_.contains("https://twitter.com/Hagnir/status/771707002632429569"))
  }
}

object ProxyProvider {
  case object GetRequest

  case class GetResponse(proxy: Proxy)

  class Properties(root: Config) extends ConfigProperties(root, "akka.actor.config.proxy-provider") {
    val downloadSize: Int = self.getInt("download-size")
    val minSize: Int = self.getInt("min-size")
  }
}
