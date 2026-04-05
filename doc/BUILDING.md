# Building billetsys

This document describes how to build the project, run it locally, and generate the manual documentation.

## Prerequisites

- Java 25
- Maven
- PostgreSQL
- Pandoc
- Eisvogel Pandoc template (for PDF manual generation)

## Build the project

```bash
mvn package
```

This produces the application artifacts in `target/`.

Use `mvn clean package` only when you want to remove previous build output before packaging.

## Run the project

Set up PostgreSQL with a database owned by user `ticketdb`:

```bash
dropdb ticketdb
createdb -E UTF8 -O ticketdb ticketdb
```

Start in development mode:

```bash
mvn quarkus:dev
```

Quarkus dev mode starts the React frontend through Quinoa. The Vite dev server handles frontend
live reload, so React changes should appear without rebuilding or restarting the backend.

The application is available at:

- http://localhost:8080

## Frontend build integration

The frontend source lives in `src/frontend`.

- From the repository root, run `npm run frontend:fix` to apply the shared frontend Prettier formatting and ESLint auto-fixes.
- From the repository root, run `npm run frontend:check` to execute the frontend lint, type-check, and formatting checks used in CI.
- Install frontend dependencies with `npm ci` from `src/frontend`.
- Run `npm run fix` from `src/frontend` to apply Prettier formatting and ESLint auto-fixes in one step.
- Run `npm run check` from `src/frontend` to execute the frontend lint, type-check, and formatting checks used in CI.
- Run `npm run format` from `src/frontend` to apply the shared frontend formatting rules.
- Development mode: Quinoa proxies `/app` to the Vite dev server.
- Production packaging: Quinoa builds the frontend into `src/frontend/dist` and stages the
  packaged assets under `target/quinoa/build`.

Browser routes such as `/profile` and `/users/...` continue to work through the backend shell
handoff while static frontend assets stay under `/app`.

The old generated frontend folder under `src/backend/main/resources/META-INF/resources/app` is
legacy output from the previous Maven wiring and should not be treated as the active source of
truth for frontend assets.

## Run tests

```bash
mvn test
```

## Build documentation

Generate English manual HTML and PDF (Eisvogel) into `target/`:

```bash
mvn -Pmanual-docs generate-resources
```

Generated files:

- `target/billetsys-en.html`
- `target/billetsys-en.pdf**

## Makefile

The targets are

* `all` - Clean, format, build and run (Default)
* `clean` - Clean
* `format` - Format the source code
* `run` - Run in development mode
* `test` - Run test cases
* `docs` - Generate documentation
* `db-drop` - Drop the database
* `db-create` - Create the database
* `full` - Run everything
