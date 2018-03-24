package com.github.telegram_bots.telegram_rss_manager.web

import java.io.StringWriter
import java.nio.charset.{Charset, StandardCharsets}

import akka.util.ByteString

import scala.xml.Elem
import scala.xml.XML.write

object Implicits {
  implicit class ExtendedElem(elem: Elem) {
    def toByteString(charset: Charset = StandardCharsets.UTF_8): ByteString = {
      val writer = new StringWriter()
      write(writer, elem, charset.name(), xmlDecl = true, null)
      ByteString(writer.toString)
    }
  }
}
