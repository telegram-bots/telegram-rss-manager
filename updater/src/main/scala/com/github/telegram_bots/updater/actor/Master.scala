package com.github.telegram_bots.updater.actor

import akka.actor.{Actor, ActorRef, Terminated}
import com.github.telegram_bots.core.actor.ReactiveActor
import com.github.telegram_bots.updater.actor.Master.WorkAvailable
import com.github.telegram_bots.updater.actor.Worker.Work
import com.softwaremill.tagging.@@

class Master(channelStorage: ActorRef @@ ChannelStorage) extends Actor with ReactiveActor {
  var workers: Set[ActorRef] = Set()
  var initialized = false

  override def preStart(): Unit = channelStorage ! ChannelStorage.UnlockAllRequest

  override def receive: Receive = {
    case ChannelStorage.UnlockAllResponse =>
      log.info("Master ready")
      initialized = true
      workers foreach (_ ! WorkAvailable)

    case Worker.Register(worker) =>
      log.info(s"Worker $worker registered")
      context.watch(worker)
      workers += worker

    case Terminated(worker) =>
      log.warning(s"Worker $worker died")
      context.unwatch(worker)
      workers -= worker

    case Worker.RequestWork =>
      log.debug(s"Worker $sender requested work")
      if (initialized) sender ! Work
  }
}

object Master {
  case object WorkAvailable
}