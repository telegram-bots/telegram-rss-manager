package com.github.telegram_bots.rss_manager.watcher.extensions

import com.github.telegram_bots.rss_manager.watcher.extension.randomDelay
import io.kotlintest.matchers.should
import io.kotlintest.specs.FeatureSpec
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import java.util.concurrent.TimeUnit

class SinglesTest : FeatureSpec({
    feature("randomDelay", {
        scenario("should delay correctly", {
            val minDelay = 2L
            val maxDelay = 10L
            val timeUnit = TimeUnit.SECONDS
            val testSub = TestObserver<String>()

            Single.just("wtf")
                    .randomDelay(minDelay, maxDelay, timeUnit)
                    .subscribe(testSub)

            val completedIn = measure({ testSub.awaitTerminalEvent() }, timeUnit)

            should { (minDelay..maxDelay).contains(completedIn) }
        })
    })
})

fun measure(block: () -> Unit, timeUnit: TimeUnit): Long {
    val started = System.currentTimeMillis()
    block()
    return timeUnit.convert(System.currentTimeMillis() - started, TimeUnit.MILLISECONDS)
}