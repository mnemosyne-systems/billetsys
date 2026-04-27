\newpage

# Ticket Import

Billetsys includes a ticket import flow for bringing tickets into the system from external sources.

The first implementation focuses on CSV import. The import framework is designed so that additional adapters can be added later for systems such as Jira and Bugzilla without changing the core ticket creation workflow.

## Purpose

Ticket import is intended for migration and bulk onboarding scenarios.

Typical use cases include:

* Moving a backlog from another helpdesk or issue tracker into billetsys
* Loading tickets during a customer onboarding or pilot
* Re-creating an external queue in billetsys for support processing

The import feature creates billetsys tickets and one initial message per imported row.

## Access

Ticket import is available to the **Admin** and **Support** roles.

The current entry point is exposed from the administration front page as a migration action. After the import is complete, users can open imported tickets from the result table. Imported tickets usually appear in the open support queue until they are assigned.

## Current format

The current import source is CSV.

Required columns:

* `source_key`
* `title`
* `company`
* `entitlement`
* `status`
* `initial_message`

Optional columns:

* `requester_email`
* `category`
* `external_issue_link`
* `created_at`

The import source system defaults to `csv`.

## CSV behavior

Each CSV row is normalized into an internal import row before billetsys creates a ticket.

During creation, billetsys applies these rules:

* Missing `requester_email` falls back to the importing user
* Missing `category` falls back to the default category
* Blank `status` falls back to `Open`
* `initial_message` becomes the first message in the new ticket
* `external_issue_link` is stored on the created ticket when provided

The `created_at` value can be used to preserve the original creation timestamp when that information is available in the source data.

## Import flow

The normal workflow is:

1. Open the import page.
2. Review the required and optional CSV columns.
3. Select a CSV file.
4. Start the import.
5. Review the result summary for created, skipped, and failed rows.
6. Open imported tickets from the result table or continue to the open support queue.

The result table provides row-level feedback so that data issues can be corrected and retried.

## Duplicate handling

Billetsys tracks imported source metadata so the same external row is not imported repeatedly as a new ticket.

Duplicate handling is based on:

* `source_system`
* `source_key`

If a row matches an already imported source ticket, billetsys skips it instead of creating a duplicate ticket.

This behavior makes re-uploading safer during migration work, especially when a file needs to be corrected and retried.

## Validation

Import validation is performed row by row after the CSV structure itself is accepted.

Examples of row-level failures include:

* Missing required row values such as `source_key`
* Unknown company names
* Unknown entitlements for the selected company
* Unknown categories
* Invalid timestamps

When a row fails validation, billetsys records the failure in the import summary and continues processing the other rows.

## Audit trail

Each import is stored as an import batch with row-level import records.

This allows billetsys to retain:

* The import source type
* The uploaded file name
* The user who ran the import
* Started and completed timestamps
* Created, skipped, and failed counts
* Row-level result details

This audit trail helps support migration work and makes import retries easier to understand later.

## Scope of the first version

The first version intentionally keeps the feature small and migration-focused.

Included:

* CSV upload
* Ticket creation
* One initial message per row
* Duplicate skipping
* Row-level result reporting

Not included yet:

* Full message history import
* Attachments from external systems
* Field mapping UI
* Update or overwrite behavior
* Live API connections to Jira or Bugzilla
* Credentials or background sync jobs

## Future adapters

The import structure is designed so that new adapters can emit the same normalized ticket import rows used by the CSV source.

That means future support for Jira and Bugzilla can be added as new import sources while reusing the same validation, ticket creation, duplicate handling, and result reporting pipeline.
