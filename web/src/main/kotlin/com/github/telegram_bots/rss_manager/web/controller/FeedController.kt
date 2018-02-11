package com.github.telegram_bots.rss_manager.web.controller

import com.github.telegram_bots.rss_manager.web.service.FeedRetriever
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class FeedController(private val service: FeedRetriever) {
    @GetMapping("/{userId}/{subscriptionName}", produces = ["application/rss+xml; charset=utf-8"])
    fun get(@PathVariable userId: Long, @PathVariable subscriptionName: String): String {
        return service.retrieve(userId, subscriptionName).blockingGet()
    }
}