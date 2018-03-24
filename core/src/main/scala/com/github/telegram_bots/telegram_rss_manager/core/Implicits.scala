package com.github.telegram_bots.telegram_rss_manager.core

import akka.http.scaladsl.coding.{Deflate, Gzip, NoCoding}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.util.Timeout

import scala.collection.SortedMap
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object Implicits {
  implicit class ToSortedMap[A,B](tuples: TraversableOnce[(A, B)])(implicit ordering: Ordering[A]) {
    def toSortedMap = SortedMap(tuples.toSeq: _*)
  }

  implicit class ExtendedAnyRef[T <: AnyRef](underlying: T) {
    def ? = Option(underlying)

    def let[U](func: (T) => U): U = func(underlying)

    def also(func: T => Unit): T = {
      func(underlying)
      underlying
    }
  }

  implicit class ExtendedString(str: String) {
    def optionIfBlank: Option[String] = Option(str.trim).filter(!_.isEmpty)
  }

  implicit class ExtendedHttpResponse(response: HttpResponse) {
    def decode: HttpResponse = {
      val decoder = response.encoding match {
        case HttpEncodings.gzip => Gzip
        case HttpEncodings.deflate => Deflate
        case _ => NoCoding
      }

      decoder.decodeMessage(response)
    }
  }

  implicit class ExtendedFuture[T](future: Future[T])(implicit executionContext: ExecutionContext, timeout: Timeout) {
    def get: T = Await.result(future, timeout.duration)

    def doOnNext(callback: T => Unit): Future[T] = future.map(e => { callback(e); e })

    def doOnComplete(callback: T => Unit): Future[T] = {
      future.onComplete {
        case Success(element) => callback(element)
        case _ =>
      }
      future
    }

    def doOnError(callback: Throwable => Unit): Future[T] = {
      future.onComplete {
        case Failure(e) => callback(e)
        case _ =>
      }
      future
    }
  }
}
