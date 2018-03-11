package com.github.telegram_bots.core

import akka.actor.{Actor, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait ReactiveActor { this: Actor ⇒
  implicit val log: LoggingAdapter = Logging(context.system, this)

  implicit val system: ActorSystem = context.system

  implicit val dispatcher: ExecutionContext = context.dispatcher

  implicit val timeout: Timeout = Timeout(5.seconds)

  implicit val materializer: Materializer = ActorMaterializer.create(context)
}
