/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { Role } from "../types/app";
import type { NamedEntity, VersionInfo } from "../types/domain/common";
import type { CustomFieldView } from "../types/domain/customFields";

export interface AdvancedSearchBootstrap {
  companies?: NamedEntity[];
  categories?: NamedEntity[];
  versions?: VersionInfo[];
  customFields?: CustomFieldView[];
  [key: string]: unknown;
}

export const ADVANCED_SEARCH_ANY = "__any__";

export const ADVANCED_SEARCH_STATUSES = [
  "Open",
  "Assigned",
  "In Progress",
  "Waiting for External Feedback",
  "Resolved",
  "Closed",
] as const;

/** Status labels the role's ticket list can actually display. */
export function advancedSearchStatuses(role?: Role): readonly string[] {
  if (role === "support") {
    return ADVANCED_SEARCH_STATUSES;
  }
  return ADVANCED_SEARCH_STATUSES.filter(
    (status) => status !== "Waiting for External Feedback",
  );
}

export interface AdvancedSearchCustomFieldDraft {
  value: string;
  from: string;
  to: string;
}

export interface AdvancedSearchDraft {
  q: string;
  statuses: string[];
  categoryId: string;
  companyId: string;
  entitlementName: string;
  levelName: string;
  supportUser: string;
  tamUser: string;
  affectsVersionId: string;
  resolvedVersionId: string;
  activityFrom: string;
  activityTo: string;
  createdFrom: string;
  createdTo: string;
  resolvedFrom: string;
  resolvedTo: string;
  customFields: Record<string, AdvancedSearchCustomFieldDraft>;
}

export interface AdvancedSearchCapabilities {
  companyFilters: boolean;
  supportFilter: boolean;
  tamFilter: boolean;
}

export interface AdvancedSearchChip {
  id: string;
  label: string;
}

export interface AdvancedSearchLookups {
  companies?: NamedEntity[];
  categories?: NamedEntity[];
  versions?: VersionInfo[];
  customFields?: CustomFieldView[];
}

const TOP_LEVEL_KEYS = [
  "q",
  "categoryId",
  "companyId",
  "entitlementName",
  "levelName",
  "supportUser",
  "tamUser",
  "affectsVersionId",
  "resolvedVersionId",
  "activityFrom",
  "activityTo",
  "createdFrom",
  "createdTo",
  "resolvedFrom",
  "resolvedTo",
] as const;

const RANGE_KEYS = [
  ["activity", "activityFrom", "activityTo"],
  ["created", "createdFrom", "createdTo"],
  ["resolved", "resolvedFrom", "resolvedTo"],
] as const;

export function advancedSearchCapabilities(
  role?: Role,
): AdvancedSearchCapabilities {
  const companyFilters =
    role === "support" || role === "superuser" || role === "tam";
  return {
    companyFilters,
    supportFilter: companyFilters,
    tamFilter: role === "support" || role === "superuser",
  };
}

export function emptyAdvancedSearchDraft(): AdvancedSearchDraft {
  return {
    q: "",
    statuses: [],
    categoryId: "",
    companyId: "",
    entitlementName: "",
    levelName: "",
    supportUser: "",
    tamUser: "",
    affectsVersionId: "",
    resolvedVersionId: "",
    activityFrom: "",
    activityTo: "",
    createdFrom: "",
    createdTo: "",
    resolvedFrom: "",
    resolvedTo: "",
    customFields: {},
  };
}

export function parseAdvancedSearch(
  source: URLSearchParams | string,
  role?: Role,
): AdvancedSearchDraft {
  const params = toSearchParams(source);
  const capabilities = advancedSearchCapabilities(role);
  return {
    q: readValue(params, "q"),
    statuses: parseStatuses(params),
    categoryId: readValue(params, "categoryId"),
    companyId: capabilities.companyFilters
      ? readValue(params, "companyId")
      : "",
    entitlementName: capabilities.companyFilters
      ? readValue(params, "entitlementName")
      : "",
    levelName: capabilities.companyFilters
      ? readValue(params, "levelName")
      : "",
    supportUser: capabilities.supportFilter
      ? readValue(params, "supportUser")
      : "",
    tamUser: capabilities.tamFilter ? readValue(params, "tamUser") : "",
    affectsVersionId: readValue(params, "affectsVersionId"),
    resolvedVersionId: readValue(params, "resolvedVersionId"),
    activityFrom: readValue(params, "activityFrom"),
    activityTo: readValue(params, "activityTo"),
    createdFrom: readValue(params, "createdFrom"),
    createdTo: readValue(params, "createdTo"),
    resolvedFrom: readValue(params, "resolvedFrom"),
    resolvedTo: readValue(params, "resolvedTo"),
    customFields: parseCustomFields(params),
  };
}

export function serializeAdvancedSearchParams(
  draft: AdvancedSearchDraft,
  role?: Role,
  fields?: CustomFieldView[],
): Record<string, string> {
  const capabilities = advancedSearchCapabilities(role);
  const params: Record<string, string> = {};

  addValue(params, "q", draft.q);
  if (draft.statuses.length > 0) {
    params.status = draft.statuses.join(",");
  }
  addValue(params, "categoryId", draft.categoryId);
  if (capabilities.companyFilters) {
    addValue(params, "companyId", draft.companyId);
    addValue(params, "entitlementName", draft.entitlementName);
    addValue(params, "levelName", draft.levelName);
  }
  if (capabilities.supportFilter) {
    addValue(params, "supportUser", draft.supportUser);
  }
  if (capabilities.tamFilter) {
    addValue(params, "tamUser", draft.tamUser);
  }
  addValue(params, "affectsVersionId", draft.affectsVersionId);
  addValue(params, "resolvedVersionId", draft.resolvedVersionId);
  addValue(params, "activityFrom", draft.activityFrom);
  addValue(params, "activityTo", draft.activityTo);
  addValue(params, "createdFrom", draft.createdFrom);
  addValue(params, "createdTo", draft.createdTo);
  addValue(params, "resolvedFrom", draft.resolvedFrom);
  addValue(params, "resolvedTo", draft.resolvedTo);

  const definitions =
    fields === undefined
      ? null
      : new Map(
          fields
            .filter((field) => field.active !== false)
            .map((field) => [String(field.id), field]),
        );

  for (const [fieldId, value] of Object.entries(draft.customFields)) {
    const field = definitions ? definitions.get(fieldId) : undefined;
    if (definitions && !field) {
      continue;
    }
    const type = field ? String(field.fieldType).toUpperCase() : null;
    const text = value.value.trim();
    const from = value.from.trim();
    const to = value.to.trim();

    if (type === "DATE") {
      addValue(params, `cf.${fieldId}.from`, from);
      addValue(params, `cf.${fieldId}.to`, to);
    } else if (type === "NUMBER") {
      if (text) {
        addValue(params, `cf.${fieldId}`, text);
      } else {
        addValue(params, `cf.${fieldId}.from`, from);
        addValue(params, `cf.${fieldId}.to`, to);
      }
    } else if (type === "TEXT" || type === "SELECT") {
      addValue(params, `cf.${fieldId}`, text);
    } else {
      addValue(params, `cf.${fieldId}`, text);
      addValue(params, `cf.${fieldId}.from`, from);
      addValue(params, `cf.${fieldId}.to`, to);
    }
  }

  return params;
}

export function validateAdvancedSearch(
  draft: AdvancedSearchDraft,
  fields: CustomFieldView[],
): Record<string, string> {
  const errors: Record<string, string> = {};

  for (const [key, fromKey, toKey] of RANGE_KEYS) {
    const from = draft[fromKey];
    const to = draft[toKey];
    if (from && to && from > to) {
      errors[key] = "The From date must not be after the To date.";
    }
  }

  for (const field of fields) {
    if (field.active === false) {
      continue;
    }
    const fieldId = String(field.id);
    const value = draft.customFields[fieldId];
    if (!value) {
      continue;
    }
    const type = String(field.fieldType).toUpperCase();
    const text = value.value.trim();
    const from = value.from.trim();
    const to = value.to.trim();

    if (type === "NUMBER") {
      if (text && (from || to)) {
        errors[`cf.${fieldId}`] =
          "Use either an exact value or a range, not both.";
        continue;
      }
      if (text && !isNumber(text)) {
        errors[`cf.${fieldId}`] = "Enter a valid number.";
        continue;
      }
      if ((from && !isNumber(from)) || (to && !isNumber(to))) {
        errors[`cf.${fieldId}`] = "Enter a valid number range.";
        continue;
      }
      if (from && to && Number(from) > Number(to)) {
        errors[`cf.${fieldId}`] = "From must not be greater than To.";
      }
    } else if (type === "DATE" && from && to && from > to) {
      errors[`cf.${fieldId}`] = "From must not be after To.";
    }
  }

  return errors;
}

export function advancedSearchChips(
  draft: AdvancedSearchDraft,
  lookups: AdvancedSearchLookups = {},
): AdvancedSearchChip[] {
  const chips: AdvancedSearchChip[] = [];
  const query = draft.q.trim();
  if (query) {
    chips.push({ id: "q", label: `Text: ${query}` });
  }
  for (const status of draft.statuses) {
    chips.push({ id: `status:${status}`, label: `Status: ${status}` });
  }
  if (draft.categoryId) {
    chips.push({
      id: "categoryId",
      label: `Category: ${nameFor(lookups.categories, draft.categoryId)}`,
    });
  }
  if (draft.companyId) {
    chips.push({
      id: "companyId",
      label: `Company: ${nameFor(lookups.companies, draft.companyId)}`,
    });
  }
  if (draft.entitlementName) {
    chips.push({
      id: "entitlementName",
      label: `Entitlement: ${draft.entitlementName}`,
    });
  }
  if (draft.levelName) {
    chips.push({ id: "levelName", label: `Level: ${draft.levelName}` });
  }
  if (draft.supportUser) {
    chips.push({ id: "supportUser", label: `Support: ${draft.supportUser}` });
  }
  if (draft.tamUser) {
    chips.push({ id: "tamUser", label: `TAM: ${draft.tamUser}` });
  }
  if (draft.affectsVersionId) {
    chips.push({
      id: "affectsVersionId",
      label: `Affects: ${nameFor(lookups.versions, draft.affectsVersionId)}`,
    });
  }
  if (draft.resolvedVersionId) {
    chips.push({
      id: "resolvedVersionId",
      label: `Resolved: ${nameFor(lookups.versions, draft.resolvedVersionId)}`,
    });
  }
  for (const [key, fromKey, toKey] of RANGE_KEYS) {
    const from = draft[fromKey];
    const to = draft[toKey];
    if (from || to) {
      const label = key.charAt(0).toUpperCase() + key.slice(1);
      chips.push({
        id: key,
        label: `${label}: ${from || "…"} – ${to || "…"}`,
      });
    }
  }
  for (const field of lookups.customFields || []) {
    if (field.active === false) {
      continue;
    }
    const fieldId = String(field.id);
    const value = draft.customFields[fieldId];
    if (!value) {
      continue;
    }
    const label = customFieldChipLabel(field, value);
    if (label) {
      chips.push({ id: `cf:${fieldId}`, label });
    }
  }
  return chips;
}

export function removeAdvancedSearchChip(
  draft: AdvancedSearchDraft,
  chipId: string,
): AdvancedSearchDraft {
  if (chipId === "q") {
    return { ...draft, q: "" };
  }
  if (chipId.startsWith("status:")) {
    const status = chipId.slice("status:".length);
    return {
      ...draft,
      statuses: draft.statuses.filter((value) => value !== status),
    };
  }
  if (chipId === "activity") {
    return { ...draft, activityFrom: "", activityTo: "" };
  }
  if (chipId === "created") {
    return { ...draft, createdFrom: "", createdTo: "" };
  }
  if (chipId === "resolved") {
    return { ...draft, resolvedFrom: "", resolvedTo: "" };
  }
  if (chipId.startsWith("cf:")) {
    const customFields = { ...draft.customFields };
    delete customFields[chipId.slice("cf:".length)];
    return { ...draft, customFields };
  }
  if (TOP_LEVEL_KEYS.includes(chipId as (typeof TOP_LEVEL_KEYS)[number])) {
    return { ...draft, [chipId]: "" };
  }
  return draft;
}

export function advancedSearchFilterSignature(
  source: URLSearchParams | string,
  role?: Role,
): string {
  const params = toSearchParams(source);
  const capabilities = advancedSearchCapabilities(role);
  const keys: string[] = [
    "q",
    "status",
    "categoryId",
    "affectsVersionId",
    "resolvedVersionId",
    "activityFrom",
    "activityTo",
    "createdFrom",
    "createdTo",
    "resolvedFrom",
    "resolvedTo",
  ];
  if (capabilities.companyFilters) {
    keys.push("companyId", "entitlementName", "levelName");
  }
  if (capabilities.supportFilter) {
    keys.push("supportUser");
  }
  if (capabilities.tamFilter) {
    keys.push("tamUser");
  }

  const entries: string[] = [];
  for (const key of keys) {
    for (const value of params.getAll(key)) {
      if (value) {
        entries.push(`${key}=${value}`);
      }
    }
  }
  for (const [key, value] of params.entries()) {
    if (key.startsWith("cf.") && value) {
      entries.push(`${key}=${value}`);
    }
  }
  entries.sort();
  return entries.join("&");
}

function toSearchParams(source: URLSearchParams | string): URLSearchParams {
  return typeof source === "string" ? new URLSearchParams(source) : source;
}

function readValue(params: URLSearchParams, key: string): string {
  return (params.get(key) || "").trim();
}

function parseStatuses(params: URLSearchParams): string[] {
  const statuses: string[] = [];
  for (const raw of params.getAll("status")) {
    for (const value of raw.split(",")) {
      const status = value.trim();
      if (status && !statuses.includes(status)) {
        statuses.push(status);
      }
    }
  }
  return statuses;
}

function parseCustomFields(
  params: URLSearchParams,
): Record<string, AdvancedSearchCustomFieldDraft> {
  const result: Record<string, AdvancedSearchCustomFieldDraft> = {};
  for (const [key, value] of params.entries()) {
    if (!key.startsWith("cf.")) {
      continue;
    }
    let fieldId = key.slice("cf.".length);
    let part: keyof AdvancedSearchCustomFieldDraft = "value";
    if (fieldId.endsWith(".from")) {
      fieldId = fieldId.slice(0, -".from".length);
      part = "from";
    } else if (fieldId.endsWith(".to")) {
      fieldId = fieldId.slice(0, -".to".length);
      part = "to";
    }
    if (!/^\d+$/.test(fieldId)) {
      continue;
    }
    const current = result[fieldId] || { value: "", from: "", to: "" };
    current[part] = value;
    result[fieldId] = current;
  }
  return result;
}

function addValue(
  params: Record<string, string>,
  key: string,
  value: string,
): void {
  const trimmed = value.trim();
  if (trimmed) {
    params[key] = trimmed;
  }
}

function nameFor(items: NamedEntity[] | undefined, id: string): string {
  const match = (items || []).find((item) => String(item.id) === id);
  return match?.name ? String(match.name) : id;
}

function isNumber(value: string): boolean {
  return Number.isFinite(Number(value));
}

function customFieldChipLabel(
  field: CustomFieldView,
  value: AdvancedSearchCustomFieldDraft,
): string {
  const name = field.name || `Custom field ${field.id}`;
  const type = String(field.fieldType).toUpperCase();
  const text = value.value.trim();
  const from = value.from.trim();
  const to = value.to.trim();

  if (type === "DATE") {
    return from || to ? `${name}: ${from || "…"} – ${to || "…"}` : "";
  }
  if (type === "NUMBER") {
    if (text) {
      return `${name}: ${text}`;
    }
    return from || to ? `${name}: ${from || "…"} – ${to || "…"}` : "";
  }
  return text ? `${name}: ${text}` : "";
}
