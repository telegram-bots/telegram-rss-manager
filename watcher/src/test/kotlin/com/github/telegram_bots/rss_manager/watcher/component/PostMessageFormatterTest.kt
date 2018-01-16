package com.github.telegram_bots.rss_manager.watcher.component

import com.github.badoualy.telegram.tl.api.*
import com.github.badoualy.telegram.tl.core.TLObjectVector
import com.github.telegram_bots.rss_manager.watcher.component.PostMessageFormatter.format
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.FeatureSpec

class PostMessageFormatterTest : FeatureSpec({
    feature("format", {
        scenario("should correctly format html message", {
            val input = TLMessage().apply {
                message = "\uD83D\uDCB0 Bold \uD83D\uDCB0 Italic \uD83D\uDCB0 Code \uD83D\uDCB0 Pre \uD83D\uDCB0 TextURL"
                entities = TLObjectVector<TLAbsMessageEntity>().apply {
                    add(TLMessageEntityBold(3, 4))
                    add(TLMessageEntityItalic(11, 6))
                    add(TLMessageEntityCode(21, 4))
                    add(TLMessageEntityPre(29, 3, ""))
                    add(TLMessageEntityTextUrl(36, 7, "http://test.ru"))
                }
            }

            val result = """💰 <b>Bold</b> 💰 <i>Italic</i> 💰 <code>Code</code> 💰 <pre>Pre</pre> 💰 <a href="http://test.ru">TextURL</a>"""

            format(input) shouldBe result
        })
    })
})