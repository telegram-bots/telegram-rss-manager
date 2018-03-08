package com.github.telegram_bots.own.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.telegram_bots.own.actor.ChannelParser.{SendProcessRequest, _}
import com.github.telegram_bots.own.actor.PostParser.Parse
import com.github.telegram_bots.own.component.PostStorage
import com.github.telegram_bots.own.domain.Post
import com.github.telegram_bots.own.domain.Types._


class ChannelParser extends Actor with ActorLogging {
  val postParser: ActorRef = context.actorOf(PostParser.props)
  val storage: PostStorage = new PostStorage

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

    for (postId <- lastPostId until lastPostId + BATCH_SIZE) {
      val batchId = getBatchId(startingPostId, postId)
      postParser ! Parse(url, startingPostId, batchId, postId, proxy)
    }
  }

  private def receiveResponse(response: ReceiveProcessResponse): Unit = {
    val ReceiveProcessResponse(startingPostId, batchId, post, proxy) = response
    val url = post.channelLink

    storage.append(batchId, post)

    val postCount = storage.count(url)

    if (postCount == MAX_SIZE) {
      self ! Complete(url, startingPostId, storage.getAndRemove(url))
    } else if (postCount % BATCH_SIZE == 0) {
      val emptyPostCount = storage.countEmpty(url, batchId)

      if (emptyPostCount == BATCH_SIZE) {
        self ! Complete(url, startingPostId, storage.getAndRemove(url))
      } else {
        val lastPostId = batchId * BATCH_SIZE + response.startingPostId

        self ! SendProcessRequest(url, startingPostId, lastPostId, proxy)
      }
    }
  }

  private def complete(action: Complete): Unit = {
    val Complete(url, startingPostId, posts) = action
    val lastPostId = posts.lastOption.map(_.id.toString).getOrElse("-")

    log.info(s"Complete parsing: $url [$startingPostId..$lastPostId] (${posts.size} not empty) posts")
    log.debug(s"Content: ${posts.map(_.id)}")
  }

  private def getBatchId(startingPostId: PostID, postId: PostID): Int = {
    val id = for {
      startRange <- startingPostId until startingPostId + MAX_SIZE by BATCH_SIZE
      id <- startRange until startRange + BATCH_SIZE
      if id == postId
    } yield ((startRange / BATCH_SIZE) % (MAX_SIZE / BATCH_SIZE)) + 1

    id.headOption.getOrElse(throw new IllegalArgumentException(s"$postId"))
  }
}

object ChannelParser {
  def props: Props = Props[ChannelParser].withDispatcher("channelDispatcher")

  val BATCH_SIZE = 5
  val MAX_SIZE = 50

  case class Start(url: ChannelURL, startingPostId: PostID, proxy: Proxy)

  case class SendProcessRequest(url: ChannelURL, startingPostId: PostID, lastPostId: PostID, proxy: Proxy)

  case class ReceiveProcessResponse(startingPostId: PostID, batchId: BatchID, post: Post, proxy: Proxy)

  case class Complete(url: ChannelURL, startingPostId: PostID, posts: List[Post])
}
