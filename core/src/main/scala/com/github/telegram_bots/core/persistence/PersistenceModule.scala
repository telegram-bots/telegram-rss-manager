package com.github.telegram_bots.core.persistence

import com.github.telegram_bots.core.config.ConfigModule
import com.softwaremill.macwire._
import slick.jdbc.PostgresProfile.api._

trait PersistenceModule { this: ConfigModule =>
  lazy val db: Database = Database.forConfig("db", config)

  lazy val channelRepository: ChannelRepository = wire[ChannelRepository]

  lazy val postRepository: PostRepository = wire[PostRepository]

  lazy val userRepository: UserRepository = wire[UserRepository]

  lazy val subscriptionRepository: SubscriptionRepository = wire[SubscriptionRepository]
}
