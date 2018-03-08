package com.github.telegram_bots.own.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.github.telegram_bots.own.actor.ChannelParser.{SendProcessRequest, props, _}
import com.github.telegram_bots.own.actor.PostParser.Parse
import com.github.telegram_bots.own.domain.Types._
import com.github.telegram_bots.own.domain.{EmptyPost, Post, PresentPost}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class ChannelParser extends Actor with ActorLogging {
  val postParser: ActorRef = context.actorOf(PostParser.props)
  val postStorage: mutable.Map[String, ListBuffer[Post]] = mutable.Map[String, ListBuffer[Post]]()

  implicit val system: ActorSystem = context.system
  implicit val executionContext: ExecutionContext = system.dispatchers.lookup(props.dispatcher)
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val materializer: Materializer = ActorMaterializer()

  override def receive: Receive = {
    case action: Start => start(action)
    case action: SendProcessRequest => sendRequest(action)
    case action: ReceiveProcessResponse => receiveResponse(action)
    case action: Complete => complete(action)
  }

  private def start(action: Start): Unit = {
    val Start(url, startingPostId, proxy) = action

    log.info(s"Start parsing: $url [$startingPostId..${startingPostId + MAX_SIZE - 1}] $proxy")
    self ! SendProcessRequest(url, startingPostId, startingPostId, proxy)
  }

  private def sendRequest(request: SendProcessRequest): Unit = {
    val SendProcessRequest(url, startingPostId, lastPostId, proxy) = request

    val batchResponse = Source(lastPostId until lastPostId + BATCH_SIZE)
      .mapAsyncUnordered(BATCH_SIZE) { postId => postParser ? Parse(url, postId, proxy) }
      .map(_.asInstanceOf[Post])
      .runWith(Sink.collection)
      .map { posts => ReceiveProcessResponse(url, startingPostId, posts, proxy) }

    pipe(batchResponse) to self
  }

  private def receiveResponse(response: ReceiveProcessResponse): Unit = {
    val ReceiveProcessResponse(url, startingPostId, posts, proxy) = response

    postStorage.getOrElseUpdate(url, ListBuffer()) ++= posts

    lazy val totalPostCount = postStorage(url).size
    lazy val emptyPostInBatchCount = posts.count(_.isInstanceOf[EmptyPost])

    if (totalPostCount == MAX_SIZE || emptyPostInBatchCount == BATCH_SIZE) {
      val posts = postStorage(url).filter(_.isInstanceOf[PresentPost]).sortBy(_.id)

      postStorage -= url

      self ! Complete(url, startingPostId, posts)
    } else {
      val nextBatchStartingId = posts.maxBy(_.id).id + 1

      self ! SendProcessRequest(url, startingPostId, nextBatchStartingId, proxy)
    }
  }

  private def complete(action: Complete): Unit = {
    val Complete(url, startingPostId, posts) = action
    val lastPostId = posts.lastOption.map(_.id.toString).getOrElse("-")

    log.info(s"Complete parsing: $url [$startingPostId..$lastPostId] (${posts.size} not empty)")
    log.debug(s"Empty posts: ${(startingPostId until startingPostId + MAX_SIZE).diff(posts.map(_.id))}")
  }
}

object ChannelParser {
  def props: Props = Props[ChannelParser].withDispatcher("channelDispatcher")

  val BATCH_SIZE = 5
  val MAX_SIZE = 50

  case class Start(url: ChannelURL, startingPostId: PostID, proxy: Proxy)

  case class SendProcessRequest(url: ChannelURL, startingPostId: PostID, lastPostId: PostID, proxy: Proxy)

  case class ReceiveProcessResponse(url: ChannelURL, startingPostId: PostID, posts: Seq[Post], proxy: Proxy)

  case class Complete(url: ChannelURL, startingPostId: PostID, posts: Seq[Post])
}
