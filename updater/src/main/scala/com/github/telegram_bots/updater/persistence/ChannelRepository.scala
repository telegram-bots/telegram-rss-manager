package com.github.telegram_bots.updater.persistence

import java.sql.Timestamp

import com.github.telegram_bots.core.domain.Channel
import com.github.telegram_bots.core.persistence.Mappers.channelMapper
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class ChannelRepository(db: Database) {
  private val channelQuery: TableQuery[Channels] = TableQuery[Channels]

  def getAndLock(workerSystem: String): Future[Option[Channel]] = {
    val query = sql"""
       UPDATE channels
       SET worker = $workerSystem
       FROM (
         SELECT * FROM channels
         WHERE worker IS NULL
         ORDER BY updated_at ASC
         LIMIT 1
         FOR UPDATE
       ) channel
       WHERE channel.id = channels.id
       RETURNING channel.id, channel.url, channel.last_post_id;
       """

    db.run { query.as[Channel].headOption }
  }

  def updateAndUnlock(channel: Channel, workerSystem: String): Future[Int] = {
    val query = channelQuery
      .filter(_.id === channel.id)
      .filter(_.worker === workerSystem)
      .map(c => (c.worker, c.lastPostId))
      .update(None, channel.lastPostId)

    db.run { query }
  }

  def unlockAll(workerSystem: String): Future[Int] = {
    val query = channelQuery.filter(_.worker === workerSystem)
      .map(_.worker)
      .update(None)

    db.run { query }
  }

  class Channels(tag: Tag) extends Table[Channel](tag, "channels") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def url: Rep[String] = column[String]("url")
    def lastPostId: Rep[Int] = column[Int]("last_post_id")
    def worker: Rep[Option[String]] = column[Option[String]]("worker")
    def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at")

    def * = (id, url, lastPostId) <> ((Channel.apply _).tupled, Channel.unapply)
  }
}