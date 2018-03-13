package com.github.telegram_bots.updater.persistence

import com.github.telegram_bots.core.domain.Channel
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.GetResult
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class ChannelRepository(db: Database) {
  private implicit val getResult: AnyRef with GetResult[Channel] = GetResult(r => Channel(r.<<?, r.<<, r.<<))

  def firstNonLocked(): Future[Option[Channel]] = {
    val query = sql"""
      SELECT id, url, last_post_id FROM channels
      WHERE in_work = FALSE
      ORDER BY updated_at DESC
      LIMIT 1
      """

    db.run { query.as[Channel].headOption }
  }

  def update(channel: Channel): Future[Int] = {
    val query = sqlu"""
      UPDATE channels
      SET
        last_post_id = CASE
        WHEN ${channel.lastPostId} > last_post_id OR last_post_id IS NULL
        THEN ${channel.lastPostId} ELSE last_post_id
        END
      WHERE id = ${channel.id}
      """

    db.run { query }
  }

  def lock(channel: Channel, workerSystem: String): Future[Int] = {
    val query =sqlu"""
      UPDATE channels
      SET in_work = TRUE, worker_system = $workerSystem
      WHERE id = ${channel.id}
      """

    db.run { query }
  }

  def unlock(channel: Channel, workerSystem: String): Future[Int] = {
    val query = sqlu"""
      UPDATE channels
      SET in_work = FALSE, worker_system = NULL
      WHERE id = ${channel.id} AND worker_system = $workerSystem
      """

    db.run { query }
  }

  def unlockAll(workerSystem: String): Future[Int] = {
    val query =sqlu"""
      UPDATE channels
      SET in_work = FALSE, worker_system = NULL
      WHERE worker_system = $workerSystem
      """

    db.run { query }
  }

  def run[R](action: DBIOAction[R, NoStream, Nothing]): Future[R] = db.run(action)
}