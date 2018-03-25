package com.github.telegram_bots.telegram_rss_manager.bot.component

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.telegram_bots.telegram_rss_manager.bot.config.Properties
import com.typesafe.scalalogging.Logger
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.models.UpdateType.UpdateType
import info.mukel.telegrambot4s.models._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait Resolver extends BotBase with AkkaImplicits with BotExecutionContext {
  val self: Resolver = this
  val logger: Logger
  val props: Properties

  private lazy val delegate: BotBase = props.mode match {
    case "polling" => new Polling {
      override implicit val system: ActorSystem = self.system
      override implicit val materializer: ActorMaterializer = self.materializer
      override implicit val executionContext: ExecutionContext = self.executionContext
      override protected val logger: Logger = self.logger
      override val client: RequestHandler = self.client

      override def token: String = props.token

      override def pollingInterval: Int = props.pollingInterval

      override def request: RequestHandler = self.request

      override def allowedUpdates: Option[Seq[UpdateType]] = self.allowedUpdates

      override def receiveUpdate(u: Update): Unit = self.receiveUpdate(u)

      override def receiveMessage(message: Message): Unit =
        self.receiveMessage(message)

      override def receiveEditedMessage(editedMessage: Message): Unit =
        self.receiveEditedMessage(editedMessage)

      override def receiveChannelPost(message: Message): Unit =
        self.receiveChannelPost(message)

      override def receiveEditedChannelPost(message: Message): Unit =
        self.receiveEditedChannelPost(message)

      override def receiveInlineQuery(inlineQuery: InlineQuery): Unit =
        self.receiveInlineQuery(inlineQuery)

      override def receiveChosenInlineResult(chosenInlineResult: ChosenInlineResult): Unit =
        self.receiveChosenInlineResult(chosenInlineResult)

      override def receiveCallbackQuery(callbackQuery: CallbackQuery): Unit =
        self.receiveCallbackQuery(callbackQuery)

      override def receiveShippingQuery(shippingQuery: ShippingQuery): Unit =
        self.receiveShippingQuery(shippingQuery)

      override def receivePreCheckoutQuery(preCheckoutQuery: PreCheckoutQuery): Unit =
        self.receivePreCheckoutQuery(preCheckoutQuery)
    }

    case "webhook" => new Webhook {
      override implicit val system: ActorSystem = self.system
      override implicit val materializer: ActorMaterializer = self.materializer
      override implicit val executionContext: ExecutionContext = self.executionContext
      override protected val logger: Logger = self.logger
      override val client: RequestHandler = self.client
      override val webhookUrl: String = props.webHookUrl
      override val interfaceIp: String = props.ip
      override val port: Int = props.port

      override def token: String = props.token

      override def certificate: Option[InputFile] =
        if (props.certificatePath.isEmpty) None
        else Some(InputFile(props.certificatePath))

      override def request: RequestHandler = self.request

      override def allowedUpdates: Option[Seq[UpdateType]] = self.allowedUpdates

      override def receiveUpdate(u: Update): Unit = self.receiveUpdate(u)

      override def receiveMessage(message: Message): Unit =
        self.receiveMessage(message)

      override def receiveEditedMessage(editedMessage: Message): Unit =
        self.receiveEditedMessage(editedMessage)

      override def receiveChannelPost(message: Message): Unit =
        self.receiveChannelPost(message)

      override def receiveEditedChannelPost(message: Message): Unit =
        self.receiveEditedChannelPost(message)

      override def receiveInlineQuery(inlineQuery: InlineQuery): Unit =
        self.receiveInlineQuery(inlineQuery)

      override def receiveChosenInlineResult(chosenInlineResult: ChosenInlineResult): Unit =
        self.receiveChosenInlineResult(chosenInlineResult)

      override def receiveCallbackQuery(callbackQuery: CallbackQuery): Unit =
        self.receiveCallbackQuery(callbackQuery)

      override def receiveShippingQuery(shippingQuery: ShippingQuery): Unit =
        self.receiveShippingQuery(shippingQuery)

      override def receivePreCheckoutQuery(preCheckoutQuery: PreCheckoutQuery): Unit =
        self.receivePreCheckoutQuery(preCheckoutQuery)
    }

    case _ => throw new IllegalArgumentException(s"Unknown mode: ${props.mode}")
  }

  override def token: String = props.token

  override def run(): Unit = {
    logger.info(s"Starting bot in ${props.mode} mode...")

    sys.addShutdownHook {
      Await.ready(shutdown(), 30.seconds)
    }

    delegate.run()
  }

  override def shutdown(): Future[Unit] = delegate.shutdown()
}
