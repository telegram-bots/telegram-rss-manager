package com.github.telegram_bots.updater.persistence

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneId}

import com.github.telegram_bots.core.domain.PostType.PostType
import com.github.telegram_bots.core.domain.{Post, PostType, PresentPost}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class PostRepository(db: Database) {
  val postsQuery: TableQuery[Posts] = TableQuery[Posts]

  def saveAll(posts: Seq[Post]): Future[Option[Int]] = db.run { postsQuery ++= posts.map(_.asInstanceOf[PresentPost]) }
}

class Posts(tag: Tag) extends Table[PresentPost](tag, "posts") {
  private implicit val localDateTimeMapper = MappedColumnType.base[LocalDateTime, Timestamp](
    date => Timestamp.from(date.atZone(ZoneId.systemDefault()).toInstant),
    _.toInstant.atZone(ZoneId.systemDefault()).toLocalDateTime
  )
  private implicit val postTypeMapper = MappedColumnType.base[PostType, String](
    _.toString,
    PostType.withName
  )

  def id: Rep[Int] = column[Int]("id")
  def `type`: Rep[PostType] = column[PostType]("post_type")
  def content: Rep[String] = column[String]("content")
  def fileURL: Rep[Option[String]] = column[Option[String]]("file_url")
  def date: Rep[LocalDateTime] = column[LocalDateTime]("date")
  def author: Rep[Option[String]] = column[Option[String]]("author")
  def channelLink: Rep[String] = column[String]("channel_link")
  def channelName: Rep[String] = column[String]("channel_name")

  def * = (id, `type`, content, fileURL, date, author, channelLink, channelName) <>
      ((PresentPost.apply _).tupled, PresentPost.unapply)
}
