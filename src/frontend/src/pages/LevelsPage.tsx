/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Link } from "react-router-dom";
import DataState from "../components/common/DataState";
import PaginationControls from "../components/common/PaginationControls";
import SortDropdown from "../components/common/SortDropdown";
import { LevelColorBadge } from "../components/common/LevelColorBadge";
import PageHeader from "../components/layout/PageHeader";
import useJson from "../hooks/useJson";
import useClientPagination from "../hooks/useClientPagination";
import { SmartLink } from "../utils/routing";
import useNumberShortcuts from "../hooks/useNumberShortcuts";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, LevelRecord } from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";

const SORT_OPTIONS = [
  { key: "name", label: "Name" },
  { key: "level", label: "Level" },
];

const SORT_CONFIGS = [
  {
    key: "name",
    comparator: (a: LevelRecord, b: LevelRecord) =>
      (a.name || "").localeCompare(b.name || "", undefined, {
        sensitivity: "base",
      }),
  },
  {
    key: "level",
    comparator: (a: LevelRecord, b: LevelRecord) =>
      Number(a.level ?? 0) - Number(b.level ?? 0),
  },
];

export default function LevelsPage({ sessionState }: SessionPageProps) {
  const levelsState = useJson<CollectionResponse<LevelRecord>>("/api/levels");

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
  } = useClientPagination<LevelRecord>({
    items: levelsState.data?.items || [],
    defaultPageSize,
    sortConfigs: SORT_CONFIGS,
    defaultSort: "name",
  });

  useNumberShortcuts({
    items: pageItems.slice(0, 10),
    getPath: (level) => `/levels/${level.id}`,
  });

  return (
    <section className="w-full mt-4">
      <PageHeader
        title="Levels"
        actions={
          <Button asChild>
            <SmartLink href="/levels/new">Create</SmartLink>
          </Button>
        }
      />

      <DataState
        state={levelsState}
        emptyMessage="No levels are available yet."
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
          {pageItems.map((level: LevelRecord) => (
            <Card key={level.id} className="hover:shadow-md transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold leading-none tracking-tight">
                    <Link
                      className="text-[var(--color-header-bg)] hover:underline hover:opacity-80"
                      to={`/levels/${level.id}`}
                    >
                      {level.name}
                    </Link>
                  </h3>
                  <LevelColorBadge
                    color={level.color}
                    display={level.colorDisplay}
                  />
                </div>
                <p className="text-sm text-muted-foreground mt-3 line-clamp-2">
                  {level.descriptionPreview || "No description"}
                </p>
                <p className="text-sm text-muted-foreground mt-2">
                  Level {level.level} • {level.fromLabel} - {level.toLabel}
                </p>
                <p className="text-sm text-muted-foreground mt-1">
                  {level.countryName || "No country"} •{" "}
                  {level.timezoneName || "No time zone"}
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
