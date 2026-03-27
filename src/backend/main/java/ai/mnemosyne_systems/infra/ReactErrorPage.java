package ai.mnemosyne_systems.infra;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public final class ReactErrorPage {

    private ReactErrorPage() {
    }

    public static Response notFound() {
        return response(Response.Status.NOT_FOUND, "/not-found", "Page not found",
                "The page you requested could not be found.");
    }

    public static Response internalServerError() {
        return response(Response.Status.INTERNAL_SERVER_ERROR, "/error", "Something went wrong",
                "An unexpected error occurred.");
    }

    private static Response response(Response.Status status, String target, String title, String message) {
        String safeTarget = escape(target);
        String safeTitle = escape(title);
        String safeMessage = escape(message);
        String html = """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="utf-8">
                    <title>%s</title>
                    <meta http-equiv="refresh" content="0;url=%s">
                    <script>
                      window.location.replace('%s');
                    </script>
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
                      <p>If the app does not redirect automatically, <a href="%s">open the React app</a>.</p>
                    </main>
                  </body>
                </html>
                """.formatted(safeTitle, safeTarget, safeTarget, safeTitle, safeMessage, safeTarget);
        return Response.status(status).type(MediaType.TEXT_HTML + ";charset=UTF-8").entity(html).build();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
