package com.github.telegram_bots.telegram_rss_manager.updater.component

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.after
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.github.telegram_bots.telegram_rss_manager.core.domain.Proxy
import org.json4s._
import org.json4s.native.JsonMethods._
import org.jsoup.Jsoup

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

sealed trait ProxyDownloader {
  def download(size: Int)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, NotUsed]
}

object ProxyDownloaders {
  val PUB_PROXY: ProxyDownloader = PubProxyCom
  val GET_PROXY_LIST: ProxyDownloader = GetProxyListCom
  val SPYS_ONE: ProxyDownloader = SpysOne
  val FREE_PROXY_LIST: ProxyDownloader = FreeProxyList
  def MIXED_PROXY: ProxyDownloader = new MixedProxy
}

private[component] object PubProxyCom extends ProxyDownloader {
  private val sizePerRequest = 20
  private val uri = "http://pubproxy.com/api/proxy"
  private val queryParams = Map(
    "format" -> "txt",
    "type" -> "http",
    "level" -> "elite",
    "https" -> "true",
    "user_agent" -> "true",
    "last_check" -> "5",
    "limit" -> s"$sizePerRequest"
  )

  override def download(size: Int)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, NotUsed] = Source(1 to size by sizePerRequest)
    .mapAsync(1) { _ => after(1 second, system.scheduler)(HttpClient.execute(uri, params = queryParams)) }
    .flatMapConcat(parseResponse)
    .log("downloaded")
    .mapMaterializedValue(_ => NotUsed)

  private def parseResponse(response: HttpResponse)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, Any] = Source
    .lazilyAsync { () => Unmarshal(response.entity).to[String] }
    .map(validateResponse)
    .mapConcat(_.split(System.lineSeparator()).toStream)
    .map(line => {
      val Array(host, port) = line.split(":")
      Proxy(host, port.toInt)
    })

  private def validateResponse(response: String): String = response match {
    case r if r.contains("You reached the maximum") => throw LimitReachedException(r)
    case _ => response
  }
}

private[component] object GetProxyListCom extends ProxyDownloader {
  private implicit val formats: DefaultFormats = DefaultFormats
  private val uri = "https://api.getproxylist.com/proxy"
  private val queryParams = Map(
    "protocol" -> "http",
    "anonymity" -> "high anonymity",
    "allowsHttps" -> "1",
    "lastTested" -> "300",
    "maxConnectTime" -> "1",
    "allowsUserAgentHeader" -> "1"
  )

  override def download(size: Int)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, NotUsed] = Source(1 to size)
    .mapAsync(10) { _ => HttpClient.execute(uri, params = queryParams) }
    .flatMapConcat(parseResponse)
    .log("downloaded")

  private def parseResponse(response: HttpResponse)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, Any] = Source
    .lazilyAsync { () => Unmarshal(response.entity).to[String] }
    .map(StringInput)
    .map(parse(_, useBigDecimalForDouble = false, useBigIntForLong = false))
    .map(validateResponse)
    .map { json =>
      val ip = (json \\ "ip").extract[String]
      val port = (json \\ "port").extract[Int]
      Proxy(ip, port)
    }

  private def validateResponse(response: JValue): JValue = response \\ "error" match {
    case JString(e) => throw LimitReachedException(e)
    case _ => response
  }
}

private[component] object FreeProxyList extends ProxyDownloader {
  private val sizePerRequest = 300
  private val uri = "https://free-proxy-list.net/"

  override def download(size: Int)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, NotUsed] = Source(1 to size by sizePerRequest)
    .mapAsync(1) { _ => HttpClient.execute(uri) }
    .flatMapConcat(parseResponse)
    .log("downloaded")
    .take(size)
    .mapMaterializedValue(_ => NotUsed)

  private def parseResponse(response: HttpResponse)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, Any] = Source
    .lazilyAsync { () => Unmarshal(response.entity).to[String] }
      .map(Jsoup.parse)
      .map(_.select("#proxylisttable tbody tr").asScala.toList)
      .flatMapConcat(Source(_))
      .map(tr => {
        val host = tr.child(0).text
        val port = tr.child(1).text

        Proxy(host, port.toInt)
      })
}

private[component] object SpysOne extends ProxyDownloader {
  private val sizePerRequest = 500
  private val uri = "http://spys.one/en/anonymous-proxy-list/"
  private val formData = Map(
    "xpp" -> "5",
    "xf1" -> "1",
    "xf2" -> "1",
    "xf4" -> "0",
    "xf5" -> "1"
  )
  private val portPattern = """\(([\w\d]+?)\^([\w\d]+?)\)""".r

  override def download(size: Int)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, NotUsed] = Source(1 to size by sizePerRequest)
    .mapAsync(1) { _ => HttpClient.execute(uri, method = POST, params = formData) }
    .flatMapConcat(parseResponse)
    .log("downloaded")
    .take(size)
    .mapMaterializedValue(_ => NotUsed)

  private def parseResponse(response: HttpResponse)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, Any] = {
    def extractVars(script: String) = {
      val map = script.split(";").map(_.split("=")).map { case Array(k, v) => k -> v }.toMap

      map.mapValues {
        case value if value contains "^" =>
          val Array(left, right) = value.split("\\^")
          left.toInt ^ map(right).toInt
        case v => v.toInt
      }
    }

    def extractPort(script: String, vars: Map[String, Int]) = {
      portPattern.findAllMatchIn(script)
        .map(m => m.group(1) -> m.group(2))
        .map { case (left, right) => vars(left) ^ vars(right) }
        .map(_.toString)
        .reduce(_ + _)
        .toInt
    }

    Source.lazilyAsync { () => Unmarshal(response.entity).to[String] }
      .map(Jsoup.parse)
      .map(html => (
        extractVars(html.select("script").get(3).html()),
        html.select("tr[class^=spy1x]").asScala.toList
      ))
      .flatMapConcat { case (vars, list) => Source(list.map((vars, _))) }
      .drop(1)
      .map { case (vars, tr) =>
        val host = tr.child(0).child(1).text
        val port = extractPort(tr.select("script").html, vars)

        Proxy(host, port)
      }
  }
}

private[component] class MixedProxy extends ProxyDownloader {
  private val downloaders: List[ProxyDownloader] = List(
    PubProxyCom,
    GetProxyListCom,
    FreeProxyList,
    SpysOne
  )
  private var currentPos: Int = 0

  override def download(size: Int)(
    implicit system: ActorSystem,
    dispatcher: ExecutionContext,
    materializer: Materializer
  ): Source[Proxy, NotUsed] = current.download(size)
    .recoverWithRetries(-1, { case _: LimitReachedException => next.download(size) })

  private def current: ProxyDownloader = this.synchronized { downloaders(currentPos) }

  private def next: ProxyDownloader = this.synchronized {
    currentPos = if (currentPos + 1 == downloaders.size) 0 else currentPos + 1
    downloaders(currentPos)
  }
}

case class LimitReachedException(message: String) extends Exception(message, null, false, false)
