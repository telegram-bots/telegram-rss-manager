package com.github.telegram_bots.rss_manager.watcher.config

import com.cloudinary.Cloudinary
import com.github.telegram_bots.rss_manager.watcher.config.properties.CloudProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CloudConfig {
    @Bean
    fun cloud(props: CloudProperties): Cloudinary = Cloudinary(props.url)
}