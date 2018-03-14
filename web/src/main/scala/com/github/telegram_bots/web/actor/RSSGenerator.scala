package com.github.telegram_bots.web.actor

import java.time.{ZoneId, ZonedDateTime}

import akka.actor.Actor
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.core.config.ConfigProperties
import com.github.telegram_bots.core.domain.{Post, PostType, PresentPost}
import com.github.telegram_bots.web.actor.RSSGenerator.{Generate, Properties}
import com.github.telegram_bots.web.component.RSSFeedGenerator
import com.github.telegram_bots.web.component.RSSFeedGenerator.{FeedChannel, FeedEntry}
import com.typesafe.config.Config

import scala.xml.Elem

class RSSGenerator(config: Config) extends Actor with ReactiveActor {
  val props = new Properties(config)

  override def receive: Receive = {
    case Generate(userId, subscriptionName, posts) =>
      sender ! generate(userId, subscriptionName, posts)
  }

  private def generate(userId: Long, subscriptionName: String, posts: Seq[Post]): Elem = {
    RSSFeedGenerator.generate(
      FeedChannel(
        title = s"$subscriptionName - ${props.titlePostfix}",
        link = s"${props.linkPrefix}/$userId/$subscriptionName",
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
  }

  private def wrapMedia(post: PresentPost): String = post.`type` match {
    case _ if post.fileURL.isEmpty => ""
    case PostType.IMAGE => s"""<img src="${post.fileURL}" /><br>"""
    case PostType.STICKER => s"""<img src="${post.fileURL}" /><br>"""
    case PostType.VIDEO => s"""<video src="${post.fileURL}" /><br>"""
    case _ => s"${post.fileURL}<br>"
  }
}

object RSSGenerator {
  case class Generate(userId: Long, subscriptionName: String, posts: Seq[Post])

  class Properties(root: Config) extends ConfigProperties(root, "akka.actor.config.rss-generator.channel") {
    val titlePostfix: String = self.getString("title-postfix")

    val linkPrefix: String = self.getString("link-prefix")

    val description: String = self.getString("description")

    val managingEditor: String = self.getString("managing-editor")

    val webMaster: String = self.getString("web-master")
  }
}
