package com.github.telegram_bots.telegram_rss_manager.core.persistence

import java.sql.Timestamp

import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel
import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel.{ChannelName, ChannelURL, Worker}
import com.github.telegram_bots.telegram_rss_manager.core.persistence.Mappers.channelMapper
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class ChannelRepository(db: Database) {
  private implicit val executionContext: ExecutionContext = db.ioExecutionContext
  private val channelQuery = TableQuery[Channels]

  def find(url: ChannelURL): Future[Option[Channel]] = {
    val query = channelQuery.filter(_.url === url)

    db.run { query.result.headOption }
  }

  def getOrCreate(url: ChannelURL, name: ChannelName): Future[Channel] = {
    val query = for {
      existing <- channelQuery.filter(_.url === url).result.headOption
      row = existing.map(_.copy(name = name)) getOrElse Channel(0, url, name, -1)
      result <- (channelQuery returning channelQuery).insertOrUpdate(row).map(_.getOrElse(row))
    } yield result

    db.run { query.transactionally }
  }

  def getAndLock(worker: Worker): Future[Option[Channel]] = {
    val query = sql"""
       UPDATE channels
       SET worker = $worker
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

  def updateAndUnlock(channel: Channel, worker: Worker): Future[Int] = {
    val query = channelQuery
      .filter(_.id === channel.id)
      .filter(_.worker === worker)
      .map(c => (c.worker, c.lastPostId))
      .update(None, channel.lastPostId)

    db.run { query }
  }

  def unlockAll(worker: Worker): Future[Int] = {
    val query = channelQuery.filter(_.worker === worker)
      .map(_.worker)
      .update(None)

    db.run { query }
  }

  class Channels(tag: Tag) extends Table[Channel](tag, "channels") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def url: Rep[String] = column[String]("url")
    def name: Rep[String] = column[String]("name")
    def lastPostId: Rep[Int] = column[Int]("last_post_id")
    def worker: Rep[Option[String]] = column[Option[String]]("worker")
    def updatedAt: Rep[Timestamp] = column[Timestamp]("updated_at")

    def * = (id, url, name, lastPostId) <> ((Channel.apply _).tupled, Channel.unapply)
  }
}