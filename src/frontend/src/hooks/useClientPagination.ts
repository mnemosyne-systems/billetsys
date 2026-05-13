/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useCallback, useEffect, useMemo } from "react";
import { useSearchParams } from "react-router-dom";

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 25, 50] as const;

export type SortDirection = "asc" | "desc";

export interface ClientPaginationState<T> {
  pageItems: T[];
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

interface SortConfig<T> {
  key: string;
  comparator: (a: T, b: T) => number;
}

interface UseClientPaginationOptions<T> {
  items: T[];
  defaultPageSize?: number;
  sortConfigs?: SortConfig<T>[];
  defaultSort?: string;
  defaultDir?: SortDirection;
}

export default function useClientPagination<T>(
  options: UseClientPaginationOptions<T>,
): ClientPaginationState<T> {
  const {
    items,
    defaultPageSize,
    sortConfigs = [],
    defaultSort,
    defaultDir = "asc",
  } = options;

  const effectiveDefaultPageSize = defaultPageSize || DEFAULT_PAGE_SIZE;

  const [searchParams, setSearchParams] = useSearchParams();

  const page = positiveInt(searchParams.get("page"), 1);
  const pageSize = positiveInt(
    searchParams.get("pageSize"),
    effectiveDefaultPageSize,
  );
  const sortKeys = useMemo(
    () => new Set(sortConfigs.map((config) => config.key)),
    [sortConfigs],
  );
  const requestedSort = searchParams.get("sort");
  const sort =
    requestedSort && sortKeys.has(requestedSort)
      ? requestedSort
      : defaultSort && sortKeys.has(defaultSort)
        ? defaultSort
        : null;
  const requestedDir = searchParams.get("dir");
  const dir: SortDirection =
    requestedDir === "asc" || requestedDir === "desc"
      ? requestedDir
      : defaultDir;

  const sortedItems = useMemo(() => {
    if (!sort || sortConfigs.length === 0) {
      return items;
    }
    const config = sortConfigs.find((c) => c.key === sort);
    if (!config) {
      return items;
    }
    const sorted = [...items].sort(config.comparator);
    return dir === "desc" ? sorted.reverse() : sorted;
  }, [items, sort, dir, sortConfigs]);

  const totalItems = sortedItems.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const safePage = Math.min(page, totalPages);

  const pageItems = useMemo(() => {
    const start = (safePage - 1) * pageSize;
    return sortedItems.slice(start, start + pageSize);
  }, [sortedItems, safePage, pageSize]);

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
    if (page !== safePage) {
      updateParams({ page: String(safePage) });
    }
  }, [page, safePage, updateParams]);

  return {
    pageItems,
    page: safePage,
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
