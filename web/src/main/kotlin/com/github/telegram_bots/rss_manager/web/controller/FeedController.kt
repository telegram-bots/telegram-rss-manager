package com.github.telegram_bots.rss_manager.web.controller

import com.github.telegram_bots.rss_manager.web.service.FeedGenerator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class FeedController(private val service: FeedGenerator) {
    @GetMapping("/{userId}/{subscriptionName}", produces = ["application/rss+xml; charset=utf-8"])
    fun get(@PathVariable userId: Long, @PathVariable subscriptionName: String): String {
        return service.generate(userId, subscriptionName).blockingGet()
    }
}