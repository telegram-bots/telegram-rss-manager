package com.github.telegram_bots.core.persistence

import com.github.telegram_bots.core.domain.User
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class UserRepository(db: Database) {
  private implicit val executionContext: ExecutionContext = db.ioExecutionContext
  private val userQuery: TableQuery[Users] = TableQuery[Users]

  def find(telegramId: Long): Future[Option[User]] = {
    val query = userQuery.filter(_.telegramId === telegramId)

    db.run { query.result.headOption }
  }

  def getOrCreate(telegramId: Long): Future[User] = {
    val query = for {
      existing <- userQuery.filter(_.telegramId === telegramId).result.headOption
      row = existing getOrElse User(0, telegramId)
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
