package com.github.telegram_bots.rss_manager.web.service

import com.github.telegram_bots.rss_manager.web.config.properties.RabbitSinkProperties
import com.github.telegram_bots.rss_manager.web.domain.Message
import com.rabbitmq.client.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Emitter
import io.reactivex.Flowable
import mu.KLogging
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.stereotype.Service


@EnableBinding
@Service
class DynamicSink(
        private val rabbitClient: RabbitTemplate,
        private val props: RabbitSinkProperties
) {
    companion object : KLogging()

    fun consume(routingKey: String): Flowable<Message> = Flowable.create(
            { emitter ->
                val queue = "${props.destination}.$routingKey"

                try {
                    rabbitClient.execute { channel ->
                        channel.basicQos(1)

//                        val consumer = FlowableConsumer(emitter.serialize(), channel)
                        while (channel.messageCount(queue) > 0 && !emitter.isCancelled) {
                            val msg = channel.basicGet(queue, false)
                            emitter.onNext(Message(msg.envelope.deliveryTag, msg.body))
//                            channel.basicConsume(queue,false, consumer)
                        }
                    }
                } finally {
                    emitter.onComplete()
                }
            },
            BackpressureStrategy.BUFFER
    )

    fun acknowledge(tag: Long) {
        rabbitClient.execute { channel -> channel.basicAck(tag, true) }
    }

    fun unacknowledge(tag: Long) {
        rabbitClient.execute { channel -> channel.basicNack(tag, true, true) }
    }

    class FlowableConsumer(private val emitter: Emitter<Message>, channel: Channel) : DefaultConsumer(channel) {
        override fun handleDelivery(tag: String, envelope: Envelope, props: AMQP.BasicProperties, body: ByteArray) {
            emitter.onNext(Message(envelope.deliveryTag, body))
        }

        override fun handleShutdownSignal(tag: String, sig: ShutdownSignalException) {
            channel.close()
            emitter.onComplete()
        }

        override fun handleCancel(tag: String) {
            channel.close()
            emitter.onComplete()
        }
    }
}