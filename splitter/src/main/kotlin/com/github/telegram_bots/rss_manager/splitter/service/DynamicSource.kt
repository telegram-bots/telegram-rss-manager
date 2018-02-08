package com.github.telegram_bots.rss_manager.splitter.service

import com.github.telegram_bots.rss_manager.splitter.config.properties.RabbitSourceProperties
import com.github.telegram_bots.rss_manager.splitter.domain.Message
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.stereotype.Service


@EnableBinding
@Service
class DynamicSource(
        private val rabbitClient: RabbitTemplate,
        private val rabbitAdmin: AmqpAdmin,
        private val props: RabbitSourceProperties
) {
    fun send(message: Message): Boolean {
        bindExchangeAndQueue(message.id)

        rabbitClient.convertAndSend(
                props.destination,
                message.id,
                message.content,
                { it -> it.apply { messageProperties.contentType = props.contentType } }
        )

        return true
    }

    private fun bindExchangeAndQueue(messageId: String) {
        val exchange = TopicExchange(props.destination, true, false)
        val queue = QueueBuilder.durable("${props.destination}.$messageId")
                .withArguments(props.producer)
                .build()

        rabbitAdmin.declareExchange(exchange)
        rabbitAdmin.declareQueue(queue)
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(messageId))
    }
}