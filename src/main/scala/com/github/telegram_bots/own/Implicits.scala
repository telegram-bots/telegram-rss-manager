package com.github.telegram_bots.own

import scala.collection.SortedMap

object Implicits {
  implicit class ConvertToOption[T](t: T) { def ? = Option(t) }

  implicit class StringOptionIfBlank(str: String) {
    def optionIfBlank: Option[String] = {
      val trimmed = str.trim
      if (trimmed.isEmpty) Option.empty
      else Option(trimmed)
    }
  }

  implicit class ToSortedMap[A,B](tuples: TraversableOnce[(A, B)])(implicit ordering: Ordering[A]) {
    def toSortedMap = SortedMap(tuples.toSeq: _*)
  }
}
