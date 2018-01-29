package com.github.telegram_bots.rss_manager.watcher.service.job

import com.github.telegram_bots.rss_manager.watcher.domain.Post
import com.github.telegram_bots.rss_manager.watcher.domain.PostType
import com.github.telegram_bots.rss_manager.watcher.domain.PostType.*
import io.reactivex.Single
import io.reactivex.functions.Function
import org.jsoup.nodes.Document
import java.time.LocalDateTime
import java.time.ZoneId

class ProcessPostJob : Function<Document, Single<Post>> {
    override fun apply(message: Document): Single<Post> = Single.fromCallable {
        val type = message.getType()
        val (url, name) = message.getURLAndName()

        Post(
            id = message.getId(),
            type = type,
            content = message.getText(type),
            date = message.getDate().atZone(ZoneId.systemDefault()).toEpochSecond(),
            author = message.getAuthor(),
            channelLink = url,
            channelName = name,
            fileURL = message.getFileURL(type)
        )
    }
}

private fun Document.getType(): PostType = when {
    select("#sticker_image").isNotEmpty() -> STICKER
    select(".tgme_widget_message_photo").isNotEmpty() -> IMAGE
    select(".tgme_widget_message_location").isNotEmpty() -> GEO
    select(".tgme_widget_message_document_icon").isNotEmpty() -> FILE
    select(".tgme_widget_message_contact").isNotEmpty() -> CONTACT
    select(".tgme_widget_message_document_icon.audio, audio.tgme_widget_message_voice").isNotEmpty() -> AUDIO
    select("video.tgme_widget_message_video, video.tgme_widget_message_roundvideo").isNotEmpty() -> VIDEO
    else -> TEXT
}

private fun Document.getText(type: PostType): String = when (type) {
    GEO -> select(".tgme_widget_message_location_info").lastOrNull()?.html() ?: ""
    CONTACT -> select(".tgme_widget_message_contact").lastOrNull()?.html() ?: ""
    else -> select(".tgme_widget_message_text").lastOrNull()?.html() ?: ""
}

private fun Document.getId() = select(".tgme_widget_message_date")
        .lastOrNull()
        ?.attr("href")
        ?.split("/")
        ?.lastOrNull()
        ?.split("?")
        ?.firstOrNull()
        ?.let(String::toInt)!!

private fun Document.getURLAndName() = select(".tgme_widget_message_owner_name")
        .lastOrNull()
        ?.let {
            val name = it.select("span")?.last()?.text()!!
            val url = it.attr("href")?.split("/")?.lastOrNull()!!

            url to name
        }!!

private fun Document.getAuthor() = select(".tgme_widget_message_from_author")
        .text()
        .nullIfBlank()

private fun Document.getDate() = getElementsByTag("time")
        .last()
        ?.attr("datetime")
        ?.split("+")
        ?.firstOrNull()
        ?.let(LocalDateTime::parse)!!

private fun Document.getFileURL(type: PostType) = when (type) {
    GEO -> select(".tgme_widget_message_location")
            .firstOrNull()
            ?.attr("style")
            ?.removePrefix("background-image:url('")
            ?.removeSuffix("')")
    AUDIO -> select("audio.tgme_widget_message_voice")
            .attr("src")
            .nullIfBlank()
    IMAGE -> select(".tgme_widget_message_photo_wrap")
            .firstOrNull()
            ?.attr("style")
            ?.removePrefix("background-image:url('")
            ?.removeSuffix("')")
    STICKER -> select("#sticker_image")
            ?.attr("style")
            ?.removePrefix("background-image:url('")
            ?.removeSuffix("')")
    VIDEO -> select("video.tgme_widget_message_video, video.tgme_widget_message_roundvideo")
            .attr("src")
            .nullIfBlank()
    CONTACT -> select(".tgme_widget_message_contact_wrap > .tgme_widget_message_user_photo > img")
            .attr("src")
            .nullIfBlank()
    else -> null
}

private fun String.nullIfBlank() = trim().let { if (it.isBlank()) null else it }