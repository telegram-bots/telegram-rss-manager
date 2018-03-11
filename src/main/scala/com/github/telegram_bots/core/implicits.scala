package com.github.telegram_bots.core

import akka.http.scaladsl.coding.{Deflate, Gzip, NoCoding}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.stream.scaladsl.Source

import scala.collection.SortedMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object implicits {
  implicit class ConvertToOption[T](t: T) { def ? = Option(t) }

  implicit class ToSortedMap[A,B](tuples: TraversableOnce[(A, B)])(implicit ordering: Ordering[A]) {
    def toSortedMap = SortedMap(tuples.toSeq: _*)
  }

  implicit class ExtendedAnyRef[T <: AnyRef](underlying: T) {
    def let[T2](func: (T) => T2): T2 = func(underlying)

    def also(func: T => Unit): T = {
      func(underlying)
      underlying
    }
  }

  implicit class ExtendedString(str: String) {
    def optionIfBlank: Option[String] = {
      val trimmed = str.trim
      if (trimmed.isEmpty) Option.empty
      else Option(trimmed)
    }
  }

  implicit class ExtendedHttpResponse(response: HttpResponse) {
    def getBody: Source[String, Any] = response.entity.dataBytes.map(_.utf8String)

    def decode: HttpResponse = {
      val decoder = response.encoding match {
        case HttpEncodings.gzip ⇒ Gzip
        case HttpEncodings.deflate ⇒ Deflate
        case HttpEncodings.identity ⇒ NoCoding
      }

      decoder.decodeMessage(response)
    }
  }

  implicit class ExtendedFuture[T](future: Future[T])(implicit executionContext: ExecutionContext) {
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
