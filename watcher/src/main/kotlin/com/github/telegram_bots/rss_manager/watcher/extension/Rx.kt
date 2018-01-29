package com.github.telegram_bots.rss_manager.watcher.extension

import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

fun <T> Single<T>.randomDelay(
        delayMin: Long,
        delayMax: Long,
        unit: TimeUnit,
        scheduler: Scheduler = Schedulers.computation()
): Single<T> {
    val random = ThreadLocalRandom.current()
    return delay(random.nextLong(delayMin, delayMax), unit, scheduler)
}

class RetryWithDelay(
        private val tries: Int = -1,
        delay: Pair<Int, TimeUnit> = 0 to SECONDS,
        maxDelay: Pair<Int, TimeUnit> = -1 to SECONDS,
        private val backOff: Double = 1.0
) : Function<Flowable<out Throwable>, Flowable<Any>> {
    private val delayTime = delay.second.convert(delay.first.toLong(), SECONDS)
    private val maxDelayTime = maxDelay.second.convert(maxDelay.first.toLong(), SECONDS)
    private var triesCount: Long = 0
    private var factor: Long = 1

    override fun apply(attempts: Flowable<out Throwable>): Flowable<Any> {
        return attempts
                .flatMap { throwable ->
                    when {
                        tries == -1 || triesCount < tries -> {
                            val nextDelayTime = when (triesCount) {
                                0L -> delayTime
                                else -> (delayTime * factor * backOff).toLong()
                            }
                            val time = when (maxDelayTime) {
                                in 1 until nextDelayTime -> maxDelayTime
                                else -> nextDelayTime
                            }

                            if (triesCount++ > 0) factor *= backOff.toLong()
                            Flowable.timer(time, SECONDS)
                        }
                        else -> Flowable.error<Any>(throwable)
                    }
                }
    }
}