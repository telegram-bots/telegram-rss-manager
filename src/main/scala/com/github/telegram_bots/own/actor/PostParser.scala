package com.github.telegram_bots.own.actor

import java.time.ZoneId

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.SmallestMailboxPool
import com.github.telegram_bots.own.actor.PostParser.Parse
import com.github.telegram_bots.own.component.PostDataExtractor._
import com.github.telegram_bots.own.component.PostDownloader._
import com.github.telegram_bots.own.domain.Types._
import com.github.telegram_bots.own.domain._
import org.jsoup.nodes.Document

import scala.language.postfixOps
import scala.util.{Failure, Success}

class PostParser extends Actor with ActorLogging {
  def receive: Receive = {
    case Parse(url, startingPostId, postId, proxy) =>
      val result = download(url, postId, proxy).map(parse(url, postId)(_))

      result match {
        case Success(post) =>
          log.debug(s"Parsed post: $url [$postId] $post")
          sender ! post
        case Failure(e) =>
          log.warning(s"Failed to parse ${e.getMessage}, retrying...")
          self ! Parse(url, startingPostId, postId, proxy)
      }
  }

  private def parse(url: ChannelURL, postId: PostID)(document: Option[Document]): Post = document match {
    case Some(message) =>
      val `type` = getType(message)

      PresentPost(
        id = postId,
        `type` = `type`,
        content = getText(message, `type`),
        date = getDate(message).atZone(ZoneId.systemDefault()).toEpochSecond,
        author = getAuthor(message),
        channelLink = url,
        channelName = getName(message),
        fileURL = getFileURL(message, `type`)
      )
    case _ => EmptyPost(id = postId, channelLink = url)
  }
}

object PostParser {
  def props: Props = Props[PostParser].withDispatcher("postDispatcher").withRouter(new SmallestMailboxPool(25))

  case class Parse(url: ChannelURL, startingPostId: PostID, postId: PostID, proxy: Proxy)
}
