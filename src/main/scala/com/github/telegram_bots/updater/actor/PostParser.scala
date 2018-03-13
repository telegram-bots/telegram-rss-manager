package com.github.telegram_bots.updater.actor

import java.net.InetSocketAddress
import java.time.ZoneId

import akka.actor.Actor
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{ClientTransport, Http}
import akka.pattern.pipe
import akka.stream.scaladsl.Sink
import com.github.telegram_bots.core.Implicits._
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.domain.Types._
import com.github.telegram_bots.core.domain._
import com.github.telegram_bots.updater.actor.PostParser.{ParseRequest, ParseResponse}
import com.github.telegram_bots.updater.component.PostDataParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.language.postfixOps

class PostParser extends Actor with ReactiveActor {
  val headers: Seq[HttpHeader] = getHeaders

  def receive: Receive = {
    case ParseRequest(channel, postId, proxy) =>
      val response = download(channel, postId, proxy)
        .flatMap(checkResponse)
        .map(parse(channel, postId))
        .doOnNext(post => log.debug(s"Parsed post: ${channel.url} [$postId] $post"))
        .map(ParseResponse(channel, _))
        .doOnError(e => log.warning(s"Failed to parse post: ${e.getMessage}"))

      pipe(response) to sender
  }

  private def download(channel: Channel, postId: PostID, proxy: Proxy): Future[Document] = {
    val settings = ConnectionPoolSettings(system)
      .withTransport(ClientTransport.httpsProxy(
        InetSocketAddress.createUnresolved(proxy.host, proxy.port)
      ))
    val request = HttpRequest(uri = s"https://t.me/${channel.url}/$postId?embed=1&single=1").withHeaders(headers)

    Http().singleRequest(request, settings = settings)
      .map(_.decode)
      .flatMap(_.getBody.runWith(Sink.head))
      .map(Jsoup.parse)
  }

  private def checkResponse(doc: Document): Future[Option[Document]] = {
    val error = doc.select(".tgme_widget_message_error").text.trim

    error match {
      case e if e == "Post not found" => Future.successful(Option.empty)
      case e if e contains "Channel with username" => Future.failed(new RuntimeException(e))
      case _ => Future.successful(Option(doc))
    }
  }

  private def parse(channel: Channel, postId: PostID)(document: Option[Document]): Post = document match {
    case Some(message) =>
      val parser = new PostDataParser(message)

      PresentPost(
        id = postId,
        `type` = parser.parseType,
        content = parser.parseContent,
        date = parser.parseDate.atZone(ZoneId.systemDefault).toEpochSecond,
        author = parser.parseAuthor,
        channelLink = channel.url,
        channelName = parser.parseChannelName,
        fileURL = parser.parseFileURL
      )
    case _ => EmptyPost(id = postId, channelLink = channel.url)
  }

  private def getHeaders: Seq[HttpHeader] = {
    Seq(
//      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36",
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
      "Accept-Encoding" -> "gzip, deflate, br",
      "Accept-Language" -> "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
      "Cache-Control" -> "max-age=0",
      "Connection" -> "keep-alive",
//      "Host" -> "t.me",
      "DNT" -> "1",
      "Upgrade-Insecure-Requests" -> "1"
    ).map { case (k, v) => RawHeader(k, v) }
  }
}

object PostParser {
  case class ParseRequest(channel: Channel, postId: PostID, proxy: Proxy)

  case class ParseResponse(channel: Channel, post: Post)
}
