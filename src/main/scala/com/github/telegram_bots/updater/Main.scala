package com.github.telegram_bots.updater

import com.github.telegram_bots.core.config.ConfigModule
import com.github.telegram_bots.updater.actor.ActorModule
import com.github.telegram_bots.updater.actor.Master.Start
import com.github.telegram_bots.updater.persistence.PersistenceModule

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App
  with ConfigModule
  with PersistenceModule
  with ActorModule
{
  master ! Start

  Await.result(system.whenTerminated, Duration.Inf)
}