/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useState } from "react";
import type { AsyncState } from "../types/app";

type JsonResult<T> = { data: T } | { unauthorized: true } | { forbidden: true };

interface JsonSnapshot<T> extends AsyncState<T> {
  url: string | null;
}

function emptyState<T>(): AsyncState<T> {
  return {
    loading: false,
    error: "",
    unauthorized: false,
    forbidden: false,
    empty: true,
    data: null,
  };
}

function loadingState<T>(): AsyncState<T> {
  return {
    loading: true,
    error: "",
    unauthorized: false,
    forbidden: false,
    empty: false,
    data: null,
  };
}

export default function useJson<T>(url: string | null): AsyncState<T> {
  const [state, setState] = useState<JsonSnapshot<T>>({
    url: null,
    ...emptyState<T>(),
  });

  useEffect(() => {
    if (!url) {
      return undefined;
    }

    let active = true;

    fetch(url, { credentials: "same-origin", cache: "no-store" })
      .then(async (response) => {
        if (response.status === 401) {
          return { unauthorized: true } as JsonResult<T>;
        }
        if (response.status === 403) {
          return { forbidden: true } as JsonResult<T>;
        }
        if (!response.ok) {
          throw new Error(`Unable to load ${url}`);
        }
        const data = (await response.json()) as T;
        return { data };
      })
      .then((result: JsonResult<T>) => {
        if (!active) {
          return;
        }
        if ("unauthorized" in result) {
          setState({
            url,
            loading: false,
            error: "",
            unauthorized: true,
            forbidden: false,
            empty: false,
            data: null,
          });
          return;
        }
        if ("forbidden" in result) {
          setState({
            url,
            loading: false,
            error: "",
            unauthorized: false,
            forbidden: true,
            empty: false,
            data: null,
          });
          return;
        }
        const items = (result.data as { items?: unknown[] } | null)?.items;
        const isEmptyList = Array.isArray(items) && items.length === 0;
        setState({
          url,
          loading: false,
          error: "",
          unauthorized: false,
          forbidden: false,
          empty: isEmptyList,
          data: result.data,
        });
      })
      .catch((error: unknown) => {
        if (active) {
          const message =
            error instanceof Error ? error.message : "Unable to load data";
          setState({
            url,
            loading: false,
            error: message,
            unauthorized: false,
            forbidden: false,
            empty: false,
            data: null,
          });
        }
      });

    return () => {
      active = false;
    };
  }, [url]);

  if (!url) {
    return emptyState<T>();
  }

  if (state.url !== url) {
    return loadingState<T>();
  }

  return state;
}
