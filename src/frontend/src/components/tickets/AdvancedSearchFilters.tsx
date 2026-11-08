/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { Role } from "../../types/app";
import type { NamedEntity, VersionInfo } from "../../types/domain/common";
import {
  ADVANCED_SEARCH_ANY,
  advancedSearchCapabilities,
  advancedSearchStatuses,
  type AdvancedSearchBootstrap,
  type AdvancedSearchChip,
  type AdvancedSearchCustomFieldDraft,
  type AdvancedSearchDraft,
} from "../../utils/advancedSearch";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import { Checkbox } from "../ui/checkbox";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  FieldTitle,
} from "../ui/field";
import { Input } from "../ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import { XIcon } from "lucide-react";

interface AdvancedSearchFiltersProps {
  role?: Role;
  bootstrap?: AdvancedSearchBootstrap;
  draft: AdvancedSearchDraft;
  errors: Record<string, string>;
  chips: AdvancedSearchChip[];
  onChange: (draft: AdvancedSearchDraft) => void;
  onApply: () => void;
  onReset: () => void;
  onRemoveChip: (chipId: string) => void;
}

export default function AdvancedSearchFilters({
  role,
  bootstrap,
  draft,
  errors,
  chips,
  onChange,
  onApply,
  onReset,
  onRemoveChip,
}: AdvancedSearchFiltersProps) {
  const capabilities = advancedSearchCapabilities(role);
  const categories = bootstrap?.categories || [];
  const companies = bootstrap?.companies || [];
  const versions = bootstrap?.versions || [];
  const customFields = (bootstrap?.customFields || []).filter(
    (field) => field.active !== false,
  );

  function setDraft(patch: Partial<AdvancedSearchDraft>) {
    onChange({ ...draft, ...patch });
  }

  function updateCustomField(
    fieldId: string,
    patch: Partial<AdvancedSearchCustomFieldDraft>,
  ) {
    const current = draft.customFields[fieldId] || {
      value: "",
      from: "",
      to: "",
    };
    onChange({
      ...draft,
      customFields: {
        ...draft.customFields,
        [fieldId]: { ...current, ...patch },
      },
    });
  }

  function toggleStatus(status: string, checked: boolean) {
    setDraft({
      statuses: checked
        ? [...draft.statuses, status]
        : draft.statuses.filter((value) => value !== status),
    });
  }

  return (
    <section
      aria-label="Advanced search filters"
      className="mb-6 rounded-xl border bg-card p-4 text-card-foreground shadow-sm"
    >
      <form
        className="space-y-5"
        onSubmit={(event) => {
          event.preventDefault();
          onApply();
        }}
      >
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-base font-semibold text-[var(--color-section-header)]">
            Advanced search
          </h2>
          <p className="text-xs text-muted-foreground">
            Combine any filters. Empty filters are ignored.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <Field className="sm:col-span-2">
            <FieldLabel htmlFor="advanced-search-q">Search text</FieldLabel>
            <Input
              id="advanced-search-q"
              value={draft.q}
              onChange={(event) => setDraft({ q: event.target.value })}
              placeholder="Ticket name, title, or message text"
            />
            <FieldDescription>
              Matches the ticket name, title, and visible message bodies.
            </FieldDescription>
          </Field>

          <Field className="sm:col-span-2">
            <FieldTitle id="advanced-search-status-label">Status</FieldTitle>
            <div
              role="group"
              aria-labelledby="advanced-search-status-label"
              className="flex flex-wrap gap-x-4 gap-y-2"
            >
              {advancedSearchStatuses(role).map((status) => (
                <label key={status} className="flex items-center gap-2 text-sm">
                  <Checkbox
                    checked={draft.statuses.includes(status)}
                    onCheckedChange={(checked) =>
                      toggleStatus(status, checked === true)
                    }
                  />
                  {status}
                </label>
              ))}
            </div>
          </Field>

          <Field>
            <FieldLabel htmlFor="advanced-search-category">Category</FieldLabel>
            <Select
              value={draft.categoryId || ADVANCED_SEARCH_ANY}
              onValueChange={(value) =>
                setDraft({
                  categoryId: value === ADVANCED_SEARCH_ANY ? "" : value,
                })
              }
            >
              <SelectTrigger id="advanced-search-category" className="w-full">
                <SelectValue placeholder="Any category" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ADVANCED_SEARCH_ANY}>
                  Any category
                </SelectItem>
                {categories.map((category: NamedEntity) => (
                  <SelectItem key={category.id} value={String(category.id)}>
                    {category.name || String(category.id)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>

          {capabilities.companyFilters && (
            <>
              <Field>
                <FieldLabel htmlFor="advanced-search-company">
                  Company
                </FieldLabel>
                <Select
                  value={draft.companyId || ADVANCED_SEARCH_ANY}
                  onValueChange={(value) =>
                    setDraft({
                      companyId: value === ADVANCED_SEARCH_ANY ? "" : value,
                    })
                  }
                >
                  <SelectTrigger
                    id="advanced-search-company"
                    className="w-full"
                  >
                    <SelectValue placeholder="Any company" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={ADVANCED_SEARCH_ANY}>
                      Any company
                    </SelectItem>
                    {companies.map((company: NamedEntity) => (
                      <SelectItem key={company.id} value={String(company.id)}>
                        {company.name || String(company.id)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </Field>

              <Field>
                <FieldLabel htmlFor="advanced-search-entitlement">
                  Entitlement name
                </FieldLabel>
                <Input
                  id="advanced-search-entitlement"
                  value={draft.entitlementName}
                  onChange={(event) =>
                    setDraft({ entitlementName: event.target.value })
                  }
                  placeholder="Exact entitlement name"
                />
                <FieldDescription>
                  Matches the entitlement name exactly.
                </FieldDescription>
              </Field>

              <Field>
                <FieldLabel htmlFor="advanced-search-level">
                  Level name
                </FieldLabel>
                <Input
                  id="advanced-search-level"
                  value={draft.levelName}
                  onChange={(event) =>
                    setDraft({ levelName: event.target.value })
                  }
                  placeholder="Exact support level name"
                />
                <FieldDescription>
                  Matches the support level name exactly.
                </FieldDescription>
              </Field>

              <Field>
                <FieldLabel htmlFor="advanced-search-support-user">
                  Assigned support
                </FieldLabel>
                <Input
                  id="advanced-search-support-user"
                  value={draft.supportUser}
                  onChange={(event) =>
                    setDraft({ supportUser: event.target.value })
                  }
                  placeholder="Name or email contains"
                />
              </Field>
            </>
          )}

          {capabilities.tamFilter && (
            <Field>
              <FieldLabel htmlFor="advanced-search-tam-user">
                TAM user
              </FieldLabel>
              <Input
                id="advanced-search-tam-user"
                value={draft.tamUser}
                onChange={(event) => setDraft({ tamUser: event.target.value })}
                placeholder="Name or email contains"
              />
            </Field>
          )}

          <Field>
            <FieldLabel htmlFor="advanced-search-affects-version">
              Affects version
            </FieldLabel>
            <Select
              value={draft.affectsVersionId || ADVANCED_SEARCH_ANY}
              onValueChange={(value) =>
                setDraft({
                  affectsVersionId: value === ADVANCED_SEARCH_ANY ? "" : value,
                })
              }
            >
              <SelectTrigger
                id="advanced-search-affects-version"
                className="w-full"
              >
                <SelectValue placeholder="Any version" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ADVANCED_SEARCH_ANY}>Any version</SelectItem>
                {versions.map((version: VersionInfo) => (
                  <SelectItem key={version.id} value={String(version.id)}>
                    {version.name || String(version.id)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>

          <Field>
            <FieldLabel htmlFor="advanced-search-resolved-version">
              Resolved version
            </FieldLabel>
            <Select
              value={draft.resolvedVersionId || ADVANCED_SEARCH_ANY}
              onValueChange={(value) =>
                setDraft({
                  resolvedVersionId: value === ADVANCED_SEARCH_ANY ? "" : value,
                })
              }
            >
              <SelectTrigger
                id="advanced-search-resolved-version"
                className="w-full"
              >
                <SelectValue placeholder="Any version" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ADVANCED_SEARCH_ANY}>Any version</SelectItem>
                {versions.map((version: VersionInfo) => (
                  <SelectItem key={version.id} value={String(version.id)}>
                    {version.name || String(version.id)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>

          <DateRangeField
            idPrefix="advanced-search-activity"
            label="Activity date"
            from={draft.activityFrom}
            to={draft.activityTo}
            error={errors.activity}
            onChange={(from, to) =>
              setDraft({ activityFrom: from, activityTo: to })
            }
          />

          <DateRangeField
            idPrefix="advanced-search-created"
            label="Created date"
            from={draft.createdFrom}
            to={draft.createdTo}
            error={errors.created}
            onChange={(from, to) =>
              setDraft({ createdFrom: from, createdTo: to })
            }
          />

          <DateRangeField
            idPrefix="advanced-search-resolved"
            label="Resolved date"
            from={draft.resolvedFrom}
            to={draft.resolvedTo}
            error={errors.resolved}
            onChange={(from, to) =>
              setDraft({ resolvedFrom: from, resolvedTo: to })
            }
          />
        </div>

        {customFields.length > 0 && (
          <div className="space-y-4 border-t pt-4">
            <h3 className="text-sm font-semibold text-[var(--color-section-header)]">
              Custom fields
            </h3>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {customFields.map((field) => {
                const fieldId = String(field.id);
                const value = draft.customFields[fieldId] || {
                  value: "",
                  from: "",
                  to: "",
                };
                const error = errors[`cf.${fieldId}`];
                const type = String(field.fieldType).toUpperCase();

                if (type === "SELECT") {
                  return (
                    <Field key={fieldId}>
                      <FieldLabel
                        htmlFor={`advanced-search-cf-${fieldId}-value`}
                      >
                        {field.name}
                      </FieldLabel>
                      <Select
                        value={value.value || ADVANCED_SEARCH_ANY}
                        onValueChange={(nextValue) =>
                          updateCustomField(fieldId, {
                            value:
                              nextValue === ADVANCED_SEARCH_ANY
                                ? ""
                                : nextValue,
                          })
                        }
                      >
                        <SelectTrigger
                          id={`advanced-search-cf-${fieldId}-value`}
                          className="w-full"
                          aria-invalid={Boolean(error) || undefined}
                        >
                          <SelectValue placeholder="Any value" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value={ADVANCED_SEARCH_ANY}>
                            Any value
                          </SelectItem>
                          {(field.options || []).map((option) => (
                            <SelectItem key={option} value={option}>
                              {option}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FieldError>{error}</FieldError>
                    </Field>
                  );
                }

                if (type === "NUMBER") {
                  return (
                    <Field key={fieldId} className="sm:col-span-2">
                      <FieldTitle>{field.name}</FieldTitle>
                      <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
                        <div className="space-y-1">
                          <label
                            htmlFor={`advanced-search-cf-${fieldId}-value`}
                            className="text-xs text-muted-foreground"
                          >
                            Exact
                          </label>
                          <Input
                            id={`advanced-search-cf-${fieldId}-value`}
                            type="number"
                            step="any"
                            inputMode="decimal"
                            value={value.value}
                            onChange={(event) =>
                              updateCustomField(fieldId, {
                                value: event.target.value,
                              })
                            }
                            aria-invalid={Boolean(error) || undefined}
                          />
                        </div>
                        <div className="space-y-1">
                          <label
                            htmlFor={`advanced-search-cf-${fieldId}-from`}
                            className="text-xs text-muted-foreground"
                          >
                            From
                          </label>
                          <Input
                            id={`advanced-search-cf-${fieldId}-from`}
                            type="number"
                            step="any"
                            inputMode="decimal"
                            value={value.from}
                            onChange={(event) =>
                              updateCustomField(fieldId, {
                                from: event.target.value,
                              })
                            }
                            aria-invalid={Boolean(error) || undefined}
                          />
                        </div>
                        <div className="space-y-1">
                          <label
                            htmlFor={`advanced-search-cf-${fieldId}-to`}
                            className="text-xs text-muted-foreground"
                          >
                            To
                          </label>
                          <Input
                            id={`advanced-search-cf-${fieldId}-to`}
                            type="number"
                            step="any"
                            inputMode="decimal"
                            value={value.to}
                            onChange={(event) =>
                              updateCustomField(fieldId, {
                                to: event.target.value,
                              })
                            }
                            aria-invalid={Boolean(error) || undefined}
                          />
                        </div>
                      </div>
                      <FieldError>{error}</FieldError>
                    </Field>
                  );
                }

                if (type === "DATE") {
                  return (
                    <Field key={fieldId} className="sm:col-span-2">
                      <FieldTitle>{field.name}</FieldTitle>
                      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                        <div className="space-y-1">
                          <label
                            htmlFor={`advanced-search-cf-${fieldId}-from`}
                            className="text-xs text-muted-foreground"
                          >
                            From
                          </label>
                          <Input
                            id={`advanced-search-cf-${fieldId}-from`}
                            type="date"
                            value={value.from}
                            onChange={(event) =>
                              updateCustomField(fieldId, {
                                from: event.target.value,
                              })
                            }
                            aria-invalid={Boolean(error) || undefined}
                          />
                        </div>
                        <div className="space-y-1">
                          <label
                            htmlFor={`advanced-search-cf-${fieldId}-to`}
                            className="text-xs text-muted-foreground"
                          >
                            To
                          </label>
                          <Input
                            id={`advanced-search-cf-${fieldId}-to`}
                            type="date"
                            value={value.to}
                            onChange={(event) =>
                              updateCustomField(fieldId, {
                                to: event.target.value,
                              })
                            }
                            aria-invalid={Boolean(error) || undefined}
                          />
                        </div>
                      </div>
                      <FieldError>{error}</FieldError>
                    </Field>
                  );
                }

                return (
                  <Field key={fieldId}>
                    <FieldLabel htmlFor={`advanced-search-cf-${fieldId}-value`}>
                      {field.name}
                    </FieldLabel>
                    <Input
                      id={`advanced-search-cf-${fieldId}-value`}
                      value={value.value}
                      onChange={(event) =>
                        updateCustomField(fieldId, {
                          value: event.target.value,
                        })
                      }
                      placeholder="Contains text"
                      aria-invalid={Boolean(error) || undefined}
                    />
                    <FieldError>{error}</FieldError>
                  </Field>
                );
              })}
            </div>
          </div>
        )}

        <div className="flex flex-wrap items-center gap-2 border-t pt-4">
          <Button type="submit">Search</Button>
          <Button type="button" variant="secondary" onClick={onReset}>
            Reset
          </Button>
        </div>

        {chips.length > 0 && (
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-medium text-muted-foreground">
              Active filters:
            </span>
            {chips.map((chip) => (
              <Badge
                key={chip.id}
                variant="secondary"
                className="max-w-full gap-1 pr-1"
              >
                <span className="truncate">{chip.label}</span>
                <button
                  type="button"
                  className="ml-0.5 rounded-full p-0.5 hover:bg-foreground/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  aria-label={`Remove ${chip.label}`}
                  onClick={() => onRemoveChip(chip.id)}
                >
                  <XIcon className="size-3" aria-hidden="true" />
                </button>
              </Badge>
            ))}
          </div>
        )}
      </form>
    </section>
  );
}

interface DateRangeFieldProps {
  idPrefix: string;
  label: string;
  from: string;
  to: string;
  error?: string;
  onChange: (from: string, to: string) => void;
}

function DateRangeField({
  idPrefix,
  label,
  from,
  to,
  error,
  onChange,
}: DateRangeFieldProps) {
  return (
    <Field className="sm:col-span-2">
      <FieldTitle>{label}</FieldTitle>
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        <div className="space-y-1">
          <label
            htmlFor={`${idPrefix}-from`}
            className="text-xs text-muted-foreground"
          >
            From
          </label>
          <Input
            id={`${idPrefix}-from`}
            type="date"
            value={from}
            onChange={(event) => onChange(event.target.value, to)}
            aria-invalid={Boolean(error) || undefined}
          />
        </div>
        <div className="space-y-1">
          <label
            htmlFor={`${idPrefix}-to`}
            className="text-xs text-muted-foreground"
          >
            To
          </label>
          <Input
            id={`${idPrefix}-to`}
            type="date"
            value={to}
            onChange={(event) => onChange(from, event.target.value)}
            aria-invalid={Boolean(error) || undefined}
          />
        </div>
      </div>
      <FieldError>{error}</FieldError>
    </Field>
  );
}
