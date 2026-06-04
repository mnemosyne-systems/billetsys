/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.resource;

import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/manual")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class ManualApiResource {

    private static final List<String> CHAPTER_SLUGS = List.of("01-introduction", "02-gettingstarted", "03-roles",
            "04-user", "05-superuser", "06-tam", "07-support", "08-admin", "09-articles", "10-reports", "11-tickets",
            "12-messages", "13-attachments", "14-email", "15-companies", "16-users", "17-categories", "18-entitlements",
            "19-levels", "20-versions", "21-profile", "22-security", "23-pdf", "24-navigation", "25-password-reset",
            "60-import", "70-dev", "71-git", "72-architecture", "73-building", "96-sponsors", "97-acknowledgement",
            "98-licenses", "99-references");

    record ChapterSummary(String slug, String title) {
    }

    record ChaptersResponse(List<ChapterSummary> chapters) {
    }

    @GET
    @Path("/chapters")
    public ChaptersResponse chapters() {
        List<ChapterSummary> chapters = CHAPTER_SLUGS.stream().map(slug -> new ChapterSummary(slug, extractTitle(slug)))
                .filter(c -> c.title() != null).toList();
        return new ChaptersResponse(chapters);
    }

    @GET
    @Path("/content/{chapter}")
    @Produces(MediaType.TEXT_PLAIN)
    public String content(@PathParam("chapter") String chapter) {
        if (!CHAPTER_SLUGS.contains(chapter)) {
            throw new NotFoundException();
        }
        String raw = readClasspathResource("manual/en/" + chapter + ".md");
        if (raw == null) {
            throw new NotFoundException();
        }
        return processMarkdown(raw);
    }

    private String extractTitle(String slug) {
        String raw = readClasspathResource("manual/en/" + slug + ".md");
        if (raw == null) {
            return null;
        }
        for (String line : raw.split("\\r?\\n")) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }
        return null;
    }

    private String processMarkdown(String raw) {
        String[] lines = raw.split("\\r?\\n", -1);
        StringBuilder result = new StringBuilder();
        boolean inFrontmatter = false;
        boolean frontmatterDone = false;
        int lineIndex = 0;

        if (lines.length > 0 && lines[0].equals("---")) {
            inFrontmatter = true;
            lineIndex = 1;
        }

        for (int i = lineIndex; i < lines.length; i++) {
            String line = lines[i];

            if (inFrontmatter) {
                if (line.equals("---") || line.equals("...")) {
                    inFrontmatter = false;
                    frontmatterDone = true;
                }
                continue;
            }

            if (!frontmatterDone && line.equals("---")) {
                inFrontmatter = true;
                continue;
            }

            if (line.equals("\\newpage")) {
                continue;
            }

            String processed = line.replaceAll("\\{\\s*width=[^}]*\\}", "");
            processed = processed.replace("](images/", "](/manual/images/");

            result.append(processed).append("\n");
        }

        return result.toString();
    }

    private String readClasspathResource(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return null;
        }
    }
}
