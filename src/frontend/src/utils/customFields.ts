/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { CustomFieldView } from "../types/domain/customFields";

export const EMPTY_CUSTOM_FIELD_SELECT = "__none__";

export function customFieldValues(
  fields: CustomFieldView[] | undefined,
): Record<string, string> {
  return Object.fromEntries(
    (fields || []).map((field) => [
      String(field.id),
      field.value == null ? "" : field.value,
    ]),
  );
}

export function serializeCustomFields(
  fields: CustomFieldView[] | undefined,
  values: Record<string, string>,
): string {
  const payload: Record<string, string> = {};
  (fields || []).forEach((field) => {
    if (field.active === false) {
      return;
    }
    const key = String(field.id);
    payload[key] = values[key] ?? "";
  });
  return JSON.stringify(payload);
}

export function hasActiveCustomFields(
  fields: CustomFieldView[] | undefined,
): boolean {
  return (fields || []).some((field) => field.active !== false);
}
