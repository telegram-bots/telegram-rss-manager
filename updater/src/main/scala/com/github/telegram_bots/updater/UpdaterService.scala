package com.github.telegram_bots.updater

import com.github.telegram_bots.core.config.ConfigModule
import com.github.telegram_bots.updater.actor.ActorModule
import com.github.telegram_bots.updater.persistence.PersistenceModule

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object UpdaterService extends App
  with ConfigModule
  with PersistenceModule
  with ActorModule
{
  val masterInstance = master
  val workers = for (_ <- 1 to 5) yield createWorker

  Await.result(system.whenTerminated, Duration.Inf)
}