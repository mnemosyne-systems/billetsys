package ai.mnemosyne_systems.infra;

import io.quarkus.runtime.LaunchMode;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Pattern;

@WebFilter("/*")
public class ReactShellFilter implements Filter {

    private static final List<Pattern> SPA_PATHS = List.of(Pattern.compile("^/$"), Pattern.compile("^/login$"),
            Pattern.compile(
                    "^/(error|not-found|profile|reports|owner|users|companies|articles|categories|entitlements|levels)$"),
            Pattern.compile("^/profile/password$"), Pattern.compile("^/owner/edit$"), Pattern.compile("^/users/new$"),
            Pattern.compile("^/users/[^/]+(?:/edit)?$"), Pattern.compile("^/tickets(?:$|/new$|/[^/]+/edit$)"),
            Pattern.compile("^/messages(?:$|/new$|/[^/]+/edit$)"), Pattern.compile("^/attachments/[^/]+$"),
            Pattern.compile("^/companies(?:$|/new$|/[^/]+(?:/edit)?$)"),
            Pattern.compile("^/articles(?:$|/new$|/[^/]+(?:/edit)?$)"),
            Pattern.compile("^/categories(?:$|/new$|/[^/]+(?:/edit)?$)"),
            Pattern.compile("^/entitlements(?:$|/new$|/[^/]+(?:/edit)?$)"),
            Pattern.compile("^/levels(?:$|/new$|/[^/]+(?:/edit)?$)"),
            Pattern.compile("^/support/tickets(?:$|/open$|/closed$|/new$|/[^/]+$)"),
            Pattern.compile("^/support/users(?:$|/new$)"),
            Pattern.compile("^/support/(support-users|tam-users|superuser-users|user-profiles|companies)/[^/]+$"),
            Pattern.compile("^/tam/users(?:$|/new$)"),
            Pattern.compile("^/user/tickets(?:$|/open$|/closed$|/new$|/[^/]+$)"),
            Pattern.compile("^/user/(support-users|tam-users|superuser-users|user-profiles|companies)/[^/]+$"),
            Pattern.compile("^/superuser/tickets(?:$|/open$|/closed$|/new$|/[^/]+$)"),
            Pattern.compile("^/superuser/users(?:$|/new$)"),
            Pattern.compile("^/superuser/(support-users|superuser-users|user-profiles|companies)/[^/]+$"));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (shouldCanonicalizeAppShell(httpRequest)) {
            httpResponse.setHeader("Cache-Control", "no-store");
            httpResponse.sendRedirect(canonicalAppLocation(httpRequest));
            return;
        }
        if (shouldServeReactShell(httpRequest)) {
            httpResponse.setHeader("Cache-Control", "no-store");
            serveReactShell(httpRequest, httpResponse);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean shouldServeReactShell(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String accept = request.getHeader("Accept");
        if (!acceptsReactShell(accept, path)) {
            return false;
        }
        return SPA_PATHS.stream().anyMatch(pattern -> pattern.matcher(path).matches());
    }

    private boolean acceptsReactShell(String accept, String path) {
        if (accept != null && (accept.contains("text/html") || accept.contains("application/xhtml+xml"))) {
            return true;
        }
        return LaunchMode.current() == LaunchMode.DEVELOPMENT && (accept == null || accept.contains("*/*"))
                && !isLegacyBaseRoute(path);
    }

    private boolean isLegacyBaseRoute(String path) {
        return switch (path) {
            case "/", "/login", "/error", "/not-found", "/profile", "/reports", "/owner", "/users", "/companies",
                    "/articles", "/categories", "/entitlements", "/levels", "/support/tickets", "/support/users",
                    "/tam/users", "/user/tickets", "/superuser/tickets", "/superuser/users" -> true;
            default -> false;
        };
    }

    private boolean shouldCanonicalizeAppShell(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.equals("/app") && !path.equals("/app/") && !path.startsWith("/app/")) {
            return false;
        }
        if (path.startsWith("/app/assets/") || path.startsWith("/app/node_modules/") || path.startsWith("/app/@vite/")
                || path.startsWith("/app/src/")) {
            return false;
        }
        String accept = request.getHeader("Accept");
        return accept != null && (accept.contains("text/html") || accept.contains("application/xhtml+xml"));
    }

    private String canonicalAppLocation(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String query = request.getQueryString();
        StringBuilder location = new StringBuilder(request.getContextPath());
        if (!path.equals("/app") && !path.equals("/app/")) {
            location.append(path.substring("/app".length()));
        } else {
            location.append('/');
        }
        if (query != null && !query.isBlank()) {
            location.append('?').append(query);
        }
        return location.toString();
    }

    private void serveReactShell(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        HttpRequest shellRequest = HttpRequest.newBuilder(reactShellUri(request)).header("Accept", "text/html").GET()
                .build();
        try {
            HttpResponse<InputStream> shellResponse = HttpClient.newHttpClient().send(shellRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            response.setStatus(shellResponse.statusCode());
            shellResponse.headers().firstValue("content-type").ifPresentOrElse(response::setContentType,
                    () -> response.setContentType("text/html;charset=UTF-8"));
            try (InputStream body = shellResponse.body()) {
                body.transferTo(response.getOutputStream());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServletException("Unable to load React shell.", ex);
        }
    }

    private URI reactShellUri(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String contextPath = request.getContextPath();
        String defaultPortScheme = "https".equalsIgnoreCase(scheme) ? ":443" : ":80";
        String portPart = ":" + port;
        if (portPart.equals(defaultPortScheme)) {
            portPart = "";
        }
        return URI.create(scheme + "://" + host + portPart + contextPath + "/index.html");
    }
}
