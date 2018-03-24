package com.github.telegram_bots.telegram_rss_manager.web.component

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}

import scala.concurrent.{ExecutionContextExecutor, Promise}

class ImperativeRequestContext(ctx: RequestContext, promise: Promise[RouteResult]) {
  private implicit val executionContext: ExecutionContextExecutor = ctx.executionContext

  def complete(obj: ToResponseMarshallable): Unit = ctx.complete(obj).onComplete(promise.complete)

  def fail(error: Throwable): Unit = ctx.fail(error).onComplete(promise.complete)
}

trait ImperativeRequestDirective {
  def imperativelyComplete(inner: ImperativeRequestContext => Unit): Route = { ctx: RequestContext =>
    val p = Promise[RouteResult]()
    inner(new ImperativeRequestContext(ctx, p))
    p.future
  }
}