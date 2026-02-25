# Building billetsys

This document describes how to build the project, run it locally, and generate the manual documentation.

## Prerequisites

- Java 17+
- Maven
- PostgreSQL
- Pandoc
- Eisvogel Pandoc template (for PDF manual generation)

## Build the project

```bash
mvn clean package
```

This produces the application artifacts in `target/`.

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

The application is available at:

- http://localhost:8080

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
- `target/billetsys-en.pdf`

