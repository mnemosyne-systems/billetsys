\newpage

# Ticket Import

Billetsys includes a ticket import flow for bringing tickets into the system from external sources.

The first implementation focuses on CSV import, and Bugzilla CSV exports are also supported. The import framework is designed so that additional adapters can be added later for systems such as Jira without changing the core ticket creation workflow.

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

## Bugzilla

Tickets can be imported from a Bugzilla CSV export.

### Export

Bugzilla exports CSV from a bug search through `buglist.cgi?ctype=csv`. The CSV button on the results page adds `&human=1` to the link, which replaces the field names in the header with labels such as "Bug ID" and "Summary". Copy the link address of the CSV button, remove `&human=1`, and open that URL, so the header contains the field names. A file whose header contains labels was exported with `human=1` and cannot be imported. The file must use commas as the separator. This was checked with Bugzilla 5.0 and 5.2.

The default header is:

```
bug_id,"product","component","assigned_to","bug_status","resolution","short_desc","changeddate"
```

Extra columns can be requested with the `columnlist` parameter. Add `opendate` to preserve the creation date, and `bug_severity` to import enhancements as `Feature`. Unknown column names are silently ignored, so `creation_ts` has no effect.

```
bug_id,"product","component","assigned_to","bug_status","resolution","short_desc","opendate","changeddate"
101,"Database","Replication","dev@example.com","CONFIRMED"," ---","Replica lags after restart","2026-03-02 09:14:00","2026-03-05 16:20:11"
```

There is one row per bug. An unresolved bug has `---` as its resolution. Bugzilla 5.2 and later add a leading space (` ---`). Values are trimmed on import.

### Import

On the import page, select Bugzilla, choose the company and the entitlement, enter the Bugzilla base URL and the time zone of the export (default UTC), and upload the file. The company and entitlement must exist in billetsys and apply to every row.

The time zone of the export is set in Bugzilla under Preferences, in the field "Timezone used to display dates and times". The default, "Site Default", is the time zone of the Bugzilla server.

### Field mapping

| billetsys field | Bugzilla source |
|---|---|
| `source_system` | Fixed value `bugzilla` |
| `source_key` | `bug_id` |
| `title` | `short_desc` |
| `status` | `bug_status` (see Status) |
| `created_at` | `opendate`, if exported, in the time zone chosen on the import page |
| `external_issue_link` | Base URL + `show_bug.cgi?id=` + `bug_id` |
| `initial_message` | Summary and other Bugzilla fields (see Initial message) |
| `category` | `Bug`, or `Feature` when `bug_severity` is `enhancement` |
| `requester_email` | Not mapped. The importing user is used |
| `company`, `entitlement` | Chosen on the import page |

### Status

Bugzilla statuses are configurable, and the CSV does not say whether a status is open or closed. A fresh Bugzilla uses `UNCONFIRMED`, `CONFIRMED`, `IN_PROGRESS`, `RESOLVED` and `VERIFIED`.

| Bugzilla status | billetsys status |
|---|---|
| `RESOLVED`, `VERIFIED`, `CLOSED` | `Closed` |
| Any other status | `Open` |

Only `Closed` is shown in the closed view.

### Initial message

The initial message contains the summary and the Bugzilla fields that have no place in a billetsys ticket. Empty values, and a resolution of `---`, are left out:

```
Summary: Replica lags after restart

Imported from Bugzilla bug #101
Product: Database
Component: Replication
Assignee: dev@example.com
Status: RESOLVED
Resolution: FIXED
```

### Limitations

* Descriptions, comments and attachments are not imported
* The reporter is not mapped, since it may not exist in billetsys
* Anonymous exports cut `assigned_to` down to the part before the `@`
* `resolution` and `assigned_to` are only kept in the initial message
* The close date is not imported. `changeddate` is the last modification time
* `opendate` has no time zone. It uses the time zone of the Bugzilla user who exported it
* Logged-in exports from Bugzilla 5.2 and later replace `\` with the fullwidth backslash `＼` (U+FF3C). This is reversed on import
* Bugzilla cuts an export at `max_search_results` bugs (10000 by default) without a warning. Split larger migrations into several searches, for example one per product
* Bugs that the exporting user is not allowed to see are left out without a warning. Being an administrator is not enough
* Custom statuses that mean closed need to be changed before the import
* Duplicate detection uses `source_system` and `source_key`, as for CSV imports

## Future adapters

The import structure is designed so that new adapters can emit the same normalized ticket import rows used by the CSV source. The Bugzilla source works this way.

That means future support for Jira can be added as a new import source while reusing the same validation, ticket creation, duplicate handling, and result reporting pipeline.
