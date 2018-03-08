package com.github.telegram_bots.own.actor

import java.net.InetSocketAddress

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.http.scaladsl.{ClientTransport, Http}
import akka.pattern.pipe
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import com.github.telegram_bots.own.actor.ProxyRetriever._
import com.github.telegram_bots.own.domain.Types._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.util.{Failure, Success}

class ProxyRetriever extends Actor with ActorLogging {
  implicit val system: ActorSystem = context.system
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  override def receive: Receive = {
    case GetList => getProxies pipeTo sender
  }

  private def getProxies: Future[Seq[Proxy]] = {
    val request = Http().singleRequest(HttpRequest(uri = PARSE_URL))

    val future = Source.fromFuture(request)
      .flatMapConcat(transform)
      .log("downloaded", { proxy => log.debug(s"Downloaded: $proxy") })
      .mapAsyncUnordered(DOWNLOAD_SIZE)(isWorking)
      .recover { case _: TimeoutException => (Proxy.EMPTY, false) }
      .filter(_._2)
      .map(_._1)
      .log("checked", { proxy => log.debug(s"Left after check: $proxy") })
      .runWith(Sink.collection)

    future.onComplete {
      case Success(proxies) =>
        log.info(s"Retrieved ${proxies.size} proxies")
        log.debug(s"Retrieved proxies: $proxies")
      case Failure(e) => log.error("Failed to retrieve proxies", e)
    }

    future
  }

  private def transform(response: HttpResponse): Source[Proxy, NotUsed] =
    Source.fromFuture(getBody(response))
      .mapConcat(_.split(System.lineSeparator()).toList)
      .map(line => {
        val Array(host, port) = line.split(":")
        Proxy(host, port.toInt)
      })

  private def isWorking(proxy: Proxy): Future[(Proxy, Boolean)] = {
    val transport = ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(proxy.host, proxy.port))
    val settings = ConnectionPoolSettings(system)
      .withTransport(transport)
      .withConnectionSettings(ClientConnectionSettings(system).withIdleTimeout(2.seconds))
    val request = Http().singleRequest(HttpRequest(uri = CHECK_URL), settings = settings)
    val response = request
      .recover { case _ => HttpResponse(status = StatusCodes.Forbidden) }
      .flatMap(response => Future(response.status).zip(getBody(response)))
      .map { case (statusCode, body) => statusCode.isSuccess() && CHECK_CONDITION(body) }

    Future(proxy).zip(response)
  }

  private def getBody(response: HttpResponse) = response.entity.dataBytes.runWith(Sink.head).map(_.utf8String)
}

object ProxyRetriever {
  def props: Props = Props[ProxyRetriever]

  val DOWNLOAD_SIZE = 25

  val PARSE_URL = s"http://pubproxy.com/api/proxy?format=txt&type=http&limit=$DOWNLOAD_SIZE&level=anonymous&https=true&user_agent=true"

  val CHECK_URL = "https://t.me/by_cotique/6"

  val CHECK_CONDITION = (body: String) => body.contains("https://twitter.com/Hagnir/status/771707002632429569")

  object GetList
}
