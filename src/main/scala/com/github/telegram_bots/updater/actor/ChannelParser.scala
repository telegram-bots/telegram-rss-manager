package com.github.telegram_bots.updater.actor

import akka.actor.{Actor, ActorRef}
import com.github.telegram_bots.core.Implicits.ExtendedAnyRef
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.config.ConfigProperties
import com.github.telegram_bots.core.domain.Types._
import com.github.telegram_bots.core.domain.{Channel, Post, PresentPost}
import com.github.telegram_bots.updater.actor.ChannelParser._
import com.github.telegram_bots.updater.actor.PostParser.{ParseRequest, ParseResponse}
import com.softwaremill.tagging.@@
import com.typesafe.config.Config

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


class ChannelParser(config: Config, postParser: ActorRef @@ PostParser) extends Actor with ReactiveActor {
  val props: Properties = new Properties(config)
  val postStorage: mutable.Map[ChannelURL, ListBuffer[Post]] = mutable.Map()
  val senders: mutable.Map[ChannelURL, ActorRef] = mutable.Map()

  override def receive: Receive = {
    case Start(channel, proxy) => start(channel, proxy)
    case ParseResponse(channel, post) => process(channel, post)
  }

  private def start(channel: Channel, proxy: Proxy): Unit = {
    val Channel(_, url, startPostId) = channel
    val endPostId = startPostId + props.batchSize - 1

    log.info(s"Start parsing: $url [$startPostId..$endPostId] $proxy")

    senders(url) = sender

    for (postId <- startPostId to endPostId) {
      postParser ! ParseRequest(channel, postId, proxy)
    }
  }

  private def process(channel: Channel, post: Post): Unit = {
    val url = channel.url
    val postCount = postStorage.getOrElseUpdate(url, ListBuffer()).also(_ += post).size

    if (postCount == props.batchSize) {
      val sender = senders.remove(url).get
      val allPosts = postStorage.remove(url).get.sortBy(_.id)
      val nonEmptyPosts = allPosts.collect { case post: PresentPost => post }
      val firstPostId = allPosts.head.id
      val lastPostId = nonEmptyPosts.lastOption.map(_.id)

      log.info(s"Complete parsing: $url [$firstPostId..${lastPostId.getOrElse("-")}] (${nonEmptyPosts.size} not empty)")

      sender ! Complete(channel, lastPostId, nonEmptyPosts)
    }
  }
}

object ChannelParser {
  case class Start(channel: Channel, proxy: Proxy)

  case class Complete(channel: Channel, endPostId: Option[PostID], posts: Seq[Post])

  class Properties(root: Config) extends ConfigProperties(root, "akka.actor.self.channel-parser") {
    val batchSize: Int = self.getInt("batch-size")
  }
}
