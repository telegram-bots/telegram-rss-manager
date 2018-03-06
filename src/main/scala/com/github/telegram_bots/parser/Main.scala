//package com.github.telegram_bots.parser
//
//import akka.actor.ActorSystem
//import com.github.telegram_bots.parser.ChannelParser.Start
//import com.typesafe.config.ConfigFactory
//
//import scala.concurrent.Await
//import scala.concurrent.duration._
//import scala.language.postfixOps
//
//object Main extends App {
//  val system = ActorSystem.create("mypool", ConfigFactory.load().getConfig("MyDispatcherExample"))
//  val master = system.actorOf(ChannelParser.props)
//
//  time {
//    master ! Start("https://www.rabbitmq.com/getstarted.html")
//    Await.result(system.whenTerminated, 10 minutes)
//  }
//
//  def time[R](block: => R): R = {
//    val t0 = System.currentTimeMillis()
//    val result = block
//    val t1 = System.currentTimeMillis()
//    println("Elapsed time: " + (t1 - t0) + "ms")
//    result
//  }
//}