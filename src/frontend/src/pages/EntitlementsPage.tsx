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
import { SmartLink } from "../utils/routing";
import useNumberShortcuts from "../hooks/useNumberShortcuts";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, EntitlementRecord } from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import PageHeader from "../components/layout/PageHeader";

type SupportLevel = NonNullable<EntitlementRecord["supportLevels"]>[number];

function supportLevelLabel(level: SupportLevel): string {
  return typeof level === "string" ? level : level.name || "Unnamed level";
}

function sortedSupportLevels(
  supportLevels: EntitlementRecord["supportLevels"],
): SupportLevel[] {
  return [...(supportLevels || [])].sort((left, right) =>
    supportLevelLabel(left).localeCompare(supportLevelLabel(right), undefined, {
      sensitivity: "base",
    }),
  );
}

const SORT_OPTIONS = [{ key: "name", label: "Name" }];

const SORT_CONFIGS = [
  {
    key: "name",
    comparator: (a: EntitlementRecord, b: EntitlementRecord) =>
      (a.name || "").localeCompare(b.name || "", undefined, {
        sensitivity: "base",
      }),
  },
];

export default function EntitlementsPage({ sessionState }: SessionPageProps) {
  const entitlementsState =
    useJson<CollectionResponse<EntitlementRecord>>("/api/entitlements");

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
  } = useClientPagination<EntitlementRecord>({
    items: entitlementsState.data?.items || [],
    defaultPageSize,
    sortConfigs: SORT_CONFIGS,
    defaultSort: "name",
  });

  useNumberShortcuts({
    items: pageItems.slice(0, 10),
    getPath: (entitlement) => `/entitlements/${entitlement.id}`,
  });

  return (
    <section className="w-full mt-4">
      <PageHeader
        title="Entitlements"
        actions={
          <Button asChild>
            <SmartLink href="/entitlements/new">Create</SmartLink>
          </Button>
        }
      />

      <DataState
        state={entitlementsState}
        emptyMessage="No entitlements are available yet."
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
          {pageItems.map((entitlement: EntitlementRecord) => (
            <Card
              key={entitlement.id}
              className="hover:shadow-md transition-shadow"
            >
              <CardHeader>
                <h3 className="font-semibold leading-none tracking-tight">
                  <Link
                    className="text-[var(--color-header-bg)] hover:underline hover:opacity-80"
                    to={`/entitlements/${entitlement.id}`}
                  >
                    {entitlement.name}
                  </Link>
                </h3>
                <p className="text-sm text-muted-foreground mt-2 line-clamp-2">
                  {entitlement.descriptionPreview || "No description"}
                </p>
                <div className="flex flex-wrap gap-2 mt-3">
                  {sortedSupportLevels(entitlement.supportLevels).map(
                    (level) => (
                      <Badge
                        variant="secondary"
                        className="font-normal"
                        key={
                          typeof level === "string" ? level : String(level.id)
                        }
                      >
                        {supportLevelLabel(level)}
                      </Badge>
                    ),
                  )}
                  {(!entitlement.supportLevels ||
                    entitlement.supportLevels.length === 0) && (
                    <span className="text-sm text-muted-foreground">
                      No levels
                    </span>
                  )}
                </div>
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
