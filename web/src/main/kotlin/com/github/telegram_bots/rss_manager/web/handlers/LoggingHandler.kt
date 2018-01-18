package com.github.telegram_bots.rss_manager.web.handlers

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpRequest
import mu.KLogger

/**
 * Handler provides logging of all requests and errors
 */
class LoggingHandler(private val logger: KLogger) : ChannelDuplexHandler() {
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is HttpRequest) logger.info { msg }
        super.channelRead(ctx, msg)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.error("", cause)
        super.exceptionCaught(ctx, cause)
    }
}