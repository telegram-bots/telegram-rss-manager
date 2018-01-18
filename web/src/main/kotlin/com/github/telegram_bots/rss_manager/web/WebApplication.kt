package com.github.telegram_bots.rss_manager.web

import java.nio.file.Paths


object WebApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        Server(1, 8691, Paths.get("/data")).create()
    }
}