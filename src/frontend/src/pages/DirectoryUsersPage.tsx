/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useLocation, useNavigate } from "react-router-dom";
import DataState from "../components/common/DataState";
import PaginationControls from "../components/common/PaginationControls";
import SortDropdown from "../components/common/SortDropdown";
import PageHeader from "../components/layout/PageHeader";
import useNumberShortcuts from "../hooks/useNumberShortcuts";
import useJson from "../hooks/useJson";
import useClientPagination from "../hooks/useClientPagination";
import { toQueryString } from "../utils/formatting";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  DirectoryUserRecord,
  DirectoryUsersResponse,
  NamedEntity,
} from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";

interface DirectoryUsersPageProps extends SessionPageProps {
  apiBase: string;
  basePath: string;
  titleFallback: string;
  description?: string;
}

const SORT_OPTIONS = [
  { key: "displayName", label: "Name" },
  { key: "email", label: "Email" },
  { key: "type", label: "Type" },
];

const SORT_CONFIGS = [
  {
    key: "displayName",
    comparator: (a: DirectoryUserRecord, b: DirectoryUserRecord) =>
      (a.displayName || a.username || "").localeCompare(
        b.displayName || b.username || "",
        undefined,
        { sensitivity: "base" },
      ),
  },
  {
    key: "email",
    comparator: (a: DirectoryUserRecord, b: DirectoryUserRecord) =>
      (a.email || "").localeCompare(b.email || "", undefined, {
        sensitivity: "base",
      }),
  },
  {
    key: "type",
    comparator: (a: DirectoryUserRecord, b: DirectoryUserRecord) =>
      (a.type || "").localeCompare(b.type || "", undefined, {
        sensitivity: "base",
      }),
  },
];

export default function DirectoryUsersPage({
  apiBase,
  titleFallback,
  description = "",
  sessionState,
}: DirectoryUsersPageProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const companyId = new URLSearchParams(location.search).get("companyId") || "";
  const dataState = useJson<DirectoryUsersResponse>(
    `${apiBase}${toQueryString({ companyId })}`,
  );
  const directory = dataState.data;

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
  } = useClientPagination<DirectoryUserRecord>({
    items: directory?.items || [],
    defaultPageSize,
    sortConfigs: SORT_CONFIGS,
    defaultSort: "displayName",
  });

  const selectCompany = (nextCompanyId: string) => {
    navigate(
      `${location.pathname}${toQueryString({ companyId: nextCompanyId })}`,
    );
  };

  useNumberShortcuts({
    items: pageItems.slice(0, 10),
    getPath: (user) => user.detailPath,
  });

  return (
    <section className="w-full mt-4">
      <PageHeader
        title={directory?.title || titleFallback}
        subtitle={directory?.description || description}
        actions={
          directory?.createPath ? (
            <Button asChild>
              <SmartLink href={directory.createPath}>Create</SmartLink>
            </Button>
          ) : null
        }
      />

      <DataState state={dataState} emptyMessage="No users are available.">
        <div className="grid gap-6">
          <div className="flex flex-wrap items-end gap-4">
            {directory?.showCompanySelector && (
              <div className="grid gap-1.5 max-w-xs">
                <label className="text-sm font-medium leading-none text-[var(--color-header-bg)]">
                  Company
                </label>
                <Select
                  value={
                    directory.selectedCompanyId !== null &&
                    directory.selectedCompanyId !== undefined
                      ? String(directory.selectedCompanyId)
                      : undefined
                  }
                  onValueChange={selectCompany}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Select company" />
                  </SelectTrigger>
                  <SelectContent position="popper">
                    {(directory.companies || []).map((company: NamedEntity) => (
                      <SelectItem
                        key={company.id}
                        value={
                          company.id !== null && company.id !== undefined
                            ? String(company.id)
                            : "none"
                        }
                      >
                        {company.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            <SortDropdown
              options={SORT_OPTIONS}
              currentSort={sort}
              currentDir={dir}
              onSort={setSort}
            />
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {pageItems.map((user: DirectoryUserRecord) => (
              <Card key={user.id} className="hover:shadow-md transition-shadow">
                <CardHeader>
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-semibold leading-none tracking-tight text-[var(--color-header-bg)]">
                      {user.detailPath ? (
                        <SmartLink
                          className="text-[var(--color-header-bg)] hover:underline hover:opacity-80"
                          href={user.detailPath}
                        >
                          {user.displayName ||
                            user.fullName ||
                            user.username ||
                            "User"}
                        </SmartLink>
                      ) : (
                        user.displayName ||
                        user.fullName ||
                        user.username ||
                        "User"
                      )}
                    </h3>
                    <Badge
                      variant="secondary"
                      className="whitespace-nowrap font-normal"
                    >
                      {user.typeLabel || user.type || "User"}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground mt-3 line-clamp-2">
                    {user.email || "No email"}
                  </p>
                  <p className="text-sm text-muted-foreground mt-1">
                    @{user.username || "unknown"}
                  </p>
                </CardHeader>
              </Card>
            ))}
          </div>

          {(!directory?.items || directory.items.length === 0) && (
            <p className="text-muted-foreground">
              No users are available for the selected company.
            </p>
          )}

          <PaginationControls
            page={page}
            pageSize={pageSize}
            totalItems={totalItems}
            totalPages={totalPages}
            pageSizeOptions={pageSizeOptions}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </div>
      </DataState>
    </section>
  );
}
