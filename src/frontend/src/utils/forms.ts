/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { DirectoryUserBootstrap } from "../types/domain";

export type BrowserFormValue = string | number | boolean | null | undefined;
export type BrowserFormEntry = BrowserFormValue | BrowserFormValue[];
export type BrowserFormEntries = Array<[string, BrowserFormEntry]>;

export interface DirectoryUserFormState {
  id: string;
  name: string;
  fullName: string;
  email: string;
  social: string;
  phoneNumber: string;
  phoneExtension: string;
  countryId: string;
  timezoneId: string;
  type: string;
  companyId: string;
  password: string;
}

export function appendFormValue(
  searchParams: URLSearchParams,
  key: string,
  value: BrowserFormEntry,
): void {
  if (Array.isArray(value)) {
    value.forEach((item) => appendFormValue(searchParams, key, item));
    return;
  }
  if (value === undefined || value === null) {
    return;
  }
  searchParams.append(key, String(value));
}

export function appendBrowserFormValue(
  form: HTMLFormElement,
  key: string,
  value: BrowserFormEntry,
): void {
  if (Array.isArray(value)) {
    value.forEach((item) => appendBrowserFormValue(form, key, item));
    return;
  }
  if (value === undefined || value === null) {
    return;
  }
  const input = document.createElement("input");
  input.type = "hidden";
  input.name = key;
  input.value = String(value);
  form.appendChild(input);
}

export function submitBrowserForm(
  url: string,
  entries: BrowserFormEntries,
): void {
  const form = document.createElement("form");
  form.method = "post";
  form.action = url;
  form.style.display = "none";
  entries.forEach(([key, value]) => appendBrowserFormValue(form, key, value));
  document.body.appendChild(form);
  form.submit();
}

export function isNetworkRequestError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : "";
  return (
    error instanceof TypeError ||
    /networkerror|failed to fetch|load failed/i.test(message)
  );
}

export function createDirectoryUserFormState(
  bootstrap: DirectoryUserBootstrap,
): DirectoryUserFormState {
  return {
    id: bootstrap?.user?.id ? String(bootstrap.user.id) : "",
    name: bootstrap?.user?.name || "",
    fullName: bootstrap?.user?.fullName || "",
    email: bootstrap?.user?.email || "",
    social: bootstrap?.user?.social || "",
    phoneNumber: bootstrap?.user?.phoneNumber || "",
    phoneExtension: bootstrap?.user?.phoneExtension || "",
    countryId: bootstrap?.user?.countryId
      ? String(bootstrap.user.countryId)
      : "",
    timezoneId: bootstrap?.user?.timezoneId
      ? String(bootstrap.user.timezoneId)
      : "",
    type: bootstrap?.user?.type || bootstrap?.types?.[0]?.value || "",
    companyId: bootstrap?.user?.companyId
      ? String(bootstrap.user.companyId)
      : bootstrap?.selectedCompanyId
        ? String(bootstrap.selectedCompanyId)
        : "",
    password: "",
  };
}
