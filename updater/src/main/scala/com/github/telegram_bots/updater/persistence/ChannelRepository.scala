package com.github.telegram_bots.updater.persistence

import java.sql.Timestamp

import com.github.telegram_bots.core.domain.Channel
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class ChannelRepository(db: Database) {
  private val channelQuery: TableQuery[Channels] = TableQuery[Channels]

  def firstNonLocked(): Future[Option[Channel]] = {
    val query = channelQuery.filter(!_.inWork)
      .sortBy(_.updatedAt.desc)
      .take(1)

    db.run { query.result.headOption }
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
    val query = channelQuery
      .filter(_.id === channel.id)
      .map(c => (c.inWork, c.workerSystem))
      .update(true, Some(workerSystem))

    db.run { query }
  }

  def unlock(channel: Channel, workerSystem: String): Future[Int] = {
    val query = channelQuery
      .filter(_.id === channel.id)
      .filter(_.workerSystem === workerSystem)
      .map(c => (c.inWork, c.workerSystem))
      .update(false, None)

    db.run { query }
  }

  def unlockAll(workerSystem: String): Future[Int] = {
    val query = channelQuery.filter(_.workerSystem === workerSystem)
      .map(c => (c.inWork, c.workerSystem))
      .update(false, None)

    db.run { query }
  }

  def run[R](action: DBIOAction[R, NoStream, Nothing]): Future[R] = db.run(action)
}

class Channels(tag: Tag) extends Table[Channel](tag, "channels") {
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def url: Rep[String] = column[String]("url")
  def lastPostId: Rep[Int] = column[Int]("last_post_id")
  def inWork: Rep[Boolean] = column[Boolean]("in_work")
  def workerSystem: Rep[Option[String]] = column[Option[String]]("worker_system")
  def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at")

  def * = (id, url, lastPostId) <> ((Channel.apply _).tupled, Channel.unapply)
}