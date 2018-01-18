package com.github.telegram_bots.rss_manager.web.handlers

import io.kotlintest.specs.StringSpec
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import java.nio.file.Paths

class ServerHandlerTest : StringSpec({
    "should return correct response" {
        val channel = EmbeddedChannel(ServerHandler(path = Paths.get("path")))

        channel.writeOneInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "test1"))
        channel.writeOneInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "test2"))
        channel.writeOneInbound(DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "test3"))

        channel.finish()
    }
})