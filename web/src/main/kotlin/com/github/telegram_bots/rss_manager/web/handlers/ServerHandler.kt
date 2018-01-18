package com.github.telegram_bots.rss_manager.web.handlers

import io.netty.channel.ChannelFutureListener.CLOSE
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.DefaultFileRegion
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpHeaderNames.*
import io.netty.handler.codec.http.HttpResponseStatus.*
import io.netty.handler.codec.http.HttpUtil.isKeepAlive
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedNioFile
import java.nio.channels.FileChannel.open
import java.nio.file.Files.*
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder

/**
 * Main handler. Provides chunked file downloading ability.
 */
class ServerHandler(private val path: Path) : SimpleChannelInboundHandler<FullHttpRequest>() {
    companion object {
        private const val FILE_CONTENT_TYPE = "application/rss+xml; charset=utf-8"
        private const val HTTP_CACHE_SECONDS = 60L
        private const val CHUNK_SIZE = 8192
        private val HTTP_DATE_FORMAT = DateTimeFormatterBuilder()
                .appendPattern("EEE, dd MMM yyyy HH:mm:ss zzz")
                .toFormatter()
                .withZone(ZoneId.systemDefault())
    }

    override fun channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest) {
        if (!req.decoderResult().isSuccess) return ctx.sendCode(BAD_REQUEST)
        if (req.method() != HttpMethod.GET) return ctx.sendCode(METHOD_NOT_ALLOWED)

        val params = req.getParams()
        if (params.size != 2 || params[0].toLongOrNull() == null) {
            return when {
                params[0] == "favicon.ico" -> ctx.sendCode(OK)
                else -> ctx.sendCode(BAD_REQUEST)
            }
        }

        val file = path.resolve(params.joinToString("_", postfix = ".xml"))
        if (!exists(file) || !isRegularFile(file)) return ctx.sendCode(NOT_FOUND)

        val fileLastModified = getLastModifiedTime(file).toInstant()
        if (req.getIfModifiedSince() == fileLastModified) ctx.sendNotModified()

        val keepALive = isKeepAlive(req)
        val fileSize = size(file)

        ctx.sendResponse(fileSize = fileSize, fileLastModified = fileLastModified, keepAlive = keepALive)
        ctx.sendContent(file = file, fileSize = fileSize, keepAlive = keepALive)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (ctx.channel().isActive) {
            ctx.sendCode(INTERNAL_SERVER_ERROR)
        }
        super.exceptionCaught(ctx, cause)
    }

    private fun FullHttpRequest.getParams() = uri().split("/").map(String::trim).filter(String::isNotBlank)

    private fun FullHttpRequest.getIfModifiedSince() = headers()
            .get(IF_MODIFIED_SINCE)
            ?.let { if (it.isBlank()) null else it }
            ?.let { HTTP_DATE_FORMAT.parse(it, Instant::from) }

    private fun ChannelHandlerContext.sendResponse(fileSize: Long, fileLastModified: Instant, keepAlive: Boolean) {
        val response = DefaultHttpResponse(HTTP_1_1, OK).apply {
            headers()
                    .set(CONTENT_TYPE, FILE_CONTENT_TYPE)
                    .set(CONTENT_LENGTH, fileSize)
                    .set(DATE, HTTP_DATE_FORMAT.format(Instant.now()))
                    .set(EXPIRES, HTTP_DATE_FORMAT.format(LocalDateTime.now().plusSeconds(HTTP_CACHE_SECONDS)))
                    .set(CACHE_CONTROL, "private, max-age=$HTTP_CACHE_SECONDS")
                    .set(LAST_MODIFIED, HTTP_DATE_FORMAT.format(fileLastModified))

            if (keepAlive) HttpUtil.setKeepAlive(this, true)
        }

        write(response)
    }

    private fun ChannelHandlerContext.sendContent(file: Path, fileSize: Long, keepAlive: Boolean) {
        val lastContentFuture = when {
            pipeline().get(SslHandler::class.java) == null -> {
                write(DefaultFileRegion(open(file), 0, fileSize), newProgressivePromise())
                writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
            }
            else -> writeAndFlush(HttpChunkedInput(ChunkedNioFile(open(file), 0, fileSize, CHUNK_SIZE)))
        }

        if (!keepAlive) lastContentFuture.addListener(CLOSE)
    }

    private fun ChannelHandlerContext.sendNotModified() {
        val response = DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED).apply {
            headers().set(DATE, HTTP_DATE_FORMAT.format(LocalDateTime.now()))
        }
        writeAndFlush(response).addListener(CLOSE)
    }

    private fun ChannelHandlerContext.sendCode(status: HttpResponseStatus) {
        writeAndFlush(DefaultFullHttpResponse(HTTP_1_1, status)).addListener(CLOSE)
    }
}