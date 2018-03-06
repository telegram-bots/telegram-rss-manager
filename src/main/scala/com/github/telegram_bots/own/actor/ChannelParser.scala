package com.github.telegram_bots.own.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.telegram_bots.own.Implicits._
import com.github.telegram_bots.own.actor.ChannelParser._
import com.github.telegram_bots.own.actor.PostParser.Parse
import com.github.telegram_bots.own.component.PostStorage
import com.github.telegram_bots.own.domain.ProcessedPost
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
    log.info(s"Start parsing: ${action.url} [${action.startingPostId}..${action.startingPostId + MAX_SIZE - 1}] ${action.proxy}")
    self ! SendProcessRequest(action.url, action.startingPostId, action.startingPostId, action.proxy)
  }

  private def sendRequest(request: SendProcessRequest): Unit = {
    for (postId <- request.lastPostId until request.lastPostId + BATCH_SIZE) {
      val batchId = math.ceil(postId.toDouble / BATCH_SIZE).toInt
      postParser ! Parse(request.url, request.startingPostId, postId, batchId, request.proxy)
    }
  }

  private def receiveResponse(response: ReceiveProcessResponse): Unit = {
    log.debug(s"Processed post ${response.url}:${response.post.postId}")

    storage.append(response.post)

    val postCount = storage.count(response.url)

    if (postCount == MAX_SIZE) {
      self ! Complete(response.url, storage.getAndRemove(response.url))
    } else if (postCount % BATCH_SIZE == 0) {
      val emptyPostCount = storage.countEmpty(response.url, response.post.batchId)

      if (emptyPostCount == BATCH_SIZE) {
        self ! Complete(response.url, storage.getAndRemove(response.url))
      } else {
        self ! SendProcessRequest(
          response.url,
          response.startingPostId,
          response.post.batchId * BATCH_SIZE + response.startingPostId,
          response.proxy
        )
      }
    }
  }

  private def complete(action: Complete): Unit = {
    log.info(s"For ${action.url} processed ${action.posts.size} posts")
    log.debug(s"Content: ${action.posts.map(p => (p.batchId, p.postId)).groupBy(_._1).mapValues(_.map(_._2)).toSortedMap}")
  }
}

object ChannelParser {
  def props: Props = Props[ChannelParser].withDispatcher("channelDispatcher")

  val BATCH_SIZE = 5
  val MAX_SIZE = 50

  case class Start(url: String, startingPostId: Int = 1, proxy: Proxy)

  case class SendProcessRequest(url: String, startingPostId: Int, lastPostId: Int, proxy: Proxy)

  case class ReceiveProcessResponse(url: String, startingPostId: Int, post: ProcessedPost, proxy: Proxy)

  case class Complete(url: String, posts: List[ProcessedPost])
}
