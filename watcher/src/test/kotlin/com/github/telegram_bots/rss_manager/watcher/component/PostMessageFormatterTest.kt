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
                message = "\uD83D\uDCB0 Bold \uD83D\uDCB0 Italic \uD83D\uDCB0 Code \uD83D\uDCB0 Pre \uD83D\uDCB0 TextURL \uD83D\uDCB0 http://www.site.com \uD83D\uDCB0 mail@site.com"
                entities = TLObjectVector<TLAbsMessageEntity>().apply {
                    add(TLMessageEntityBold(3, 4))
                    add(TLMessageEntityItalic(11, 6))
                    add(TLMessageEntityCode(21, 4))
                    add(TLMessageEntityPre(29, 3, ""))
                    add(TLMessageEntityTextUrl(36, 7, "http://test.ru"))
                    add(TLMessageEntityUrl(47, 19))
                    add(TLMessageEntityEmail(70, 13))
                }
            }

            val result = """💰 <b>Bold</b> 💰 <i>Italic</i> 💰 <code>Code</code> 💰 <pre>Pre</pre> 💰 <a href="http://test.ru">TextURL</a> 💰 <a href="http://www.site.com">http://www.site.com</a> 💰 <a href="mailto:mail@site.com">mail@site.com</a>"""

            format(input) shouldBe result
        })
    })
})