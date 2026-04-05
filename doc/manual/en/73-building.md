\newpage

# Building

## Development workflow

[**billetsys**][billetsys] uses [Quarkus][quarkus] for the backend and integrates the React frontend
through [Quinoa][quinoa] and [Vite][vite].

For normal development, start the application from the repository root with

```sh
mvn quarkus:dev
```

Keep this command running while you work. Backend changes are handled by Quarkus dev mode and
frontend changes are reloaded through the Vite dev server without rebuilding or restarting the
application.

Use

```sh
mvn clean quarkus:dev
```

only when you need to clear `target/` and force a fresh rebuild because local build output looks
stale or inconsistent.

## Production build

To create a production-style build from the repository root, run

```sh
mvn package
```

This packages the backend and frontend together.

Use

```sh
mvn clean package
```

only when you want to remove previous build output before packaging.

## Frontend packaging

The frontend source lives in `src/frontend`.

In development mode, Quinoa proxies requests under `/app` to the Vite dev server.

During packaging, Quinoa builds the frontend into `src/frontend/dist` and stages the packaged
frontend assets under `target/quinoa/build`.

Browser routes such as `/profile` and `/users/...` continue to be handled through the backend
shell handoff while static frontend assets remain under `/app`.

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
