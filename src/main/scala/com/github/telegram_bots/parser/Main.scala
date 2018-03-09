package com.github.telegram_bots.parser

import akka.actor.ActorSystem
import com.github.telegram_bots.parser.actor.Master
import com.github.telegram_bots.parser.actor.Master.Start

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {
  val system = ActorSystem("parser")
  val masterActor = system.actorOf(Master.props)

  time {
    masterActor ! Start

    Await.result(system.whenTerminated, 10 minutes)
  }

  def time[R](block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
    result
  }
}