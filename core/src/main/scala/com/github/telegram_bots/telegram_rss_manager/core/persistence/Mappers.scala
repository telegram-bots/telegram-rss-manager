package com.github.telegram_bots.telegram_rss_manager.core.persistence

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneId}

import com.github.telegram_bots.telegram_rss_manager.core.domain._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{GetResult, JdbcType}

object Mappers {
  def enumMapper(enum: Enumeration): JdbcType[enum.Value] = MappedColumnType.base[enum.Value, String](
    _.toString,
    str => enum.withName(str)
  )

  implicit val localDateTimeMapper: JdbcType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
    date => Timestamp.from(date.atZone(ZoneId.systemDefault()).toInstant),
    _.toLocalDateTime
  )

  implicit val postTypeMapper: JdbcType[Post.PostType.Value] = enumMapper(Post.PostType)

  implicit val postMapper: GetResult[PresentPost] = GetResult(r => PresentPost(
    r.<<,
    Post.PostType.withName(r.<<),
    r.<<,
    r.<<?,
    r.<<[java.sql.Timestamp].toLocalDateTime,
    r.<<?,
    r.<<,
    r.<<
  ))

  implicit val channelMapper: GetResult[Channel] = GetResult(r => Channel(r.<<, r.<<, r.<<, r.<<))

  implicit val userMapper: GetResult[User] = GetResult(r => User(r.<<, r.<<))

  implicit val subscriptionMapper: GetResult[Subscription] = GetResult(r => Subscription(r.<<, r.<<, r.<<))
}
