package com.github.telegram_bots.telegram_rss_manager.core.persistence

import com.github.telegram_bots.telegram_rss_manager.core.domain.User
import com.github.telegram_bots.telegram_rss_manager.core.domain.User.TelegramID
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class UserRepository(db: Database) {
  private implicit val executionContext: ExecutionContext = db.ioExecutionContext
  private val userQuery = TableQuery[Users]

  def find(chatId: TelegramID): Future[Option[User]] = {
    val query = userQuery.filter(_.telegramId === chatId)

    db.run { query.result.headOption }
  }

  def getOrCreate(chatId: TelegramID): Future[User] = {
    val query = for {
      existing <- userQuery.filter(_.telegramId === chatId).result.headOption
      row = existing getOrElse User(0, chatId)
      result <- (userQuery returning userQuery).insertOrUpdate(row).map(_.getOrElse(row))
    } yield result

    db.run { query.transactionally }
  }

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def telegramId: Rep[Long] = column[Long]("telegram_id")

    def * = (id, telegramId) <> ((User.apply _).tupled, User.unapply)
  }
}
