<!--
  Eclipse Public License - v 2.0

    THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
    PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
    OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
-->

# Entity Diagram

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
        BIGINT primary_contact_id FK
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
    }

    ENTITLEMENT {
        BIGINT id PK
        STRING name
        STRING description
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
        BOOLEAN is_default
    }

    COMPANY_ENTITLEMENT {
        BIGINT id PK
        BIGINT company_id FK
        BIGINT entitlement_id FK
        BIGINT support_level_id FK
    }

    ENTITLEMENT_LEVEL {
        BIGINT entitlement_id PK, FK
        BIGINT support_level_id PK, FK
    }

    COUNTRY ||--o{ TIMEZONE : has
    COUNTRY ||--o{ COMPANY : locates
    COUNTRY ||--o{ USER : locates
    TIMEZONE ||--o{ COMPANY : assigns
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
    MESSAGE }o--|| USER : authored
    ENTITLEMENT ||--o{ COMPANY_ENTITLEMENT : includes
    ENTITLEMENT ||--o{ ENTITLEMENT_LEVEL : maps
    COUNTRY ||--o{ LEVEL : locates
    LEVEL ||--o{ COMPANY_ENTITLEMENT : levels
    LEVEL ||--o{ ENTITLEMENT_LEVEL : maps
    COMPANY_ENTITLEMENT ||--o{ TICKET : applies
    CATEGORY ||--o{ TICKET : categorizes
```
