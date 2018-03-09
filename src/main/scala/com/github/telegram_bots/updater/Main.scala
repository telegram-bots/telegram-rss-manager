package com.github.telegram_bots.updater

import akka.actor.ActorSystem
import com.github.telegram_bots.updater.actor.Master
import com.github.telegram_bots.updater.actor.Master.Start

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {
  val system = ActorSystem("updater")
  val masterActor = system.actorOf(Master.props)

  masterActor ! Start
  Await.result(system.whenTerminated, Duration.Inf)
}