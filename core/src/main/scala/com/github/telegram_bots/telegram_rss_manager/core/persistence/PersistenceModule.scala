package com.github.telegram_bots.telegram_rss_manager.core.persistence

import java.net.URI

import com.github.telegram_bots.telegram_rss_manager.core.config.ConfigModule
import com.softwaremill.macwire._
import com.typesafe.config.ConfigValueFactory.fromAnyRef
import slick.jdbc.PostgresProfile.api._

trait PersistenceModule { this: ConfigModule =>
  lazy val db: Database = {
    val oldConf = config.getConfig("db")
    val url = new URI(oldConf.getString("url"))
    val (user, password) = Option(url.getUserInfo)
      .map(_.split(":"))
      .map(arr => (arr.headOption, arr.lastOption))
      .getOrElse((None, None))

    val newConf = oldConf
      .withValue("url", fromAnyRef(s"jdbc:${url.getScheme}://${url.getHost}:${url.getPort}${url.getPath}"))
      .withValue("user", fromAnyRef(user.getOrElse("")))
      .withValue("password", fromAnyRef(password.getOrElse("")))

    Database.forConfig("", newConf)
  }

  lazy val channelRepository: ChannelRepository = wire[ChannelRepository]

  lazy val postRepository: PostRepository = wire[PostRepository]

  lazy val userRepository: UserRepository = wire[UserRepository]

  lazy val subscriptionRepository: SubscriptionRepository = wire[SubscriptionRepository]
}
