package com.github.telegram_bots.web.persistence

import com.github.telegram_bots.core.domain.{Post, PresentPost}
import com.github.telegram_bots.core.persistence.Mappers._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class PostRepository(db: Database) {
  def getLatest(userId: Long, subscriptionName: String, limit: Int): Future[Seq[Post]] = {
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
}