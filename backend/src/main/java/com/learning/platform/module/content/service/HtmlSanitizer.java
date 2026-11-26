package com.learning.platform.module.content.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/** 富文本白名单清洗：去掉 script、事件属性、javascript: 链接等。前端展示时仍需再用 DOMPurify 过滤一次。 */
final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("hr", "s", "del", "ins", "mark", "figure", "figcaption")
            .addAttributes(":all", "style", "class")
            .addAttributes("a", "target")
            .addAttributes("video", "src", "controls", "poster", "width", "height")
            .addProtocols("video", "src", "https");

    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings().prettyPrint(false);

    private HtmlSanitizer() {
    }

    static String clean(String html) {
        return html == null ? null : Jsoup.clean(html, "", SAFELIST, OUTPUT);
    }
}
