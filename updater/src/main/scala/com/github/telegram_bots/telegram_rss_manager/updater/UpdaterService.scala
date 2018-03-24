package com.github.telegram_bots.telegram_rss_manager.updater

import com.github.telegram_bots.telegram_rss_manager.core.config.ConfigModule
import com.github.telegram_bots.telegram_rss_manager.core.persistence.PersistenceModule
import com.github.telegram_bots.telegram_rss_manager.updater.actor.ActorModule

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object UpdaterService extends App
  with ConfigModule
  with PersistenceModule
  with ActorModule
{
  val masterInstance = master
  val workerInstances = (1 to config.getInt("akka.workers")).map(_ => createWorker)

  Await.result(system.whenTerminated, Duration.Inf)
}