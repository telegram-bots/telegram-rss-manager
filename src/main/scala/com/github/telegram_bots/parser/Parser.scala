//package com.github.telegram_bots.parser
//
//import akka.actor.{Actor, Props}
//import akka.event.Logging
//import akka.routing.{RoundRobinPool, SmallestMailboxPool, SmallestMailboxRoutingLogic}
//import org.jsoup.Jsoup
//import com.github.telegram_bots.parser.ChannelParser.ReceiveProcessResponse
//import com.github.telegram_bots.parser.PostParser.Parse
//
//import scala.collection.JavaConverters._
//import scala.language.postfixOps
//
//
//class Parser extends Actor {
//  val baseUrl = "https://www.rabbitmq.com"
//  val log = Logging(context.system, this)
//
//  def receive: ReceiveProcessResponse = {
//    case Parse(url) =>
//      val links = getLinks(url)
//      sender() ! ReceiveProcessResponse(url, links)
//  }
//
//  def getLinks(url: String): List[String] = {
//    val response = Jsoup.connect(url).ignoreContentType(true).execute()
//    val doc = response.parse()
//    doc.getElementsByTag("a")
//      .asScala
//      .map(e => e.attr("href"))
//      .filter(link => link.startsWith("/") && link.endsWith(".html"))
//      .take(5)
//      .map(link => baseUrl + link)
//      .toList
//  }
//
//}
//
//object Parser {
//  def props: Props = Props[PostParser].withDispatcher("fixedDispatcher20").withRouter(new SmallestMailboxPool(20))
//  case class Parse(url: String)
//}
