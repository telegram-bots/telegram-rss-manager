package com.github.telegram_bots.own.component

import java.time.LocalDateTime

import com.github.telegram_bots.own.domain.PostType._
import org.jsoup.nodes.Document
import com.github.telegram_bots.own.Implicits._
import scala.collection.JavaConverters._

object PostDataExtractor {
  def getType(doc: Document): PostType = doc match {
    case d if !d.select("#sticker_image").isEmpty => STICKER
    case d if !d.select(".tgme_widget_message_photo").isEmpty => IMAGE
    case d if !d.select(".tgme_widget_message_location").isEmpty => GEO
    case d if !d.select(".tgme_widget_message_document_icon").isEmpty => FILE
    case d if !d.select(".tgme_widget_message_contact").isEmpty => CONTACT
    case d if !d.select(".tgme_widget_message_document_icon.audio, audio.tgme_widget_message_voice").isEmpty => AUDIO
    case d if !d.select("video.tgme_widget_message_video, video.tgme_widget_message_roundvideo").isEmpty => VIDEO
    case _ => TEXT
  }

  def getText(doc: Document, `type`: PostType): String = `type` match {
    case GEO => doc.select(".tgme_widget_message_location_info").asScala.lastOption.map(_.html).getOrElse("")
    case CONTACT => doc.select(".tgme_widget_message_contact").asScala.lastOption.map(_.html).getOrElse("")
    case _ => doc.select(".tgme_widget_message_text").asScala.lastOption.map(_.html).getOrElse("")
  }

  def getId(doc: Document): Int = doc.select(".tgme_widget_message_date").asScala
    .lastOption
    .map(_.attr("href"))
    .map(_.split("/"))
    .flatMap(_.lastOption)
    .map(_.split("\\?"))
    .flatMap(_.headOption)
    .map(_.toInt)
    .get

  def getURLAndName(doc: Document): (String, String) = doc.select(".tgme_widget_message_owner_name").asScala
    .lastOption
    .map(it => {
      val name = it.select("span").asScala.lastOption.map(_.text).get
      val url = it.attr("href").split("/").lastOption.get

      (url, name)
    })
    .get

  def getAuthor(doc: Document): Option[String] = (doc.select(".tgme_widget_message_from_author").text()?)
    .flatMap(_.optionIfBlank)

  def getDate(doc: Document): LocalDateTime = doc.getElementsByTag("time").asScala
    .lastOption
    .map(_.attr("datetime"))
    .map(_.split("\\+"))
    .flatMap(_.headOption)
    .map(LocalDateTime.parse(_))
    .get

  def getFileURL(doc: Document, `type`: PostType): Option[String] = `type` match {
    case GEO => doc.select(".tgme_widget_message_location").asScala
      .headOption
      .map(_.attr("style"))
      .map(_.replace("background-image:url('", ""))
      .map(_.replace("')", ""))
    case AUDIO => doc.select("audio.tgme_widget_message_voice")
      .attr("src")
      .optionIfBlank
    case IMAGE => doc.select(".tgme_widget_message_photo_wrap").asScala
      .headOption
      .map(_.attr("style"))
      .map(_.replace("background-image:url('", ""))
      .map(_.replace("')", ""))
    case STICKER => doc.select("#sticker_image").asScala
      .headOption
      .map(_.attr("style"))
      .map(_.replace("background-image:url('", ""))
      .map(_.replace("')", ""))
    case VIDEO => doc.select("video.tgme_widget_message_video, video.tgme_widget_message_roundvideo")
      .attr("src")
      .optionIfBlank
    case CONTACT => doc.select(".tgme_widget_message_contact_wrap > .tgme_widget_message_user_photo > img")
      .attr("src")
      .optionIfBlank
    case _ => Option.empty
  }
}
