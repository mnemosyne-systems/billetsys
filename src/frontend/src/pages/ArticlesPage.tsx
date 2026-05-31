/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useLocation } from "react-router-dom";
import DataState from "../components/common/DataState";
import PaginationControls from "../components/common/PaginationControls";
import SortableTableHead from "../components/common/SortableTableHead";
import PageHeader from "../components/layout/PageHeader";
import usePaginatedList from "../hooks/usePaginatedList";
import useNumberShortcuts from "../hooks/useNumberShortcuts";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { ArticleRecord, CollectionResponse } from "../types/domain";
import { Button } from "../components/ui/button";
import { useMemo } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHeader,
  TableRow,
} from "../components/ui/table";

const SORT_KEYS = ["title", "tags"] as const;

export default function ArticlesPage({ sessionState }: SessionPageProps) {
  const defaultPageSize = sessionState.data?.defaultPageSize ?? undefined;
  const location = useLocation();
  const locationSearchTerm =
    new URLSearchParams(location.search).get("q") || "";

  const extraParams = useMemo(
    () => ({
      q: locationSearchTerm || undefined,
    }),
    [locationSearchTerm],
  );

  const {
    state: articlesState,
    page,
    pageSize,
    totalItems,
    totalPages,
    sort,
    dir,
    setPage,
    setPageSize,
    setSort,
    pageSizeOptions,
  } = usePaginatedList<CollectionResponse<ArticleRecord>>({
    apiUrl: "/api/articles",
    extraParams,
    defaultPageSize,
    defaultSort: "title",
    defaultDir: "asc",
    sortKeys: SORT_KEYS,
  });

  useNumberShortcuts({
    items: (articlesState.data?.items || []).slice(0, 10),
    getPath: (article) => `/articles/${article.id}`,
  });

  const emptyMessage = locationSearchTerm
    ? `No articles matched "${locationSearchTerm}".`
    : "No articles are available yet.";

  return (
    <div className="w-full mx-auto mt-2">
      <PageHeader
        title="Articles"
        actions={
          articlesState.data?.canCreate ? (
            <Button asChild>
              <SmartLink href={articlesState.data.createPath}>Create</SmartLink>
            </Button>
          ) : null
        }
      />

      <div className="w-full">
        <DataState state={articlesState} emptyMessage={emptyMessage}>
          <div className="max-w-full overflow-x-auto">
            <Table className="text-base">
              <TableHeader>
                <TableRow className="bg-muted/50 hover:bg-muted/50">
                  <SortableTableHead
                    columnKey="title"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                    className="min-w-[240px]"
                  >
                    Title
                  </SortableTableHead>
                  <SortableTableHead
                    columnKey="tags"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                    className="min-w-[180px]"
                  >
                    Tags
                  </SortableTableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(articlesState.data?.items || []).map((article) => (
                  <TableRow key={article.id}>
                    <TableCell className="py-3 px-4 font-medium">
                      <SmartLink
                        className="font-semibold text-[#b00020] hover:text-[#b00020] hover:underline transition-colors"
                        href={`/articles/${article.id}`}
                      >
                        {article.title}
                      </SmartLink>
                    </TableCell>
                    <TableCell className="py-3 px-4 text-muted-foreground">
                      {article.tags || "—"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <PaginationControls
            page={page}
            pageSize={pageSize}
            totalItems={totalItems}
            totalPages={totalPages}
            pageSizeOptions={pageSizeOptions}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </DataState>
      </div>
    </div>
  );
}
