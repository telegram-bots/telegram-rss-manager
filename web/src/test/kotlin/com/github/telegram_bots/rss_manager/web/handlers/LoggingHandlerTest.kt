package com.github.telegram_bots.rss_manager.web.handlers

import ch.qos.logback.classic.Level.INFO
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.specs.StringSpec
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import mu.KLogger
import org.slf4j.Logger
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory


class LoggingHandlerTest : StringSpec({
    "correctly logs requests" {
        val (logger, events) = CustomLogger.get()
        val channel = EmbeddedChannel(LoggingHandler(logger))
        channel.writeOneInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "test1"))
        channel.writeOneInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "test2"))
        channel.writeOneInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "test3"))
        channel.finish()

        events.size shouldBe 3
        events[0] should {
            it.level shouldBe INFO
            it.message.contains("GET") shouldEqual true
            it.message.contains("test1") shouldEqual true
        }
        events[1] should {
            it.level shouldBe INFO
            it.message.contains("POST") shouldEqual true
            it.message.contains("test2") shouldEqual true
        }
        events[2] should {
            it.level shouldBe INFO
            it.message.contains("PUT") shouldEqual true
            it.message.contains("test3") shouldEqual true
        }
    }
})

class CustomLogger(private val logger: Logger) : Logger by logger, KLogger {
    companion object {
        fun get(): Pair<KLogger, List<ILoggingEvent>> {
            val logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
            val events = mutableListOf<ILoggingEvent>()
            val appender = object : AppenderBase<ILoggingEvent>() {
                override fun append(e: ILoggingEvent) {
                    events.add(e)
                }
            }
            appender.start()
            logger.addAppender(appender)

            return CustomLogger(logger) to events
        }
    }

    override val underlyingLogger: Logger
        get() = logger
}