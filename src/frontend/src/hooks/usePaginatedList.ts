/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useCallback, useEffect, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import useJson from "./useJson";
import { toQueryString } from "../utils/formatting";
import type { AsyncState } from "../types/app";
import type { CollectionResponse } from "../types/domain";

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 25, 50] as const;

export type SortDirection = "asc" | "desc";

export interface PaginatedListState<T extends CollectionResponse<unknown>> {
  state: AsyncState<T>;
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  sort: string | null;
  dir: SortDirection;
  setPage: (page: number) => void;
  setPageSize: (size: number) => void;
  setSort: (column: string) => void;
  pageSizeOptions: readonly number[];
}

interface UsePaginatedListOptions {
  apiUrl: string;
  extraParams?: Record<string, string | undefined>;
  defaultPageSize?: number;
  defaultSort?: string;
  defaultDir?: SortDirection;
  sortKeys?: readonly string[];
}

export default function usePaginatedList<T extends CollectionResponse<unknown>>(
  options: UsePaginatedListOptions,
): PaginatedListState<T> {
  const {
    apiUrl,
    extraParams,
    defaultPageSize,
    defaultSort,
    defaultDir = "asc",
    sortKeys,
  } = options;
  const effectiveDefaultPageSize = defaultPageSize || DEFAULT_PAGE_SIZE;

  const [searchParams, setSearchParams] = useSearchParams();

  const requestedPage = positiveInt(searchParams.get("page"), 1);
  const requestedPageSize = positiveInt(
    searchParams.get("pageSize"),
    effectiveDefaultPageSize,
  );
  const requestedSort = searchParams.get("sort");
  const sort =
    requestedSort && (!sortKeys || sortKeys.includes(requestedSort))
      ? requestedSort
      : defaultSort || null;
  const requestedDir = searchParams.get("dir");
  const dir: SortDirection =
    requestedDir === "asc" || requestedDir === "desc"
      ? requestedDir
      : defaultDir;

  const fullUrl = useMemo(() => {
    const params: Record<string, string | undefined> = {
      ...extraParams,
      page: String(requestedPage),
      pageSize: String(requestedPageSize),
      sort: sort || undefined,
      dir: sort ? dir : undefined,
    };
    return `${apiUrl}${toQueryString(params)}`;
  }, [apiUrl, extraParams, requestedPage, requestedPageSize, sort, dir]);

  const state = useJson<T>(fullUrl);

  const totalItems = state.data?.totalItems ?? 0;
  const totalPages = state.data?.totalPages ?? 1;
  const page = state.data?.page ?? requestedPage;
  const pageSize = state.data?.pageSize ?? requestedPageSize;

  const updateParams = useCallback(
    (updates: Record<string, string | null>) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          for (const [key, value] of Object.entries(updates)) {
            if (value === null || value === undefined) {
              next.delete(key);
            } else {
              next.set(key, value);
            }
          }
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  const setPage = useCallback(
    (newPage: number) => {
      updateParams({ page: String(Math.max(1, newPage)) });
    },
    [updateParams],
  );

  const setPageSize = useCallback(
    (newSize: number) => {
      updateParams({ pageSize: String(newSize), page: "1" });
    },
    [updateParams],
  );

  const setSort = useCallback(
    (column: string) => {
      if (sort === column) {
        const newDir = dir === "asc" ? "desc" : "asc";
        updateParams({ sort: column, dir: newDir, page: "1" });
      } else {
        updateParams({ sort: column, dir: "asc", page: "1" });
      }
    },
    [sort, dir, updateParams],
  );

  useEffect(() => {
    if (!state.data) {
      return;
    }
    const updates: Record<string, string | null> = {};
    if (state.data.page && state.data.page !== requestedPage) {
      updates.page = String(state.data.page);
    }
    if (state.data.pageSize && state.data.pageSize !== requestedPageSize) {
      updates.pageSize = String(state.data.pageSize);
    }
    if (Object.keys(updates).length > 0) {
      updateParams(updates);
    }
  }, [requestedPage, requestedPageSize, state.data, updateParams]);

  return {
    state,
    page,
    pageSize,
    totalItems,
    totalPages,
    sort,
    dir,
    setPage,
    setPageSize,
    setSort,
    pageSizeOptions: PAGE_SIZE_OPTIONS,
  };
}

function positiveInt(value: string | null, fallback: number): number {
  const parsed = Number.parseInt(value || "", 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}
