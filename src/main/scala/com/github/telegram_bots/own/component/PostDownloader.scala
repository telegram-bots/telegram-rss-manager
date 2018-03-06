package com.github.telegram_bots.own.component

import com.github.telegram_bots.own.domain.Types.Proxy
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import com.github.telegram_bots.own.Implicits._
import scala.collection.JavaConverters._

import scala.util.Try

object PostDownloader {
  def download(channelUrl: String, postId: Int, proxy: Proxy): Try[Option[Document]] = {
    val url = formatUrl(channelUrl, postId)
    val connection = connect(url, proxy)
    val data = connection.get()?

    data
      .map(it => {
        val error = it.select(".tgme_widget_message_error").text().trim()

        error match {
          case e if e == "Post not found" => Try(Option.empty)
          case e if e.contains("Channel with username") => Try(throw new RuntimeException(e))
          case _ => Try(Option(it))
        }
      })
      .get
  }

  private def formatUrl(channelUrl: String, postId: Int) = s"https://t.me/$channelUrl/$postId?embed=1&single=1"

  private def connect(url: String, proxy: Proxy) = Jsoup.connect(url)
    .proxy(proxy.host, proxy.port)
    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
    .headers(Map(
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
      "Accept-Encoding" -> "gzip, deflate, br",
      "Accept-Language" -> "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
      "Cache-Control" -> "max-age=0",
      "Connection" -> "keep-alive",
      "Host" -> "t.me",
      "DNT" -> "1",
      "Upgrade-Insecure-Requests" -> "1"
    ).asJava)
}
