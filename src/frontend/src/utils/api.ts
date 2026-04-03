/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

export type FormValue = string | number | boolean | File | null | undefined;
export type FormEntryValue = FormValue | FormValue[];
export type FormEntries = Array<[string, FormEntryValue]>;

interface FormOptions {
  headers?: HeadersInit;
}

interface MultipartOptions {
  headers?: HeadersInit;
}

export async function fetchJson<T>(url: string): Promise<T> {
  const response = await performRequest(url, {
    credentials: "same-origin",
    cache: "no-store",
    headers: {
      "X-Billetsys-Client": "react",
    },
  });
  if (response.status === 401) {
    throw new Error("You need to sign in again.");
  }
  if (response.status === 403) {
    throw new Error("You do not have access to this page.");
  }
  if (!response.ok) {
    throw new Error(
      toErrorMessage(await response.text(), `Unable to load ${url}`),
    );
  }
  return response.json() as Promise<T>;
}

export async function postForm(
  url: string,
  entries: FormEntries,
  options: FormOptions = {},
): Promise<Response> {
  const body = new URLSearchParams();
  entries.forEach(([key, value]) => appendSearchValue(body, key, value));

  const response = await performRequest(url, {
    method: "POST",
    credentials: "same-origin",
    redirect: "manual",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
      "X-Billetsys-Client": "react",
      ...options.headers,
    },
    body: body.toString(),
  });

  if (isSuccessfulRedirect(response)) {
    return response;
  }
  if (response.status === 401) {
    throw new Error("You need to sign in again.");
  }
  if (response.status === 403) {
    throw new Error("You do not have access to this action.");
  }
  if (!response.ok) {
    throw new Error(toErrorMessage(await response.text(), "Unable to save."));
  }

  return response;
}

export async function postMultipart(
  url: string,
  entries: FormEntries,
  options: MultipartOptions = {},
): Promise<Response> {
  const body = new FormData();
  entries.forEach(([key, value]) => appendFormValue(body, key, value));

  const response = await performRequest(url, {
    method: "POST",
    credentials: "same-origin",
    redirect: "manual",
    headers: {
      "X-Billetsys-Client": "react",
      ...options.headers,
    },
    body,
  });

  if (isSuccessfulRedirect(response)) {
    return response;
  }
  if (response.status === 401) {
    throw new Error("You need to sign in again.");
  }
  if (response.status === 403) {
    throw new Error("You do not have access to this action.");
  }
  if (!response.ok) {
    throw new Error(toErrorMessage(await response.text(), "Unable to save."));
  }

  return response;
}

function appendFormValue(
  searchParams: FormData,
  key: string,
  value: FormEntryValue,
): void {
  if (Array.isArray(value)) {
    value.forEach((item) => appendFormValue(searchParams, key, item));
    return;
  }
  if (value === undefined || value === null) {
    return;
  }
  searchParams.append(key, value instanceof File ? value : String(value));
}

function appendSearchValue(
  searchParams: URLSearchParams,
  key: string,
  value: FormEntryValue,
): void {
  if (Array.isArray(value)) {
    value.forEach((item) => appendSearchValue(searchParams, key, item));
    return;
  }
  if (value === undefined || value === null) {
    return;
  }
  searchParams.append(key, String(value));
}

async function performRequest(
  url: string,
  init: RequestInit,
): Promise<Response> {
  return fetch(url, init);
}

function isSuccessfulRedirect(response: Response): boolean {
  return (
    response.type === "opaqueredirect" ||
    (response.status >= 300 && response.status < 400)
  );
}

function toErrorMessage(text: string, fallback: string): string {
  const trimmed = text.trim();
  if (!trimmed) {
    return fallback;
  }
  if (trimmed.startsWith("<!doctype html") || trimmed.startsWith("<html")) {
    return fallback;
  }
  return trimmed;
}
