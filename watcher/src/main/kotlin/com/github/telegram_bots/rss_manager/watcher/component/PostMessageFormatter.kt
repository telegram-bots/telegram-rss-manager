package com.github.telegram_bots.rss_manager.watcher.component

import com.github.badoualy.telegram.tl.api.*
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object PostMessageFormatter {
    private val UTF_16LE = Charset.forName("UTF-16LE")!!
    private val HTML_TAGS = listOf(
            "<" to "&lt;",
            ">" to "&gt;",
            "&" to "&amp;"
    )

    fun format(message: TLMessage) = message.message
            .let { if (it.isBlank()) null else it }
            ?.replaceHTMLTags()
            ?.convertEntities(message.entities)
            ?: ""

    private fun String.convertEntities(entities: List<TLAbsMessageEntity>?): String {
        if (entities == null) return this
        val source = toByteArray(UTF_16LE)
        return entities.asSequence()
                .map { Triple(it.format(source), it.startPos(), it.endPos()) }
                .fold(Triple(0, source, ByteArrayOutputStream()), {
                    (curPos, source, target), (replacement, startPos, endPos) ->
                    target.write(source.sliceArray(curPos until startPos))
                    target.write(replacement)
                    Triple(endPos, source, target)
                })
                .let { (endPos, source, target) ->
                    if (endPos < source.size) {
                        target.write(source.sliceArray(endPos until source.size))
                    }

                    target
                }
                .toByteArray()
                .let { String(it, UTF_16LE) }
    }

    private fun String.replaceHTMLTags() = HTML_TAGS.fold(this, { str, (from, to) -> str.replace(from, to) })

    private fun TLAbsMessageEntity.format(byteArray: ByteArray): ByteArray {
        val value = extract(byteArray)
        val result = when (this) {
            is TLMessageEntityTextUrl -> """<a href="$url">$value</a>"""
            is TLMessageEntityBold -> """<b>$value</b>"""
            is TLMessageEntityItalic -> """<i>$value</i>"""
            is TLMessageEntityCode -> """<code>$value</code>"""
            is TLMessageEntityPre -> """<pre>$value</pre>"""
            else -> value
        }

        return result.toByteArray(UTF_16LE)
    }

    private fun TLAbsMessageEntity.startPos() = offset * 2

    private fun TLAbsMessageEntity.endPos() = (offset + length) * 2

    private fun TLAbsMessageEntity.extract(byteArray: ByteArray) = byteArray.sliceArray(startPos() until endPos())
            .let { String(it, UTF_16LE) }
}
