package com.github.telegram_bots.telegram_rss_manager.updater.actor.parser

import java.util.concurrent.TimeoutException

import akka.actor.{ActorRef, FSM}
import akka.event.{Logging, LoggingAdapter}
import com.github.telegram_bots.telegram_rss_manager.core.actor.ReactiveActor
import com.github.telegram_bots.telegram_rss_manager.core.config.ConfigProperties
import com.github.telegram_bots.telegram_rss_manager.core.domain.Post.PostID
import com.github.telegram_bots.telegram_rss_manager.core.domain.{Channel, Post, PresentPost, Proxy}
import com.github.telegram_bots.telegram_rss_manager.updater.actor.parser.ChannelParser._
import com.github.telegram_bots.telegram_rss_manager.updater.actor.parser.PostParser.Parse
import com.softwaremill.tagging.@@
import com.typesafe.config.Config

import scala.concurrent.duration._
import scala.language.postfixOps


class ChannelParser(config: Config, postParser: ActorRef @@ PostParser) extends FSM[State, Data] with ReactiveActor {
  override implicit val log: LoggingAdapter = Logging(system, this)
  val props: Properties = new Properties(config)

  startWith(Idle, Uninitialized)

  when(Idle) {
    case Event(Start(channel, proxy), Uninitialized) =>
      val Channel(_, url, _, currentPostId) = channel
      val range = currentPostId + 1 to currentPostId + props.batchSize
      log.info(s"Start parsing: $url [$range] $proxy")

      for (postId <- range) postParser ! Parse(channel, postId, proxy)

      goto(Processing) using StateData(sender, channel, range, List())
  }

  when(Processing, stateTimeout = props.batchSize * 2 seconds) {
    case Event(Next(channel, post), StateData(respondTo, _, range, posts)) =>
      val newPosts = posts :+ post
      if (!range.contains(post.id)) {
        stay
      } else if (newPosts.lengthCompare(props.batchSize) != 0) {
        stay using StateData(respondTo, channel, range, newPosts)
      } else {
        complete(respondTo, channel, newPosts)

        goto(Idle) using Uninitialized
      }
    case Event(Failure(e), StateData(respondTo, channel, _, posts)) =>
      complete(respondTo, channel, posts, Some(e))

      goto(Idle) using Uninitialized
    case Event(StateTimeout, StateData(respondTo, channel, _, posts)) =>
      complete(respondTo, channel, posts, Some(new TimeoutException("Timed out parsing")))

      goto(Idle) using Uninitialized
  }

  whenUnhandled {
    case Event(Failure(e), _) =>
      log.debug(s"Received failure for discarded request: $e")
      stay
  }

  private def complete(
    respondTo: ActorRef,
    channel: Channel,
    posts: Seq[Post],
    failure: Option[Exception] = None
  ): Unit = {
    def getId(post: Option[Post]) = post.map(_.id).getOrElse(channel.lastPostId)
    val sortedPosts = posts.sortBy(_.id)
    val nonEmptyPosts = sortedPosts.collect { case post: PresentPost => post }
    val range = getId(sortedPosts.headOption) to getId(sortedPosts.lastOption)
    val message = s"${channel.url} [$range] (${nonEmptyPosts.map(_.id)} not empty)"

    failure match {
      case Some(e) => log.warning(s"Complete parsing with failure: $message", e)
      case _ => log.info(s"Complete parsing: $message")
    }

    respondTo ! Complete(channel, range.end, nonEmptyPosts)
  }
}

object ChannelParser {
  sealed trait Event
  case class Start(channel: Channel, proxy: Proxy) extends Event
  case class Next(channel: Channel, post: Post) extends Event
  case class Failure(cause: Exception) extends Event
  case class Complete(channel: Channel, endPostId: PostID, posts: Seq[Post]) extends Event

  sealed trait State
  case object Idle extends State
  case object Processing extends State

  sealed trait Data
  case object Uninitialized extends Data
  case class StateData(respondTo: ActorRef, channel: Channel, range: Range, posts: Seq[Post]) extends Data

  class Properties(root: Config) extends ConfigProperties(root, "akka.actor.config.channel-parser") {
    val batchSize: Int = self.getInt("batch-size")
  }
}
