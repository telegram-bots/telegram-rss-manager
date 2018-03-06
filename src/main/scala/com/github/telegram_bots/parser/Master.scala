//package com.github.telegram_bots.parser
//
//import java.util.Calendar
//
//import akka.actor.{Actor, ActorRef, Props}
//import akka.event.Logging
//import com.github.telegram_bots.parser.ChannelParser.{Start, Stop, ReceiveProcessResponse}
//import com.github.telegram_bots.parser.PostParser.Parse
//
//import scala.collection.mutable
//
//
//class Master extends Actor {
//
//  val log = Logging(context.system, this)
//  var visiting: mutable.Map[String, Boolean] = mutable.Map[String, Boolean]()
//  var visited: mutable.Map[String, Boolean] = mutable.Map[String, Boolean]()
//  val parser: ActorRef = context.actorOf(PostParser.props)
//
//  def currentTime: Int = Calendar.getInstance().get(Calendar.MILLISECOND)
//
//  override def receive: ReceiveProcessResponse = {
//    case Start(url: String) =>
//      log.info("starting with url: " + url)
//      visiting += (url -> true)
//      parser ! Parse(url)
//
//    case ReceiveProcessResponse(sourceUrl: String, urls: List[String]) =>
//      urls.foreach(link => {
//        if (!visiting.contains(link) && !visited.contains(link)) {
//          parser ! Parse(link)
//          visiting = visiting += (link -> true)
//        }
//      })
//      visiting -= sourceUrl
//      visited += sourceUrl -> true
//      if (visiting.isEmpty) self ! Stop
//
//    case Stop =>
//      log.info("stopping, processed " + visited.size + " links")
//      context.system.terminate()
//  }
//}
//
//object Master {
//  def props: Props = Props[ChannelParser].withDispatcher("fixedDispatcher1")
//
//  case object Stop
//
//  case class ReceiveProcessResponse(sourceUrl: String, urls: List[String])
//
//  case class Start(url: String)
//
//}
