package com.github.telegram_bots.telegram_rss_manager.core.persistence

import com.github.telegram_bots.telegram_rss_manager.core.domain.Channel.ChannelID
import com.github.telegram_bots.telegram_rss_manager.core.domain.Subscription.SubscriptionName
import com.github.telegram_bots.telegram_rss_manager.core.domain.User.UserID
import com.github.telegram_bots.telegram_rss_manager.core.domain.{Channel, Subscription}
import com.github.telegram_bots.telegram_rss_manager.core.persistence.Mappers.channelMapper
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionRepository(db: Database) {
  private implicit val executionContext: ExecutionContext = db.ioExecutionContext
  private val subscriptionQuery = TableQuery[Subscriptions]

  def subscribe(userId: UserID, channelId: ChannelID, name: SubscriptionName = "main"): Future[Int] = {
    val query = subscriptionQuery += Subscription(userId, channelId, name)

    db.run { query }
  }

  def unsubscribe(userId: UserID, channelId: ChannelID, name: SubscriptionName = "main"): Future[Int] = {
    val query = subscriptionQuery.filter(_.userId === userId)
      .filter(_.channelId === channelId)
      .filter(_.name === name)
      .delete

    db.run { query }
  }

  def list(userId: UserID): Future[Seq[Channel]] = {
    val query = sql"""
      SELECT c.*
      FROM subscriptions AS s
      JOIN channels AS c ON c.id = s.channel_id
      WHERE s.user_id = $userId
      """

    db.run { query.as[Channel] }
  }

  class Subscriptions(tag: Tag) extends Table[Subscription](tag, "subscriptions") {
    def userId: Rep[Int] = column[Int]("user_id")
    def channelId: Rep[Int] = column[Int]("channel_id")
    def name: Rep[String] = column[String]("name")

    def * = (userId, channelId, name) <> ((Subscription.apply _).tupled, Subscription.unapply)
  }
}