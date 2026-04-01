\newpage

# Architecture

## Backend

[**billetsys**](https://github.com/mnemosyne-systems/billetsys) is built on [Java](https://en.wikipedia.org/wiki/Java_(programming_language)) 25
and [Quarkus](https://quarkus.io/) 3.33 using [PostgreSQL](https://www.postgresql.org/) as the database.

The data model is defined in `model/` using JPA entities backed by [Hibernate ORM Panache](https://quarkus.io/guides/hibernate-orm-panache).

HTTP endpoints are handled in `resource/` using JAX-RS via [RESTEasy](https://resteasy.dev/).

Business logic is handled in `service/`. PDF generation is handled in `PdfService` and email notifications are handled in `TicketEmailService` using [Quarkus Mailer](https://quarkus.io/guides/mailer).

Shared helpers are defined in `util/`. Session management is handled in `AuthHelper` and file uploads are handled in `AttachmentHelper`.

Infrastructure concerns such as exception mappers and Markdown template extensions are defined in `infra/`.

Application startup and database seeding is handled in `init/` by `AppSeeder`.

## Frontend

The frontend uses [Qute](https://quarkus.io/guides/qute) server-side templates located under `src/main/resources/templates/`.

Role-specific views are organized under `admin/`, `support/`, `superuser/`, and `user/`.

Shared templates for tickets, messages, articles, and other resources are organized by feature.

Report charts are handled by [Chart.js][chartjs] and code highlighting in articles is handled by [highlight.js][highlightjs].

## Entity Model
```mermaid
erDiagram
    COUNTRY {
        BIGINT id PK
        STRING name
        STRING code
    }

    TIMEZONE {
        BIGINT id PK
        STRING name
        BIGINT country_id FK
    }

    COMPANY {
        BIGINT id PK
        STRING name
        BIGINT ticket_sequence
        STRING address1
        STRING address2
        STRING city
        STRING state
        STRING zip
        BIGINT country_id FK
        BIGINT timezone_id FK
        BIGINT superuser_id FK
    }

    INSTALLATION {
        BIGINT id PK
        STRING name
        BIGINT company_id FK
    }

    USER {
        BIGINT id PK
        STRING name
        STRING full_name
        STRING email
        STRING social
        STRING phone_number
        STRING phone_extension
        STRING user_type
        STRING password_hash
        TEXT logo_base64
        BIGINT country_id FK
        BIGINT timezone_id FK
    }

    TICKET {
        BIGINT id PK
        STRING name
        STRING status
        BIGINT company_id FK
        BIGINT requester_id FK
        BIGINT company_entitlement_id FK
        BIGINT affects_version_id FK
        BIGINT resolved_version_id FK
        BIGINT category_id FK
        STRING external_issue_link
    }

    MESSAGE {
        BIGINT id PK
        TEXT body
        DATETIME date
        BIGINT ticket_id FK
        BIGINT author_id FK
    }

    ATTACHMENT {
        BIGINT id PK
        STRING name
        STRING mime_type
        BYTEA data
        BIGINT message_id FK
        BIGINT article_id FK
    }

    ARTICLE {
        BIGINT id PK
        STRING title
        TEXT tags
        TEXT body
    }

    ENTITLEMENT {
        BIGINT id PK
        STRING name
        STRING description
    }

    VERSION {
        BIGINT id PK
        STRING name
        DATE date
        BIGINT entitlement_id FK
    }

    LEVEL {
        BIGINT id PK
        STRING name
        STRING description
        INT level
        STRING color
        INT from_day
        INT from_time
        INT to_day
        INT to_time
        BIGINT country_id FK
        BIGINT timezone_id FK
    }

    CATEGORY {
        BIGINT id PK
        STRING name
        STRING description
        BOOLEAN is_default
    }

    COMPANY_ENTITLEMENT {
        BIGINT id PK
        BIGINT company_id FK
        BIGINT entitlement_id FK
        BIGINT support_level_id FK
        DATE date
        INT duration
    }

    ENTITLEMENT_LEVEL {
        BIGINT entitlement_id PK, FK
        BIGINT support_level_id PK, FK
    }

    COUNTRY ||--o{ TIMEZONE : has
    COUNTRY ||--o{ COMPANY : locates
    COUNTRY ||--o{ USER : locates
    TIMEZONE ||--o{ COMPANY : assigns
    COMPANY ||--|| INSTALLATION : has
    TIMEZONE ||--o{ USER : assigns
    TIMEZONE ||--o{ LEVEL : assigns
    COMPANY ||--o{ TICKET : has
    COMPANY }o--o{ USER : associates
    COMPANY }o--|| PRIMARY_CONTACT : has
    COMPANY ||--o{ COMPANY_ENTITLEMENT : has
    USER ||--o{ TICKET : requests
    TICKET ||--o{ MESSAGE : has
    TICKET }o--o{ USER : "support assigned"
    TICKET }o--o{ USER : "tam assigned"
    MESSAGE ||--o{ ATTACHMENT : has
    ARTICLE ||--o{ ATTACHMENT : has
    MESSAGE }o--|| USER : authored
    ENTITLEMENT ||--o{ COMPANY_ENTITLEMENT : includes
    ENTITLEMENT ||--o{ VERSION : has
    VERSION ||--o{ TICKET : affects
    VERSION ||--o{ TICKET : resolves
    ENTITLEMENT ||--o{ ENTITLEMENT_LEVEL : maps
    COUNTRY ||--o{ LEVEL : locates
    LEVEL ||--o{ COMPANY_ENTITLEMENT : levels
    LEVEL ||--o{ ENTITLEMENT_LEVEL : maps
    COMPANY_ENTITLEMENT ||--o{ TICKET : applies
    CATEGORY ||--o{ TICKET : categorizes
```

[chartjs]: https://www.chartjs.org/
[highlightjs]: https://highlightjs.org/
