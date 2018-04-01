package com.github.telegram_bots.telegram_rss_manager.updater.component

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.http.scaladsl.{ClientTransport, Http}
import akka.util.Timeout
import com.github.telegram_bots.telegram_rss_manager.core.Implicits.ExtendedHttpResponse
import com.github.telegram_bots.telegram_rss_manager.core.domain.Proxy

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

object HttpClient {
  private val defaultHeaders: Set[HttpHeader] =
    Set(
//      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36",
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
      "Accept-Encoding" -> "gzip, deflate",
      "Accept-Language" -> "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
      "Cache-Control" -> "max-age=0",
      "Connection" -> "keep-alive",
      "DNT" -> "1",
      "Upgrade-Insecure-Requests" -> "1"
    )
    .map { case (k, v) => RawHeader(k, v) }

  def execute(
     url: String,
     method: HttpMethod = GET,
     params: Map[String, String] = Map.empty,
     proxy: Proxy = null,
     timeout: Timeout = null,
     headers: Set[HttpHeader] = Set.empty,
   )(implicit system: ActorSystem, dispatcher: ExecutionContext): Future[HttpResponse] = {
    val settings = configure(proxy, timeout, system)
    val entity = if (method == POST && params.nonEmpty) FormData(params).toEntity else HttpEntity.Empty
    val query = if (method == GET && params.nonEmpty) Uri.Query(params) else Uri.Query.Empty
    val request = HttpRequest(uri = Uri(url).withQuery(query), method = method, entity = entity)
      .withHeaders(Seq((defaultHeaders ++ headers).toSeq: _*))

    Http().singleRequest(request, settings = settings).map(_.decode)
  }

  private def configure(proxy: Proxy = null, timeout: Timeout = null, system: ActorSystem): ConnectionPoolSettings = {
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
