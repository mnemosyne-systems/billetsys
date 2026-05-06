# Building billetsys

This document describes how to build the project, run it locally, and generate the manual documentation.

## Prerequisites

- Java 25
- Maven
- Node.js and npm
- PostgreSQL
- Pandoc
- Eisvogel Pandoc template (for PDF manual generation)

## Build the project

Install frontend dependencies before running frontend checks manually:

```bash
cd src/frontend
npm ci
```

```bash
mvn package
```

This produces the application artifacts in `target/`.

Use `mvn clean package` only when you want to remove previous build output before packaging.

## PostgreSQL setup

You will need a [PostgreSQL](https://www.postgresql.org/) setup as

```sh
createuser -P ticketdb
createdb -E UTF8 -O ticketdb ticketdb
```

where the password is `ticketdb`. Enable access in `pg_hba.conf` and reload.

The configuration is defined in `src/backend/main/resources/application.properties`.

## Running billetsys

### Basic: run support services with Compose and start billetsys

From the project root, start the local support services with `make platform`. This will automatically detect and use either Podman Compose or Docker Compose:

```bash
make platform
```

The root `docker-compose.yml` starts:

- `cap` on `http://localhost:3000`
- `valkey` for CAP storage

Then start billetsys itself in development mode:

```bash
mvn quarkus:dev
```

Quarkus dev mode starts the React frontend through Quinoa. The Vite dev server handles frontend
live reload, so React changes should appear without rebuilding or restarting the backend.

The application is available at:

- http://localhost:8080

### Advanced: run services manually

If you prefer not to use Compose, run each dependency yourself and point billetsys at those services with the environment variables documented below.

Set up PostgreSQL with a database owned by user `ticketdb`:

```bash
dropdb ticketdb
createdb -E UTF8 -O ticketdb ticketdb
```

If you want real forgot-password emails to be delivered, or need to configure the CAP widget, refer to the **Password Reset** chapter in the manual (`doc/manual/en/25-password-reset.md`).

Then start billetsys in development mode:

```bash
mvn quarkus:dev
```

### CAP Docker Compose settings

The local CAP container needs these Compose environment values:

- `ADMIN_KEY`: Admin password for the CAP service. Replace the placeholder value in `docker-compose.yml` before using it outside throwaway local testing.
- `REDIS_URL`: CAP storage connection string. The provided Compose file uses `redis://valkey:6379`.

### Application environment variables

To configure the application for local development, copy `.env.example` to `.env` in the repository root and fill out the necessary secrets. Quarkus automatically loads the `.env` file on startup.

Configure the following environment variables when running locally or in deployed environments:

- `APP_PUBLIC_BASE_URL`: Public base URL used in password reset links, for example `http://localhost:8080` locally or your production HTTPS URL.
- `MAIL_FROM`: Sender address used for forgot-password emails.
- `MAIL_MOCK`: Set to `true` for local development if you do not want to send real email. Set it to `false` when testing real SMTP delivery.

For CAP widget setup and real SMTP configuration details, refer to the **Password Reset** chapter in the manual (`doc/manual/en/25-password-reset.md`).


## Frontend build integration

The frontend source lives in `src/frontend`.

- From the repository root, run `npm --prefix src/frontend run fix` to apply the shared frontend Prettier formatting and ESLint auto-fixes.
- From the repository root, run `npm --prefix src/frontend run check` to execute the frontend lint, type-check, and formatting checks used in CI.
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

The `format` target installs frontend npm dependencies when they are missing or when
`src/frontend/package.json` or `src/frontend/package-lock.json` changes.

The targets are

* `all` - Clean, format, build and run (Default)
* `clean` - Clean
* `format` - Format the source code
* `run` - Run in development mode
* `test` - Run test cases
* `docs` - Generate documentation
* `db-drop` - Drop the database
* `db-create` - Create the database
* `platform` - Start compose services (CAP + Valkey)
* `full` - Run everything (db-drop, db-create, platform, clean, format, test, docs, run)
