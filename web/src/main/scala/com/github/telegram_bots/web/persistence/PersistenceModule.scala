package com.github.telegram_bots.web.persistence

import com.github.telegram_bots.core.config.ConfigModule
import com.softwaremill.macwire._
import slick.jdbc.PostgresProfile.api._

trait PersistenceModule { this: ConfigModule =>
  lazy val db: Database = Database.forConfig("db", config)

  lazy val postRepository: PostRepository = wire[PostRepository]
}
