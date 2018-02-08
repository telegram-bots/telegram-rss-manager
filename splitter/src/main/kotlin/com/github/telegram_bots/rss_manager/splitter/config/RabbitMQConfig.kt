package com.github.telegram_bots.rss_manager.splitter.config

import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.boot.autoconfigure.amqp.RabbitProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import java.net.URI

@Configuration
class RabbitMQConfig {
    @Bean
    @Primary
    fun rabbitProperties(env: Environment): RabbitProperties {
        val uri = env.getProperty("spring.rabbitmq.url").let(::URI)
        val (user, pass) = uri.userInfo?.split(":")
                .let { it?.getOrNull(0) to it?.getOrNull(1) }

        return RabbitProperties().apply {
            host = uri.host
            port = uri.port
            username = user
            password = pass
            virtualHost = uri.path
        }
    }

    @Bean
    fun ampqAdmin(connection: ConnectionFactory): AmqpAdmin = RabbitAdmin(connection)
}