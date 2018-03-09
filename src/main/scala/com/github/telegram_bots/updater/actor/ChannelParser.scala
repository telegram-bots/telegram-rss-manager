package com.github.telegram_bots.updater.actor

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Sink, Source}
import com.github.telegram_bots.core.ReactiveActor
import com.github.telegram_bots.core.domain.types._
import com.github.telegram_bots.core.domain.{Channel, EmptyPost, Post, PresentPost}
import com.github.telegram_bots.updater.actor.ChannelParser.{ProcessRequest, _}
import com.github.telegram_bots.updater.actor.PostParser.Parse

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext


class ChannelParser(batchSize: Int, totalSize: Int) extends Actor with ReactiveActor {
  override implicit val dispatcher: ExecutionContext = system.dispatchers.lookup(ChannelParser.dispatcher)

  val postParser: ActorRef = context.actorOf(PostParser.props)
  val postStorage: mutable.Map[String, ListBuffer[Post]] = mutable.Map[String, ListBuffer[Post]]()

  override def receive: Receive = {
    case action: Start => start(action)
    case action: ProcessRequest => sendRequest(action)
    case action: ProcessResponse => receiveResponse(action)
  }

  private def start(action: Start): Unit = {
    val Start(channel, proxy) = action

    log.info(s"Start parsing: ${channel.url} [${channel.lastPostId}..${channel.lastPostId + totalSize - 1}] $proxy")
    self ! ProcessRequest(sender, channel, channel.lastPostId, proxy)
  }

  private def sendRequest(request: ProcessRequest): Unit = {
    val ProcessRequest(sender, channel, lastPostId, proxy) = request

    val batchResponse = Source(lastPostId until lastPostId + batchSize)
      .mapAsyncUnordered(batchSize) { postId => postParser ? Parse(channel.url, postId, proxy) }
      .map(_.asInstanceOf[Post])
      .withAttributes(ActorAttributes.dispatcher(ChannelParser.dispatcher))
      .grouped(batchSize)
      .map { posts => ProcessResponse(sender, channel, posts, proxy) }
      .runWith(Sink.head)

    pipe(batchResponse) to self
  }

  private def receiveResponse(response: ProcessResponse): Unit = {
    val ProcessResponse(sender, channel, posts , proxy) = response

    postStorage.getOrElseUpdate(channel.url, ListBuffer()) ++= posts

    lazy val totalPostCount = postStorage(channel.url).size
    lazy val emptyPostInBatchCount = posts.count(_.isInstanceOf[EmptyPost])

    if (totalPostCount == totalSize || emptyPostInBatchCount == batchSize) {
      val posts = postStorage(channel.url).filter(_.isInstanceOf[PresentPost]).sortBy(_.id)
      val lastPostId = posts.lastOption.map(_.id)

      postStorage -= channel.url

      log.info(s"Complete parsing: ${channel.url} [${channel.lastPostId}..${lastPostId.getOrElse("-")}] (${posts.size} not empty)")

      sender ! Complete(channel, lastPostId, posts)
    } else {
      val nextBatchPostId = posts.maxBy(_.id).id + 1

      self ! ProcessRequest(sender, channel, nextBatchPostId, proxy)
    }
  }
}

object ChannelParser {
  def dispatcher = "channelDispatcher"

  def props(batchSize: Int, totalSize: Int): Props =
    Props(new ChannelParser(batchSize, totalSize)).withDispatcher(dispatcher)

  case class Start(channel: Channel, proxy: Proxy)

  case class ProcessRequest(sender: ActorRef, channel: Channel, lastPostId: PostID, proxy: Proxy)

  case class ProcessResponse(sender: ActorRef, channel: Channel, posts: Seq[Post], proxy: Proxy)

  case class Complete(channel: Channel, lastPostId: Option[PostID], posts: Seq[Post])
}
