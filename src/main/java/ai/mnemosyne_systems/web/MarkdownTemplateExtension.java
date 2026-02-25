/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.web;

import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateExtension;
import java.util.regex.Pattern;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

@TemplateExtension
public class MarkdownTemplateExtension {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().escapeHtml(true).build();
    private static final Pattern ATTACHMENT_IMAGE_SRC = Pattern.compile("src=\"(/attachments/\\d+)(?=\\\")");

    public static RawString markdown(String value) {
        if (value == null || value.isBlank()) {
            return new RawString("");
        }
        Node document = PARSER.parse(value);
        String html = RENDERER.render(document);
        html = ATTACHMENT_IMAGE_SRC.matcher(html).replaceAll("src=\"$1/data");
        return new RawString(html);
    }
}
