/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Link } from "react-router-dom";
import useJson from "../hooks/useJson";
import useClientPagination from "../hooks/useClientPagination";
import DataState from "../components/common/DataState";
import PaginationControls from "../components/common/PaginationControls";
import SortDropdown from "../components/common/SortDropdown";
import PageHeader from "../components/layout/PageHeader";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { CategoryRecord, CollectionResponse } from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";

const SORT_OPTIONS = [{ key: "name", label: "Name" }];

const SORT_CONFIGS = [
  {
    key: "name",
    comparator: (a: CategoryRecord, b: CategoryRecord) =>
      (a.name || "").localeCompare(b.name || "", undefined, {
        sensitivity: "base",
      }),
  },
];

export default function CategoriesPage({ sessionState }: SessionPageProps) {
  const categoriesState =
    useJson<CollectionResponse<CategoryRecord>>("/api/categories");

  const defaultPageSize = sessionState.data?.defaultPageSize ?? undefined;

  const {
    pageItems,
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
  } = useClientPagination<CategoryRecord>({
    items: categoriesState.data?.items || [],
    defaultPageSize,
    sortConfigs: SORT_CONFIGS,
    defaultSort: "name",
  });

  return (
    <section className="w-full mt-4">
      <PageHeader
        title="Categories"
        actions={
          categoriesState.data?.canCreate ? (
            <Button asChild>
              <SmartLink href={categoriesState.data.createPath}>
                Create
              </SmartLink>
            </Button>
          ) : null
        }
      />

      <DataState
        state={categoriesState}
        emptyMessage="No categories are available yet."
      >
        <div className="flex items-center justify-between mb-4">
          <SortDropdown
            options={SORT_OPTIONS}
            currentSort={sort}
            currentDir={dir}
            onSort={setSort}
          />
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {pageItems.map((category: CategoryRecord) => (
            <Card
              key={category.id}
              className="hover:shadow-md transition-shadow"
            >
              <CardHeader>
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold leading-none tracking-tight">
                    <Link
                      className="text-[var(--color-header-bg)] hover:underline hover:opacity-80"
                      to={`/categories/${category.id}`}
                    >
                      {category.name}
                    </Link>
                  </h3>
                  {category.isDefault && (
                    <Badge
                      variant="secondary"
                      className="whitespace-nowrap font-normal"
                    >
                      Default
                    </Badge>
                  )}
                </div>
                <p className="text-sm text-muted-foreground mt-2 line-clamp-2">
                  {category.descriptionPreview || "No description"}
                </p>
              </CardHeader>
            </Card>
          ))}
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
    </section>
  );
}
