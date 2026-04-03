# Frontend Structure

This frontend is a Vite + React + TypeScript application that renders the React shell for `billetsys`.

## Development Workflow

- Run `mvn quarkus:dev` from the repository root for normal frontend and backend development.
- Keep that process running while you edit frontend files so Vite live reload can update the UI.
- Use `mvn clean quarkus:dev` only when you deliberately want to wipe `target/` and force a fresh rebuild.
- Run `npm run typecheck` from `src/frontend` when you want a strict TypeScript check without building.
- Run `npm run check` from `src/frontend` before opening a pull request. This is the main validation command for contributors and CI.
- Run `npm run format` from `src/frontend` when you want to apply the shared Prettier formatting rules across the frontend workspace.
- Run `npm run fix` from `src/frontend` when you want one command that applies both Prettier formatting and ESLint auto-fixes before a final validation pass.
- The root Git hook runs `lint-staged` for this frontend package, so staged frontend files are linted and formatted automatically on commit.
- The pre-commit hook checks staged files only. Run `npm run check` from `src/frontend` before pushing when you want to reproduce the full frontend CI validation locally.

## Entry Flow

- `src/main.tsx`
  Boots React, sets up `BrowserRouter`, and handles `/app` and `/app/#...` URL normalization before render.
- `src/App.tsx`
  Keeps the top-level shell small. It loads the session, sets the document title, chooses the login vs authenticated shell, and renders `AppRoutes`.
- `src/AppRoutes.tsx`
  Acts as the top-level route registry. It composes focused route-group modules and keeps the catch-all status route in one obvious place.

## Folder Layout

- `src/components/`
  Reusable UI pieces shared across pages.
- `src/components/layout/`
  Shell-level layout components such as the authenticated header, login header, and footer.
- `src/components/common/`
  Common UI helpers such as loading/error state wrappers, badges, and attachment pickers.
- `src/components/markdown/`
  Markdown rendering and editing components.
- `src/components/users/`
  Reusable user-related UI blocks and selectors.
- `src/hooks/`
  Shared React hooks for data loading and external script handling.
- `src/pages/`
  Route-level screens. Page implementations should live here instead of inside `AppRoutes.jsx`.
- `src/routes/`
  Route-group modules plus shared route metadata/path helpers. These files define route records that `AppRoutes.jsx` turns into actual React Router `<Route>` elements.
- `src/utils/`
  Shared non-visual helpers for API calls, routing, formatting, navigation, and form helpers.
- `src/types/`
  Shared TypeScript contracts for app/session state, domain payloads, forms, reports, and ticket status values.
- `src/types/domain/`
  Domain contracts split by feature area such as users, content, companies, tickets, attachments, and common primitives.

## Current Responsibility Split

- Keep `App.tsx` limited to app-shell composition only.
- Keep `AppRoutes.tsx` limited to composing route groups and defining the fallback `*` route.
- Put domain-specific route definitions in `src/routes/`.
- Move reusable UI into `components/`.
- Move repeated fetch/state logic into `hooks/`.
- Move pure helper logic into `utils/`.
- Move self-contained screens into `pages/`.
- Put reusable API/domain contracts into `src/types/` instead of redefining them across pages.

## Route Organization

- `src/AppRoutes.tsx`
  Collects route records from each route-group module, wraps them with shared loading/error/auth behavior, and renders the final `<Routes>` tree.
- `src/routes/CoreRoutes.tsx`
  Login, home, shell-level status pages, and profile/owner account flows.
- `src/routes/DirectoryRoutes.tsx`
  Admin, support, TAM, user, and superuser directory-style user/company routes.
- `src/routes/TicketRoutes.tsx`
  Ticket workbench routes plus support, user, and superuser ticket flows.
- `src/routes/MessagingRoutes.tsx`
  Message CRUD routes and attachment preview routes.
- `src/routes/ContentRoutes.tsx`
  Companies, articles, categories, entitlements, and support level routes.
- `src/routes/paths.ts`
  Shared route/path constants for commonly repeated paths. Use this when paths are referenced from route files, pages, navigation helpers, or redirects.

## How Route Files Work

Each file in `src/routes/` exports a function that returns an array of route records. A route record is a plain object with:

- `path`
  The React Router path string.
- `element`
  The page/component to render.
- `requiresAuth` (optional)
  When true, `AppRoutes.tsx` wraps the route with the shared auth guard.
- `allowedRoles` (optional)
  When present, `AppRoutes.tsx` wraps the route with the shared role guard.

`AppRoutes.tsx` is responsible for applying shared wrappers around every route record:

- `Suspense` for lazy-loaded pages
- `RouteErrorBoundary` for route render/lazy import failures
- `RequireAuth` for authenticated-only routes
- `RequireRole` for role-restricted routes

Route-group files should focus on declaring route intent, not repeating wrapper logic.

## Adding Or Updating A Route

When adding a new screen:

1. Create or update the page component in `src/pages/` using `.ts` or `.tsx`.
2. Add the route record to the correct route-group file in `src/routes/`.
3. If the path will be reused in multiple places, add a constant to `src/routes/paths.ts`.
4. If the screen requires sign-in, set `requiresAuth: true`.
5. If the screen belongs to a clearly role-scoped route family such as support, TAM, user, or superuser, add `allowedRoles`.
6. If the page is large or not part of the initial shell, prefer lazy-loading it in the route-group file.
7. For day-to-day development, keep `mvn quarkus:dev` running from the repository root and rely on live reload.
8. Run `npm run build` from `src/frontend` when you want a production-style frontend build.
9. If a new screen consumes backend data that is reused elsewhere, add or update the shared contract under `src/types/` rather than keeping duplicate page-local interfaces.

## Choosing The Right Route File

- Put shell/account/login routes in `CoreRoutes.jsx`.
- Put user/company directory-style routes in `DirectoryRoutes.jsx`.
- Put support, user, superuser, or workbench ticket routes in `TicketRoutes.jsx`.
- Put message and attachment routes in `MessagingRoutes.jsx`.
- Put company/article/category/entitlement/level routes in `ContentRoutes.jsx`.

If a new feature grows large enough, create a new route-group file instead of making an existing one too broad.

## TypeScript Conventions

- Frontend source files should use `.ts` and `.tsx`; `.js` and `.jsx` are no longer used in `src/`.
- Keep shared app contracts in `src/types/app.ts`.
- Keep reusable domain payloads in `src/types/domain/` and re-export them through `src/types/domain.ts`.
- Keep shared form-only state contracts in `src/types/forms.ts`.
- Keep shared reporting contracts in `src/types/reports.ts`.
- Keep shared ticket status constants/types in `src/types/tickets.ts`.
- Keep one-off component-only helper types local when they are not reused anywhere else.

## Important Files

- `src/hooks/useJson.ts`
  Shared JSON loader for authenticated fetch-based page data.
- `src/hooks/useText.ts`
  Shared text loader used for lightweight endpoints.
- `src/hooks/useExternalScript.ts`
  Loads external browser scripts such as Chart.js only when needed.
- `src/utils/api.ts`
  Shared POST/JSON request helpers.
- `src/utils/routing.tsx`
  Client-side route normalization and `SmartLink`.
- `src/utils/navigation.ts`
  Header navigation and ticket-menu helper logic.
- `src/components/routing/RouteErrorBoundary.tsx`
  Route-level error boundary for page render and lazy import failures.
- `src/components/routing/RouteGuards.tsx`
  Shared route guards for authentication and role-based access.

## Refactor Guidance

- Do not add new page or helper logic back into `src/App.tsx`.
- If a change affects only one route screen, place it in the matching file under `src/pages/`.
- If a change adds or reorganizes paths, update the matching module under `src/routes/` instead of expanding `AppRoutes.tsx`.
- If the same literal path appears in several places, move it into `src/routes/paths.ts`.
- Put route access rules in route metadata (`requiresAuth`, `allowedRoles`) instead of hand-rolling checks inside every page when the rule is route-level.
- Keep shared route wrappers in `AppRoutes.tsx`; avoid repeating `Suspense`, error boundaries, or guards inside individual route-group files.
- If code is reused by multiple pages, extract it to `components/`, `hooks/`, or `utils/` instead of duplicating it.
- If type contracts are reused by multiple pages or route groups, extract them to `src/types/` instead of duplicating them.
- If route definitions begin repeating heavily inside a route group, consider a small config map, but keep readability first.
- When a shared helper exports JSX, use `.tsx`.

## Build

Run the frontend build from this folder:

```bash
npm run typecheck
npm run build
```

The typecheck command validates the frontend under strict TypeScript settings without emitting files.

```bash
npm run build
```

The build output is written to `dist/`. Quarkus packages those files through Quinoa, while
`mvn quarkus:dev` proxies `/app/*` to the Vite dev server for live frontend updates.

## Backend Integration

- Quarkus serves the frontend under `/app`.
- In development, Quinoa proxies `/app/*` to the Vite dev server on port `5173`.
- Top-level browser routes such as `/profile` are redirected by the backend to `/app/#...`, and `src/main.tsx` converts those locations back into normal client-side routes during app startup.
- Production packaging uses the `dist/` output generated from this frontend project. The legacy generated folder under `src/backend/main/resources/META-INF/resources/app` should not be used as the active frontend source anymore.
