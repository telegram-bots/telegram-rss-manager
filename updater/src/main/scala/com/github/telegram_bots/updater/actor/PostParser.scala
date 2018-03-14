package com.github.telegram_bots.updater.actor

import akka.actor.Actor
import akka.pattern.pipe
import akka.stream.scaladsl.Sink
import com.github.telegram_bots.core.Implicits._
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.domain.Types._
import com.github.telegram_bots.core.domain._
import com.github.telegram_bots.updater.actor.PostParser.{ParseRequest, ParseResponse}
import com.github.telegram_bots.updater.component.{HttpClient, PostDataParser}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.Future
import scala.language.postfixOps

class PostParser extends Actor with ReactiveActor {
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
    HttpClient.get(s"https://t.me/${channel.url}/$postId?embed=1&single=1", proxy)
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
        date = parser.parseDate,
        author = parser.parseAuthor,
        channelLink = channel.url,
        channelName = parser.parseChannelName,
        fileURL = parser.parseFileURL
      )
    case _ => EmptyPost(id = postId, channelLink = channel.url)
  }
}

object PostParser {
  case class ParseRequest(channel: Channel, postId: PostID, proxy: Proxy)

  case class ParseResponse(channel: Channel, post: Post)
}
