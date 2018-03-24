package com.github.telegram_bots.telegram_rss_manager.web.component

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.{ISO_INSTANT, RFC_1123_DATE_TIME}

import scala.xml.Elem

object RSSFeedGenerator {
  def generate(channel: FeedChannel, entries: Iterable[FeedEntry]): Elem = {
    <rss version="2.0">
      <channel>
        <title>{channel.title}</title>
        <link>{channel.link}</link>
        <description>{channel.description}</description>
        <managingEditor>{channel.managingEditor}</managingEditor>
        <webMaster>{channel.webMaster}</webMaster>
        <pubDate>{channel.date.format(RFC_1123_DATE_TIME)}</pubDate>
        <dc:date>{channel.date.format(ISO_INSTANT)}</dc:date>
        {
          for (entry <- entries) yield {
            <item>
              <title>{entry.title}</title>
              <link>{entry.link}</link>
              <guid>{entry.link}</guid>
              <description>{entry.description}</description>
              <pubDate>{entry.date.format(RFC_1123_DATE_TIME)}</pubDate>
              <dc:date>{entry.date.format(ISO_INSTANT)}</dc:date>
            </item>
          }
        }
      </channel>
    </rss>
  }

  case class FeedChannel(
    title: String,
    link: String,
    description: String,
    managingEditor: String,
    webMaster: String,
    date: ZonedDateTime
  )

  case class FeedEntry(
    title: String,
    link: String,
    description: String,
    date: ZonedDateTime
  )
}
