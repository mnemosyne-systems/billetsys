import { useEffect, useState } from 'react';
import type { AsyncState } from '../types/app';

type JsonResult<T> =
  | { data: T }
  | { unauthorized: true }
  | { forbidden: true };

export default function useJson<T>(url: string | null): AsyncState<T> {
  const [state, setState] = useState<AsyncState<T>>({
    loading: Boolean(url),
    error: '',
    unauthorized: false,
    forbidden: false,
    empty: false,
    data: null
  });

  useEffect(() => {
    if (!url) {
      setState({ loading: false, error: '', unauthorized: false, forbidden: false, empty: true, data: null });
      return undefined;
    }

    let active = true;
    setState(current => ({ ...current, loading: true, error: '', unauthorized: false, forbidden: false, empty: false }));

    fetch(url, { credentials: 'same-origin', cache: 'no-store' })
      .then(async response => {
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
        if ('unauthorized' in result) {
          setState({ loading: false, error: '', unauthorized: true, forbidden: false, empty: false, data: null });
          return;
        }
        if ('forbidden' in result) {
          setState({ loading: false, error: '', unauthorized: false, forbidden: true, empty: false, data: null });
          return;
        }
        const items = (result.data as { items?: unknown[] } | null)?.items;
        const isEmptyList = Array.isArray(items) && items.length === 0;
        setState({
          loading: false,
          error: '',
          unauthorized: false,
          forbidden: false,
          empty: isEmptyList,
          data: result.data
        });
      })
      .catch((error: unknown) => {
        if (active) {
          const message = error instanceof Error ? error.message : 'Unable to load data';
          setState({
            loading: false,
            error: message,
            unauthorized: false,
            forbidden: false,
            empty: false,
            data: null
          });
        }
      });

    return () => {
      active = false;
    };
  }, [url]);

  return state;
}
