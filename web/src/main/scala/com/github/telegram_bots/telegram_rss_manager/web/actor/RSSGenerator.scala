package com.github.telegram_bots.telegram_rss_manager.web.actor

import java.time.{ZoneId, ZonedDateTime}

import akka.actor.Actor
import akka.util.ByteString
import com.github.telegram_bots.telegram_rss_manager.core.actor.ReactiveActor
import com.github.telegram_bots.telegram_rss_manager.core.config.ConfigProperties
import com.github.telegram_bots.telegram_rss_manager.core.domain.Subscription.SubscriptionName
import com.github.telegram_bots.telegram_rss_manager.core.domain.User.TelegramID
import com.github.telegram_bots.telegram_rss_manager.core.domain.{Post, PresentPost}
import com.github.telegram_bots.telegram_rss_manager.core.domain.Post.PostType._
import com.github.telegram_bots.telegram_rss_manager.web.Implicits.ExtendedNode
import com.github.telegram_bots.telegram_rss_manager.web.actor.RSSGenerator.{GenerateRequest, GenerateResponse, Properties}
import com.github.telegram_bots.telegram_rss_manager.web.component.RSSFeedGenerator
import com.github.telegram_bots.telegram_rss_manager.web.component.RSSFeedGenerator.{FeedChannel, FeedEntry}
import com.typesafe.config.Config

class RSSGenerator(config: Config) extends Actor with ReactiveActor {
  val props = new Properties(config)

  override def receive: Receive = {
    case GenerateRequest(userId, subscriptionName, posts) =>
      log.debug(s"Requested Generate($userId, $subscriptionName, ${posts.size})")

      sender ! GenerateResponse(generate(userId, subscriptionName, posts))
  }

  private def generate(telegramID: TelegramID, subscriptionName: SubscriptionName, posts: Seq[Post]): ByteString = {
    RSSFeedGenerator
      .generate(
        FeedChannel(
          title = s"$subscriptionName - ${props.titlePostfix}",
          link = s"${props.linkPrefix}/$telegramID/$subscriptionName",
          description = props.description,
          managingEditor = props.managingEditor,
          webMaster = props.webMaster,
          date = ZonedDateTime.now()
        ),
        posts.map(_.asInstanceOf[PresentPost]).map(post => FeedEntry(
          title = s"${post.id} - ${post.channelName}",
          link = s"https://t.me/${post.channelLink}/${post.id}",
          description = wrapMedia(post) + post.content,
          date = post.date.atZone(ZoneId.systemDefault())
        ))
      )
      .toByteString()
  }

  private def wrapMedia(post: PresentPost): String = post.`type` match {
    case _ if post.fileURL.isEmpty => ""
    case IMAGE => s"""<img src="${post.fileURL}" /><br>"""
    case STICKER => s"""<img src="${post.fileURL}" /><br>"""
    case VIDEO => s"""<video src="${post.fileURL}" /><br>"""
    case _ => s"${post.fileURL}<br>"
  }
}

object RSSGenerator {
  case class GenerateRequest(telegramID: TelegramID, subscriptionName: SubscriptionName, posts: Seq[Post])

  case class GenerateResponse(payload: ByteString)

  class Properties(root: Config) extends ConfigProperties(root, "akka.actor.config.rss-generator.channel") {
    val titlePostfix: String = self.getString("title-postfix")

    val linkPrefix: String = self.getString("link-prefix")

    val description: String = self.getString("description")

    val managingEditor: String = self.getString("managing-editor")

    val webMaster: String = self.getString("web-master")
  }
}
