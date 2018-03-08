package com.github.telegram_bots.own.actor

import java.net.InetSocketAddress
import java.time.ZoneId

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging
import akka.event.Logging.Debug
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{ClientTransport, Http}
import akka.pattern.{ask, pipe}
import akka.routing.SmallestMailboxPool
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Materializer}
import akka.util.Timeout
import com.github.telegram_bots.own.Implicits._
import com.github.telegram_bots.own.actor.PostParser.{Parse, dispatcher, props}
import com.github.telegram_bots.own.component.PostDataParser
import com.github.telegram_bots.own.domain.Types._
import com.github.telegram_bots.own.domain._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.duration._
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class PostParser extends Actor with ActorLogging {
  val headers: Seq[HttpHeader] = getHeaders

  implicit val system: ActorSystem = context.system
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatchers.lookup(dispatcher)
  implicit val timeout: Timeout = Timeout(5.seconds)

  def receive: Receive = {
    case Parse(url, postId, proxy) =>
      val post = Source.fromFuture(download(url, postId, proxy))
        .mapAsyncUnordered(1)(checkResponse)
        .map(parse(url, postId)(_))
        .log("parsed-post", post => s"$url [$postId] $post")(log)
        .recover { case e =>
          log.warning(s"Failed to parse ${e.getMessage}, retrying...")
          self ? Parse(url, postId, proxy)
        }
        .withAttributes(ActorAttributes.dispatcher(dispatcher))
        .runWith(Sink.head)

      pipe(post) to sender
  }

  private def download(url: ChannelURL, postId: PostID, proxy: Proxy): Future[Document] = {
    val transport = ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(proxy.host, proxy.port))
    val settings = ConnectionPoolSettings(context.system).withTransport(transport)
    val request = HttpRequest(uri = s"https://t.me/$url/$postId?embed=1&single=1").withHeaders(headers)

    Http().singleRequest(request, settings = settings)
      .map(_.decode)
      .flatMap(_.getBody)
      .map(Jsoup.parse)
  }

  private def checkResponse(doc: Document): Future[Option[Document]] = {
    val error = doc.select(".tgme_widget_message_error").text().trim()

    error match {
      case e if e == "Post not found" => Future.successful(Option.empty)
      case e if e.contains("Channel with username") => Future.failed(new RuntimeException(e))
      case _ => Future.successful(Option(doc))
    }
  }

  private def parse(url: ChannelURL, postId: PostID)(document: Option[Document]): Post = document match {
    case Some(message) =>
      val parser = new PostDataParser(message)

      PresentPost(
        id = postId,
        `type` = parser.parseType,
        content = parser.parseContent,
        date = parser.parseDate.atZone(ZoneId.systemDefault()).toEpochSecond,
        author = parser.parseAuthor,
        channelLink = url,
        channelName = parser.parseChannelName,
        fileURL = parser.parseFileURL
      )
    case _ => EmptyPost(id = postId, channelLink = url)
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
  def dispatcher = "postDispatcher"

  def props: Props = Props[PostParser].withDispatcher(dispatcher).withRouter(new SmallestMailboxPool(25))

  case class Parse(url: ChannelURL, postId: PostID, proxy: Proxy)
}
