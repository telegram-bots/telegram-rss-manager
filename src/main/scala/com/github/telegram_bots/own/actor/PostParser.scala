package com.github.telegram_bots.own.actor

import java.time.ZoneId

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.SmallestMailboxPool
import com.github.telegram_bots.own.actor.ChannelParser.ReceiveProcessResponse
import com.github.telegram_bots.own.actor.PostParser.Parse
import com.github.telegram_bots.own.component.PostDataExtractor._
import com.github.telegram_bots.own.component.PostDownloader._
import com.github.telegram_bots.own.domain.Types._
import com.github.telegram_bots.own.domain.{Post, ProcessedPost}
import org.jsoup.nodes.Document

import scala.language.postfixOps
import scala.util.{Failure, Success}

class PostParser extends Actor with ActorLogging {
  def receive: Receive = {
    case Parse(channelUrl, startingPostId, postId, batchId, proxy) =>
      val result = download(channelUrl, postId, proxy).map(_.map(parse))

      result match {
        case Success(post) =>
          sender ! ReceiveProcessResponse(channelUrl, startingPostId, ProcessedPost(channelUrl, batchId, postId, post), proxy)
        case Failure(e) =>
          self ! Parse(channelUrl, startingPostId, postId, batchId, proxy)
      }
  }

  private def parse(message: Document): Post = {
    val `type` = getType(message)
    val (url, name) = getURLAndName(message)

    Post(
      id = getId(message),
      `type` = `type`,
      content = getText(message, `type`),
      date = getDate(message).atZone(ZoneId.systemDefault()).toEpochSecond,
      author = getAuthor(message),
      channelLink = url,
      channelName = name,
      fileURL = getFileURL(message, `type`)
    )
  }
}

object PostParser {
  def props: Props = Props[PostParser].withDispatcher("postDispatcher").withRouter(new SmallestMailboxPool(25))

  case class Parse(channelUrl: String, startingPostId: Int, postId: Int, batchId: Int, proxy: Proxy)
}
