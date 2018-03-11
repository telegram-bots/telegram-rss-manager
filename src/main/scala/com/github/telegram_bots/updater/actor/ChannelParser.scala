package com.github.telegram_bots.updater.actor

import akka.actor.{Actor, ActorRef, Props}
import com.github.telegram_bots.core.ReactiveActor
import com.github.telegram_bots.core.domain.types._
import com.github.telegram_bots.core.domain.{Channel, Post, PresentPost}
import com.github.telegram_bots.core.implicits.ExtendedAnyRef
import com.github.telegram_bots.updater.actor.ChannelParser._
import com.github.telegram_bots.updater.actor.PostParser.{ParseRequest, ParseResponse}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


class ChannelParser(batchSize: Int) extends Actor with ReactiveActor {
  val postParser: ActorRef = context.actorOf(PostParser.props, PostParser.getClass.getSimpleName)
  val postStorage: mutable.Map[ChannelURL, ListBuffer[Post]] = mutable.Map()
  val senders: mutable.Map[ChannelURL, ActorRef] = mutable.Map()

  override def receive: Receive = {
    case Start(channel, proxy) => start(channel, proxy)
    case ParseResponse(channel, post) => process(channel, post)
  }

  private def start(channel: Channel, proxy: Proxy): Unit = {
    val Channel(url, startPostId) = channel
    val endPostId = startPostId + batchSize - 1

    log.info(s"Start parsing: $url [$startPostId..$endPostId] $proxy")

    senders(url) = sender

    for (postId <- startPostId to endPostId) {
      postParser ! ParseRequest(channel, postId, proxy)
    }
  }

  private def process(channel: Channel, post: Post): Unit = {
    val Channel(url, _) = channel

    val currentUrlPosts = postStorage.getOrElseUpdate(url, ListBuffer()).also(_ += post)
    if (currentUrlPosts.lengthCompare(batchSize) == 0) {
      val sender = senders(url)
      val nonEmptyPosts = currentUrlPosts.collect { case post: PresentPost => post }.sortBy(_.id)
      val firstPostId = nonEmptyPosts.head.id
      val lastPostId = nonEmptyPosts.lastOption.map(_.id)

      postStorage -= url
      senders -= url

      log.info(s"Complete parsing: $url [$firstPostId..${lastPostId.getOrElse("-")}] (${nonEmptyPosts.size} not empty)")

      sender ! Complete(channel, lastPostId, nonEmptyPosts)
    }
  }
}

object ChannelParser {
  def props(batchSize: Int): Props = Props(new ChannelParser(batchSize))
      .withDispatcher("channelDispatcher")

  case class Start(channel: Channel, proxy: Proxy)

  case class Complete(channel: Channel, endPostId: Option[PostID], posts: Seq[Post])
}
