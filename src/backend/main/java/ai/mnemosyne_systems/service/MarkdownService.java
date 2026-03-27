/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

@ApplicationScoped
public class MarkdownService {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

    public String renderHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return renderer.render(parser.parse(markdown));
    }
}
