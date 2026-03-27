package ai.mnemosyne_systems.infra;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        if (shouldServeReactShell(httpRequest)) {
            httpResponse.setHeader("Cache-Control", "no-store");
            request.getRequestDispatcher("/app/index.html").forward(request, response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean shouldServeReactShell(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String accept = request.getHeader("Accept");
        if (accept == null || (!accept.contains("text/html") && !accept.contains("application/xhtml+xml"))) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return SPA_PATHS.stream().anyMatch(pattern -> pattern.matcher(path).matches());
    }
}
