package com.github.telegram_bots.updater.component

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.http.scaladsl.{ClientTransport, Http}
import akka.util.Timeout
import com.github.telegram_bots.core.Implicits._
import com.github.telegram_bots.core.domain.Types._

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

object HttpClient {
  private val defaultHeaders: Set[HttpHeader] =
    Set(
      //      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36",
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
      "Accept-Encoding" -> "gzip, deflate, br",
      "Accept-Language" -> "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
      "Cache-Control" -> "max-age=0",
      "Connection" -> "keep-alive",
      "DNT" -> "1",
      "Upgrade-Insecure-Requests" -> "1"
    ).map { case (k, v) => RawHeader(k, v) }

  def get(url: String, proxy: Proxy = null, timeout: Timeout = null, headers: Set[HttpHeader] = Set())
         (implicit system: ActorSystem, executionContext: ExecutionContext): Future[HttpResponse] = {
    val settings = prepare(proxy, timeout)(system)
    val request = HttpRequest(uri = url).withHeaders(Seq((defaultHeaders ++ headers).toSeq: _*))

    Http().singleRequest(request, settings = settings)
      .map(_.decode)
  }

  private def prepare(proxy: Proxy = null, timeout: Timeout = null)(system: ActorSystem): ConnectionPoolSettings = {
    var settings = ConnectionPoolSettings(system)

    if (proxy != null) {
      settings = settings.withTransport(ClientTransport.httpsProxy(
        InetSocketAddress.createUnresolved(proxy.host, proxy.port)
      ))
    }

    if (timeout != null) {
      settings = settings.withConnectionSettings(ClientConnectionSettings(system)
        .withIdleTimeout(timeout.duration)
      )
    }

    settings
  }
}
