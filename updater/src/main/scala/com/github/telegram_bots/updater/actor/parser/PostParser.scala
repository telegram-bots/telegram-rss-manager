package com.github.telegram_bots.updater.actor.parser

import akka.actor.Actor
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import com.github.telegram_bots.core.Implicits._
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.domain.Types._
import com.github.telegram_bots.core.domain._
import com.github.telegram_bots.updater.actor.parser.ChannelParser.{Failure, Next}
import com.github.telegram_bots.updater.actor.parser.PostParser.{Parse, ParsingException}
import com.github.telegram_bots.updater.component.{HttpClient, PostDataParser}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class PostParser extends Actor with ReactiveActor {
  private val queryParams = Map(
    "embed" -> "1",
    "single" -> "1"
  )

  def receive: Receive = {
    case Parse(channel, postId, proxy) =>
      val response = download(channel, postId, proxy)
        .flatMap(checkResponse)
        .map(parse(channel, postId))
        .doOnNext(post => log.debug(s"Parsed post: ${channel.url} [$postId] ${post.getClass.getSimpleName}"))
        .map(Next(channel, _))
        .recover { case e: ParsingException => Failure(e) }

      pipe(response) to sender
  }

  private def download(channel: Channel, postId: PostID, proxy: Proxy): Future[Document] =
    HttpClient.execute(s"https://t.me/${channel.url}/$postId", params = queryParams, proxy = proxy, timeout = 2 seconds)
      .flatMap(r => Unmarshal(r.entity).to[String])
      .map(Jsoup.parse)

  private def checkResponse(doc: Document): Future[Option[Document]] = {
    val error = doc.select(".tgme_widget_message_error").text.trim

    error match {
      case e if e == "Post not found" => Future.successful(Option.empty)
      case e if e contains "Channel with username" => Future.failed(ParsingException(e))
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
  case class Parse(channel: Channel, postId: PostID, proxy: Proxy)

  case class ParsingException(message: String) extends Exception(message)
}
