package com.github.telegram_bots.updater.actor

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import akka.NotUsed
import akka.actor.Actor
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import com.github.telegram_bots.core.Implicits._
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.config.ConfigProperties
import com.github.telegram_bots.core.domain.Types._
import com.github.telegram_bots.updater.actor.ProxyProvider._
import com.github.telegram_bots.updater.component.{HttpClient, ProxyDownloader, ProxyDownloaders}
import com.typesafe.config.Config

import scala.concurrent.{Future, TimeoutException}

class ProxyProvider(config: Config) extends Actor with ReactiveActor {
  val props = new Properties(config)
  val downloader: ProxyDownloader = ProxyDownloaders.MIXED_PROXY
  val proxies: BlockingQueue[Proxy] = new LinkedBlockingQueue()
  var running: Boolean = false

  override def receive: Receive = {
    case GetRequest =>
      if (!running && proxies.size() <= props.minSize) {
        running = true
        download
          .runForeach(proxies.offer(_))
          .doOnComplete { _ => running = false }
      }

      val proxy = proxies.poll(timeout.duration._1, timeout.duration._2)
      if (proxy != null) sender ! GetResponse(proxy)
  }

  private def download: Source[Proxy, NotUsed] = downloader.download(props.downloadSize)
      .mapAsyncUnordered(props.downloadSize)(proxy => Future.successful(proxy).zip(checkWorking(proxy)))
      .recover { case _: TimeoutException => (Proxy.EMPTY, false) }
      .filter(_._2)
      .map(_._1)
      .log("checked")

  private def checkWorking(proxy: Proxy): Future[Boolean] = {
    HttpClient.execute("https://t.me/by_cotique/6", proxy = proxy, timeout = timeout)
      .recover { case _ => HttpResponse(status = StatusCodes.Forbidden) }
      .map(_.entity)
      .flatMap(Unmarshal(_).to[String])
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
