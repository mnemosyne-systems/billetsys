/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Article;
import ai.mnemosyne_systems.model.CrossReference;
import ai.mnemosyne_systems.model.Message;
import ai.mnemosyne_systems.model.Ticket;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class CrossReferenceService {

    public static final String TARGET_TYPE_TICKET = "ticket";
    public static final String TARGET_TYPE_ARTICLE = "article";

    private record ReferenceTypeConfig(Pattern pattern, String targetType, String displayPrefix, String pathPrefix) {
    }

    private final List<ReferenceTypeConfig> configs = List.of(
            new ReferenceTypeConfig(Pattern.compile("#\\[(\\d+)]"), TARGET_TYPE_TICKET, "#", null),
            new ReferenceTypeConfig(Pattern.compile("\\$\\[(\\d+)]"), TARGET_TYPE_ARTICLE, "$", "/articles/"));

    public void extractAndSaveReferences(Message message, Set<Long> accessibleTicketIds) {
        if (message == null || message.body == null || message.body.isBlank()) {
            return;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (ReferenceTypeConfig config : configs) {
            Matcher matcher = config.pattern.matcher(message.body);
            while (matcher.find()) {
                long targetId = Long.parseLong(matcher.group(1));
                String key = config.targetType + ":" + targetId;
                if (!seen.add(key)) {
                    continue;
                }
                if (config.targetType.equals(TARGET_TYPE_TICKET)) {
                    if (message.ticket != null && message.ticket.id != null && message.ticket.id.equals(targetId)) {
                        continue;
                    }
                    if (accessibleTicketIds != null && !accessibleTicketIds.contains(targetId)) {
                        continue;
                    }
                    Ticket target = Ticket.findById(targetId);
                    if (target == null) {
                        continue;
                    }
                } else if (config.targetType.equals(TARGET_TYPE_ARTICLE)) {
                    Article target = Article.findById(targetId);
                    if (target == null) {
                        continue;
                    }
                }
                CrossReference ref = new CrossReference();
                ref.sourceMessage = message;
                ref.sourceTicket = message.ticket;
                ref.targetType = config.targetType;
                ref.targetId = targetId;
                ref.createdAt = LocalDateTime.now();
                ref.persistAndFlush();
            }
        }
    }

    public Map<Long, Ticket> preloadReferencedTickets(List<String> bodies) {
        Set<Long> allIds = new LinkedHashSet<>();
        for (String body : bodies) {
            if (body == null || body.isBlank()) {
                continue;
            }
            for (ReferenceTypeConfig config : configs) {
                if (!config.targetType.equals(TARGET_TYPE_TICKET)) {
                    continue;
                }
                Matcher matcher = config.pattern.matcher(body);
                while (matcher.find()) {
                    allIds.add(Long.parseLong(matcher.group(1)));
                }
            }
        }
        if (allIds.isEmpty()) {
            return Map.of();
        }
        List<Ticket> tickets = Ticket.list("id in ?1", List.copyOf(allIds));
        Map<Long, Ticket> map = new LinkedHashMap<>();
        for (Ticket t : tickets) {
            map.put(t.id, t);
        }
        return map;
    }

    public String transformBody(String body, String pathPrefix, Map<Long, Ticket> ticketCache) {
        if (body == null || body.isBlank()) {
            return body;
        }
        String result = body;
        for (ReferenceTypeConfig config : configs) {
            Matcher matcher = config.pattern.matcher(result);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                long targetId = Long.parseLong(matcher.group(1));
                String replacement;
                if (config.targetType.equals(TARGET_TYPE_TICKET)) {
                    Ticket target = ticketCache.get(targetId);
                    if (target != null) {
                        String displayName = config.displayPrefix + target.name;
                        replacement = "[" + displayName + "](" + pathPrefix + targetId + ")";
                    } else {
                        replacement = matcher.group(0);
                    }
                } else if (config.targetType.equals(TARGET_TYPE_ARTICLE)) {
                    Article article = Article.findById(targetId);
                    if (article != null) {
                        String displayName = article.title != null && !article.title.isBlank() ? article.title
                                : config.displayPrefix + article.id;
                        replacement = "[" + displayName + "](" + config.pathPrefix + article.id + ")";
                    } else {
                        replacement = matcher.group(0);
                    }
                } else {
                    replacement = matcher.group(0);
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    public CrossReferencesResponse getReferences(Ticket ticket, Set<Long> accessibleTicketIds, String pathPrefix) {
        List<CrossReference> forward = CrossReference.list("sourceTicket = ?1 and targetType = ?2", ticket,
                TARGET_TYPE_TICKET);
        Set<Long> forwardIds = new LinkedHashSet<>();
        for (CrossReference ref : forward) {
            if (accessibleTicketIds != null && !accessibleTicketIds.contains(ref.targetId)) {
                continue;
            }
            forwardIds.add(ref.targetId);
        }
        Map<Long, Ticket> forwardTickets = new LinkedHashMap<>();
        if (!forwardIds.isEmpty()) {
            List<Ticket> loaded = Ticket.list("id in ?1", List.copyOf(forwardIds));
            for (Ticket t : loaded) {
                forwardTickets.put(t.id, t);
            }
        }
        List<CrossReferenceEntry> references = new ArrayList<>();
        for (CrossReference ref : forward) {
            Ticket target = forwardTickets.get(ref.targetId);
            if (target != null) {
                references.add(
                        new CrossReferenceEntry(target.id, target.name, target.displayTitle(), pathPrefix + target.id,
                                ref.createdAt, target.status, target.category != null ? target.category.name : null,
                                target.company != null ? target.company.name : null,
                                target.companyEntitlement != null && target.companyEntitlement.supportLevel != null
                                        ? target.companyEntitlement.supportLevel.name
                                        : null));
            }
        }

        List<CrossReference> backward = CrossReference.list(
                "select cr from CrossReference cr join fetch cr.sourceTicket where cr.targetType = ?1 and cr.targetId = ?2",
                TARGET_TYPE_TICKET, ticket.id);
        List<CrossReferenceEntry> referencedBy = new ArrayList<>();
        for (CrossReference ref : backward) {
            if (accessibleTicketIds != null && !accessibleTicketIds.contains(ref.sourceTicket.id)) {
                continue;
            }
            referencedBy.add(new CrossReferenceEntry(ref.sourceTicket.id, ref.sourceTicket.name,
                    ref.sourceTicket.displayTitle(), pathPrefix + ref.sourceTicket.id, ref.createdAt,
                    ref.sourceTicket.status, ref.sourceTicket.category != null ? ref.sourceTicket.category.name : null,
                    ref.sourceTicket.company != null ? ref.sourceTicket.company.name : null,
                    ref.sourceTicket.companyEntitlement != null
                            && ref.sourceTicket.companyEntitlement.supportLevel != null
                                    ? ref.sourceTicket.companyEntitlement.supportLevel.name
                                    : null));
        }

        List<CrossReference> articleRefs = CrossReference.list("sourceTicket = ?1 and targetType = ?2", ticket,
                TARGET_TYPE_ARTICLE);
        List<ArticleReferenceEntry> articles = new ArrayList<>();
        if (!articleRefs.isEmpty()) {
            Set<Long> articleIds = new LinkedHashSet<>();
            for (CrossReference ref : articleRefs) {
                articleIds.add(ref.targetId);
            }
            List<Article> loadedArticles = Article.list("id in ?1", List.copyOf(articleIds));
            Map<Long, Article> articleMap = new LinkedHashMap<>();
            for (Article a : loadedArticles) {
                articleMap.put(a.id, a);
            }
            for (CrossReference ref : articleRefs) {
                Article a = articleMap.get(ref.targetId);
                if (a != null) {
                    articles.add(new ArticleReferenceEntry(a.id, String.valueOf(a.id), a.title, buildExcerpt(a.body),
                            "/articles/" + a.id));
                }
            }
            articles.sort((l, r) -> {
                String lt = l.articleTitle() == null ? "" : l.articleTitle();
                String rt = r.articleTitle() == null ? "" : r.articleTitle();
                return lt.compareToIgnoreCase(rt);
            });
        }

        return new CrossReferencesResponse(references, referencedBy, articles);
    }

    public record CrossReferenceEntry(Long ticketId, String ticketName, String ticketTitle, String detailPath,
            LocalDateTime createdAt, String status, String categoryName, String companyName, String levelName) {
    }

    public static final int EXCERPT_MAX_LENGTH = 180;

    public static String buildExcerpt(String body) {
        if (body == null) {
            return "";
        }
        String stripped = body.replaceAll("(?s)```.*?```", " ").replaceAll("`([^`]+)`", "$1")
                .replaceAll("!\\[[^\\]]*]\\([^)]*\\)", " ").replaceAll("\\[([^\\]]+)]\\([^)]*\\)", "$1")
                .replaceAll("[#>*_~|\\-]+", " ").replaceAll("\\s+", " ").trim();
        if (stripped.length() <= EXCERPT_MAX_LENGTH) {
            return stripped;
        }
        return stripped.substring(0, EXCERPT_MAX_LENGTH).trim() + "...";
    }

    public String transformBodyForPdf(String body, Map<Long, Ticket> ticketCache) {
        if (body == null || body.isBlank()) {
            return body;
        }
        String result = body;
        for (ReferenceTypeConfig config : configs) {
            Matcher matcher = config.pattern.matcher(result);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                long targetId = Long.parseLong(matcher.group(1));
                String replacement;
                if (config.targetType.equals(TARGET_TYPE_TICKET)) {
                    Ticket target = ticketCache.get(targetId);
                    if (target != null) {
                        replacement = config.displayPrefix + target.name;
                    } else {
                        replacement = matcher.group(0);
                    }
                } else if (config.targetType.equals(TARGET_TYPE_ARTICLE)) {
                    Article article = Article.findById(targetId);
                    if (article != null) {
                        replacement = article.title != null && !article.title.isBlank() ? article.title
                                : config.displayPrefix + article.id;
                    } else {
                        replacement = matcher.group(0);
                    }
                } else {
                    replacement = matcher.group(0);
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    public record ArticleReferenceEntry(Long articleId, String articleName, String articleTitle, String articleExcerpt,
            String detailPath) {
    }

    public record CrossReferencesResponse(List<CrossReferenceEntry> references, List<CrossReferenceEntry> referencedBy,
            List<ArticleReferenceEntry> articles) {
    }
}
