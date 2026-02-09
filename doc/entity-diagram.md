<!--
  Eclipse Public License - v 2.0

    THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
    PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
    OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
-->

# Entity Diagram

```mermaid
erDiagram
    COMPANY {
        BIGINT id PK
        STRING name
        STRING address1
        STRING address2
        STRING city
        STRING state
        STRING zip
        STRING country
    }

    TICKET {
        BIGINT id PK
        STRING name
        STRING status
        BIGINT company_id FK
        BIGINT requester_id FK
        BIGINT company_entitlement_id FK
    }

    MESSAGE {
        BIGINT id PK
        TEXT body
        DATETIME date
        BIGINT ticket_id FK
    }

    USER {
        BIGINT id PK
        STRING name
        STRING email
        STRING type
        STRING password_hash
        TEXT logo_base64
    }

    ENTITLEMENT {
        BIGINT id PK
        STRING name
        STRING description
        INT price
    }

    SUPPORT_LEVEL {
        BIGINT id PK
        STRING name
        STRING description
        INT critical
        STRING critical_color
        INT escalate
        STRING escalate_color
        INT normal
        STRING normal_color
    }

    COMPANY_ENTITLEMENT {
        BIGINT id PK
        BIGINT company_id FK
        BIGINT entitlement_id FK
        BIGINT support_level_id FK
    }

    COMPANY ||--o{ TICKET : has
    TICKET ||--o{ MESSAGE : has
    COMPANY }o--o{ USER : associates
    TICKET }o--o{ USER : assigned
    COMPANY ||--o{ COMPANY_ENTITLEMENT : has
    ENTITLEMENT ||--o{ COMPANY_ENTITLEMENT : includes
    SUPPORT_LEVEL ||--o{ COMPANY_ENTITLEMENT : levels
    COMPANY_ENTITLEMENT ||--o{ TICKET : applies
```
