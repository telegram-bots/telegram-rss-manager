package com.github.telegram_bots.telegram_rss_manager.core.persistence

import java.time.LocalDateTime

import com.github.telegram_bots.telegram_rss_manager.core.domain.Post.PostType.PostType
import com.github.telegram_bots.telegram_rss_manager.core.domain.Subscription.SubscriptionName
import com.github.telegram_bots.telegram_rss_manager.core.domain.User.TelegramID
import com.github.telegram_bots.telegram_rss_manager.core.domain.{Post, PresentPost}
import com.github.telegram_bots.telegram_rss_manager.core.persistence.Mappers._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Index

import scala.concurrent.Future

class PostRepository(db: Database) {
  private val postsQuery = TableQuery[Posts]

  def getLatest(userId: TelegramID, subscriptionName: SubscriptionName, limit: Int): Future[Seq[Post]] = {
    val query = sql"""
      SELECT p.*
      FROM users AS u
        JOIN subscriptions AS s ON s.user_id = u.id
        JOIN channels AS c ON c.id = s.channel_id
        JOIN posts AS p ON p.channel_link = c.url
      WHERE u.telegram_id = $userId AND s.name = $subscriptionName
      ORDER BY p.date DESC
      LIMIT $limit;
      """

    db.run { query.as[PresentPost] }
  }

  def saveAll(posts: Seq[Post]): Future[Option[Int]] =
    db.run { postsQuery ++= posts.map(_.asInstanceOf[PresentPost]) }

  class Posts(tag: Tag) extends Table[PresentPost](tag, "posts") {
    def id: Rep[Int] = column[Int]("id")
    def `type`: Rep[PostType] = column[PostType]("post_type")
    def content: Rep[String] = column[String]("content")
    def fileURL: Rep[Option[String]] = column[Option[String]]("file_url")
    def date: Rep[LocalDateTime] = column[LocalDateTime]("date")
    def author: Rep[Option[String]] = column[Option[String]]("author")
    def channelLink: Rep[String] = column[String]("channel_link")
    def channelName: Rep[String] = column[String]("channel_name")
    def index: Index = index("id_channel_link_constraint", (id, channelLink), unique = true)

    def * = (id, `type`, content, fileURL, date, author, channelLink, channelName) <>
      ((PresentPost.apply _).tupled, PresentPost.unapply)
  }
}