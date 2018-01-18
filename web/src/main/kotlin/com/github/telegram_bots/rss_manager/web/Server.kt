package com.github.telegram_bots.rss_manager.web

import com.github.telegram_bots.rss_manager.web.handlers.LoggingHandler
import com.github.telegram_bots.rss_manager.web.handlers.ServerHandler
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.stream.ChunkedWriteHandler
import mu.KLogging
import java.nio.file.Path

/**
 * Server implementation
 */
class Server(private val threads: Int, private val port: Int, private val path: Path) {
    companion object : KLogging()

    fun create() {
        val bossGroup = NioEventLoopGroup(threads)
        val workerGroup = NioEventLoopGroup()

        try {
            ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(ch: SocketChannel) {
                            ch.pipeline()
                                    .addLast(HttpServerCodec())
                                    .addLast(HttpObjectAggregator(65536))
                                    .addLast(ChunkedWriteHandler())
                //                    .addLast(HttpContentCompressor()) // Why :(
                                    .addLast(LoggingHandler(logger))
                                    .addLast(ServerHandler(path = path))
                        }
                    })
                    .bind(port)
                    .sync()
                    .channel()
                    .closeFuture()
                    .sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}