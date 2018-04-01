package com.github.telegram_bots.telegram_rss_manager.updater.actor

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import akka.NotUsed
import akka.actor.Actor
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.Source
import com.github.telegram_bots.telegram_rss_manager.core.Implicits._
import com.github.telegram_bots.telegram_rss_manager.core.actor.ReactiveActor
import com.github.telegram_bots.telegram_rss_manager.core.config.ConfigProperties
import com.github.telegram_bots.telegram_rss_manager.core.domain.Proxy
import com.github.telegram_bots.telegram_rss_manager.updater.actor.ProxyProvider._
import com.github.telegram_bots.telegram_rss_manager.updater.component.{HttpClient, ProxyDownloaders}
import com.typesafe.config.Config

import scala.concurrent.{Future, TimeoutException}

class ProxyProvider(config: Config) extends Actor with ReactiveActor {
  val props = new Properties(config)
  val proxies: BlockingQueue[Proxy] = new LinkedBlockingQueue()
  val downloader = ProxyDownloaders.MIXED_PROXY
  var running = false

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
      .mapAsyncUnordered(props.minSize)(proxy => Future.successful(proxy).zip(checkWorking(proxy)))
      .recover { case _: TimeoutException => (Proxy.EMPTY, false) }
      .filter(_._2)
      .map(_._1)
      .log("checked")

  private def checkWorking(proxy: Proxy): Future[Boolean] =
    HttpClient.execute("https://t.me/by_cotique/6", proxy = proxy, timeout = timeout)
      .recover { case _ => HttpResponse(status = StatusCodes.Forbidden) }
      .map(_.entity)
      .flatMap(Unmarshal(_).to[String])
      .map(_.contains("https://twitter.com/Hagnir/status/771707002632429569"))
}

object ProxyProvider {
  case object GetRequest

  case class GetResponse(proxy: Proxy)

  class Properties(root: Config) extends ConfigProperties(root, "akka.actor.config.proxy-provider") {
    val downloadSize: Int = self.getInt("download-size")
    val minSize: Int = self.getInt("min-size")
  }
}
