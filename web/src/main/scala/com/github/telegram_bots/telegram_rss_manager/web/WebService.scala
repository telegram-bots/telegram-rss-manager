package com.github.telegram_bots.telegram_rss_manager.web

import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.github.telegram_bots.telegram_rss_manager.core.config.ConfigModule
import com.github.telegram_bots.telegram_rss_manager.core.persistence.PersistenceModule
import com.github.telegram_bots.telegram_rss_manager.web.actor.ActorModule
import com.github.telegram_bots.telegram_rss_manager.web.actor.FeedResponder.GetRequest
import com.github.telegram_bots.telegram_rss_manager.web.component.ImperativeRequestDirective

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.language.postfixOps

object WebService extends App
  with ConfigModule
  with PersistenceModule
  with ActorModule
  with ImperativeRequestDirective
{
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val dispatcher: ExecutionContextExecutor = system.dispatcher

  val route =
    path(LongNumber / Segment) { (telegramID, subscriptionName) =>
      get {
        encodeResponseWith(Gzip) {
          imperativelyComplete { ctx =>
            createFeedResponder ! GetRequest(ctx, telegramID, subscriptionName, 500)
          }
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
