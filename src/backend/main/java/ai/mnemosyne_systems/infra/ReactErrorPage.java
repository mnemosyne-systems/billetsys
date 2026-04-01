package ai.mnemosyne_systems.infra;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public final class ReactErrorPage {

    private ReactErrorPage() {
    }

    public static Response notFound() {
        return response(Response.Status.NOT_FOUND, "/not-found", "Page not found",
                "The page you requested could not be found.", false);
    }

    public static Response internalServerError() {
        return response(Response.Status.INTERNAL_SERVER_ERROR, "/error", "Something went wrong",
                "An unexpected error occurred.", true);
    }

    private static Response response(Response.Status status, String target, String title, String message,
            boolean autoRedirect) {
        String safeTarget = escape(target);
        String safeTitle = escape(title);
        String safeMessage = escape(message);
        String redirectMarkup = autoRedirect ? """
                  <meta http-equiv="refresh" content="0;url=%s">
                  <script>
                    window.location.replace('%s');
                  </script>
                """.formatted(safeTarget, safeTarget) : "";
        String fallbackCopy = autoRedirect ? "open the React app" : "try the React route directly";
        String html = """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="utf-8">
                    <title>%s</title>
                %s
                    <style>
                      body { font-family: system-ui, sans-serif; margin: 0; background: #0f172a; color: #e2e8f0; }
                      main { max-width: 40rem; margin: 10vh auto; padding: 2rem; }
                      a { color: #93c5fd; }
                    </style>
                  </head>
                  <body>
                    <main>
                      <h1>%s</h1>
                      <p>%s</p>
                      <p>If needed, <a href="%s">%s</a>.</p>
                    </main>
                  </body>
                </html>
                """.formatted(safeTitle, redirectMarkup, safeTitle, safeMessage, safeTarget, fallbackCopy);
        return Response.status(status).type(MediaType.TEXT_HTML + ";charset=UTF-8").entity(html).build();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
