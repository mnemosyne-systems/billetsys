/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useMemo, useRef, useState } from "react";
import DataState from "../components/common/DataState";
import PaginationControls from "../components/common/PaginationControls";
import SortableTableHead from "../components/common/SortableTableHead";
import PageHeader from "../components/layout/PageHeader";
import AdvancedSearchFilters from "../components/tickets/AdvancedSearchFilters";
import useJson from "../hooks/useJson";
import usePaginatedList from "../hooks/usePaginatedList";
import { shouldUseLightTextOnColor } from "../utils/formatting";
import useNumberShortcuts from "../hooks/useNumberShortcuts";
import { SmartLink } from "../utils/routing";
import {
  advancedSearchChips,
  advancedSearchFilterSignature,
  emptyAdvancedSearchDraft,
  parseAdvancedSearch,
  removeAdvancedSearchChip,
  serializeAdvancedSearchParams,
  validateAdvancedSearch,
  type AdvancedSearchBootstrap,
  type AdvancedSearchDraft,
} from "../utils/advancedSearch";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, TicketListItem } from "../types/domain";
import { Button } from "../components/ui/button";
import { useLocation, useSearchParams } from "react-router-dom";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../components/ui/table";

interface SupportTicketsPageProps extends SessionPageProps {
  view: "assigned" | "open" | "closed";
  apiBase?: string;
  createFallbackPath?: string;
  title?: string;
  advancedSearch?: boolean;
}

interface TicketListResponse extends CollectionResponse<TicketListItem> {
  view?: "assigned" | "open" | "closed";
  searchTerm?: string;
}

const SORT_KEYS = [
  "name",
  "title",
  "date",
  "status",
  "category",
  "company",
  "entitlement",
  "level",
  "affects",
] as const;

export default function SupportTicketsPage({
  title,
  view,
  sessionState,
  apiBase = "/api/support/tickets",
  createFallbackPath = "/support/tickets/new",
  advancedSearch = false,
}: SupportTicketsPageProps) {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const locationSearchTerm =
    new URLSearchParams(location.search).get("q") || "";
  const role = sessionState.data?.role;

  const [bootstrapRefresh, setBootstrapRefresh] = useState(0);
  const bootstrapLoadedRef = useRef(false);
  const bootstrapState = useJson<AdvancedSearchBootstrap>(
    advancedSearch
      ? `${apiBase}/bootstrap?allVersions=true&refresh=${bootstrapRefresh}`
      : null,
    { keepPreviousData: true },
  );
  const bootstrap = bootstrapState.data;

  useEffect(() => {
    if (bootstrapState.data) {
      bootstrapLoadedRef.current = true;
    }
  }, [bootstrapState.data]);

  // Custom fields can be added while this page is open in another tab. Refresh
  // the lookups when the tab becomes visible again so the new fields appear.
  useEffect(() => {
    if (!advancedSearch) {
      return undefined;
    }
    const refresh = () => {
      if (bootstrapLoadedRef.current) {
        setBootstrapRefresh((value) => value + 1);
      }
    };
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        refresh();
      }
    };
    window.addEventListener("focus", refresh);
    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => {
      window.removeEventListener("focus", refresh);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [advancedSearch]);

  const customFields = useMemo(
    () =>
      (bootstrap?.customFields || []).filter((field) => field.active !== false),
    [bootstrap],
  );

  const [draft, setDraft] = useState<AdvancedSearchDraft>(() =>
    parseAdvancedSearch(searchParams, role),
  );
  const [validationErrors, setValidationErrors] = useState<
    Record<string, string>
  >({});
  const filterSignature = useMemo(
    () => advancedSearchFilterSignature(searchParams, role),
    [searchParams, role],
  );
  const [draftSignature, setDraftSignature] = useState(filterSignature);

  if (draftSignature !== filterSignature) {
    setDraftSignature(filterSignature);
    setDraft(parseAdvancedSearch(searchParams, role));
    setValidationErrors({});
  }

  const appliedDraft = useMemo(
    () => parseAdvancedSearch(searchParams, role),
    [searchParams, role],
  );
  const appliedParams = useMemo(
    () =>
      serializeAdvancedSearchParams(
        appliedDraft,
        role,
        bootstrap ? customFields : undefined,
      ),
    [appliedDraft, role, bootstrap, customFields],
  );

  const extraParams = useMemo(
    () =>
      advancedSearch
        ? appliedParams
        : {
            view: view !== "assigned" ? view : undefined,
            q: locationSearchTerm || undefined,
          },
    [advancedSearch, appliedParams, view, locationSearchTerm],
  );

  const chips = useMemo(
    () =>
      advancedSearch
        ? advancedSearchChips(draft, {
            companies: bootstrap?.companies,
            categories: bootstrap?.categories,
            versions: bootstrap?.versions,
            customFields,
          })
        : [],
    [advancedSearch, draft, bootstrap, customFields],
  );

  function applyDraft(nextDraft: AdvancedSearchDraft) {
    const next = new URLSearchParams(searchParams);
    const preserved = new Set(["page", "pageSize", "sort", "dir"]);
    for (const key of Array.from(next.keys())) {
      if (!preserved.has(key)) {
        next.delete(key);
      }
    }
    const serialized = serializeAdvancedSearchParams(
      nextDraft,
      role,
      customFields,
    );
    for (const [key, value] of Object.entries(serialized)) {
      next.set(key, value);
    }
    next.set("page", "1");
    setSearchParams(next);
  }

  function handleDraftChange(nextDraft: AdvancedSearchDraft) {
    setDraft(nextDraft);
    if (Object.keys(validationErrors).length > 0) {
      setValidationErrors({});
    }
  }

  function handleApply() {
    const errors = validateAdvancedSearch(draft, customFields);
    setValidationErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }
    applyDraft(draft);
  }

  function handleReset() {
    const empty = emptyAdvancedSearchDraft();
    setDraft(empty);
    setValidationErrors({});
    applyDraft(empty);
  }

  function handleRemoveChip(chipId: string) {
    const nextDraft = removeAdvancedSearchChip(draft, chipId);
    setDraft(nextDraft);
    setValidationErrors({});
    applyDraft(nextDraft);
  }

  const defaultPageSize = sessionState.data?.defaultPageSize ?? undefined;

  const {
    state: ticketsState,
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
  } = usePaginatedList<TicketListResponse>({
    apiUrl: advancedSearch ? `${apiBase}/search` : apiBase,
    extraParams,
    defaultPageSize,
    defaultSort: "name",
    defaultDir: "asc",
    sortKeys: SORT_KEYS,
  });

  const currentView = ticketsState.data?.view || view || "assigned";
  const activeSearch = ticketsState.data?.searchTerm || locationSearchTerm;
  const hasActiveAdvancedFilters = Object.keys(appliedParams).length > 0;
  const showLevelColumn = apiBase !== "/api/user/tickets";
  const showCreateButton =
    !advancedSearch &&
    !(apiBase === "/api/user/tickets" && currentView === "closed");
  const pageTitle =
    title ||
    (currentView === "open"
      ? "Open tickets"
      : currentView === "closed"
        ? "Closed tickets"
        : "Active tickets");
  const emptyMessage = advancedSearch
    ? hasActiveAdvancedFilters
      ? "No tickets matched your search."
      : "No tickets"
    : activeSearch
      ? `No tickets matched "${activeSearch}".`
      : "No tickets";

  const shortcutTickets = (ticketsState.data?.items || []).slice(0, 10);

  useNumberShortcuts({
    items: shortcutTickets,
    getPath: (ticket) => ticket?.detailPath,
  });

  return (
    <div className="w-full mx-auto mt-2">
      <PageHeader
        title={pageTitle}
        actions={
          showCreateButton ? (
            <div className="flex flex-wrap gap-2">
              <Button asChild>
                <SmartLink
                  href={ticketsState.data?.createPath || createFallbackPath}
                >
                  Create
                </SmartLink>
              </Button>
            </div>
          ) : undefined
        }
      />

      <div className="w-full">
        {advancedSearch ? (
          <AdvancedSearchFilters
            role={role}
            bootstrap={bootstrap || undefined}
            draft={draft}
            errors={validationErrors}
            chips={chips}
            onChange={handleDraftChange}
            onApply={handleApply}
            onReset={handleReset}
            onRemoveChip={handleRemoveChip}
          />
        ) : null}
        <DataState state={ticketsState} emptyMessage={emptyMessage}>
          <div className="max-w-full overflow-x-auto">
            <Table className="text-base">
              <TableHeader>
                <TableRow className="bg-muted/50 hover:bg-muted/50">
                  <SortableTableHead
                    columnKey="name"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                  >
                    Name
                  </SortableTableHead>
                  <SortableTableHead
                    columnKey="title"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                    className="min-w-[160px]"
                  >
                    Title
                  </SortableTableHead>
                  <SortableTableHead
                    columnKey="date"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                  >
                    Date
                  </SortableTableHead>
                  <SortableTableHead
                    columnKey="status"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                  >
                    Status
                  </SortableTableHead>
                  <SortableTableHead
                    columnKey="category"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                  >
                    Category
                  </SortableTableHead>
                  <TableHead className="whitespace-nowrap">Support</TableHead>
                  <SortableTableHead
                    columnKey="company"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                  >
                    Company
                  </SortableTableHead>
                  <SortableTableHead
                    columnKey="entitlement"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                  >
                    Entitlement
                  </SortableTableHead>
                  {showLevelColumn && (
                    <SortableTableHead
                      columnKey="level"
                      currentSort={sort}
                      currentDir={dir}
                      onSort={setSort}
                    >
                      Level
                    </SortableTableHead>
                  )}
                  <SortableTableHead
                    columnKey="affects"
                    currentSort={sort}
                    currentDir={dir}
                    onSort={setSort}
                  >
                    Affects
                  </SortableTableHead>
                  {currentView === "closed" && (
                    <TableHead className="whitespace-nowrap">
                      Resolved
                    </TableHead>
                  )}
                </TableRow>
              </TableHeader>
              <TableBody>
                {(ticketsState.data?.items || []).map(
                  (ticket: TicketListItem) => {
                    const useLightText = shouldUseLightTextOnColor(
                      ticket.slaColor,
                    );
                    const linkClass = useLightText
                      ? "text-white"
                      : ticket.slaColor
                        ? "text-[#111827]"
                        : "text-primary";
                    const secondaryClass = ticket.slaColor
                      ? ""
                      : "text-muted-foreground";
                    return (
                      <TableRow
                        key={ticket.id}
                        className={
                          useLightText
                            ? "hover:opacity-90 transition-opacity"
                            : ""
                        }
                        style={
                          ticket.slaColor
                            ? {
                                backgroundColor: ticket.slaColor,
                                color: useLightText ? "#ffffff" : "#111827",
                              }
                            : undefined
                        }
                      >
                        <TableCell className="font-medium py-3 px-4">
                          <div className="flex items-center gap-1.5 whitespace-nowrap">
                            <SmartLink
                              className={`font-semibold hover:underline ${linkClass}`}
                              href={ticket.detailPath}
                            >
                              {ticket.name}
                            </SmartLink>
                            {ticket.messageDirectionArrow && (
                              <span className="opacity-70 text-sm translate-y-px">
                                {ticket.messageDirectionArrow}
                              </span>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className={`py-3 px-4 ${secondaryClass}`}>
                          {ticket.title || "-"}
                        </TableCell>
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${secondaryClass}`}
                        >
                          {ticket.messageDateLabel || "-"}
                        </TableCell>
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${secondaryClass}`}
                        >
                          {ticket.status || "-"}
                        </TableCell>
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${secondaryClass}`}
                        >
                          {ticket.categoryName || "-"}
                        </TableCell>
                        <TableCell className="whitespace-nowrap py-3 px-4">
                          {ticket.supportUser ? (
                            <a
                              className={`hover:underline ${linkClass}`}
                              href={ticket.supportUser.detailPath}
                            >
                              {ticket.supportUser.displayName ||
                                ticket.supportUser.username}
                            </a>
                          ) : (
                            <span className={secondaryClass}>—</span>
                          )}
                        </TableCell>
                        <TableCell className="whitespace-nowrap py-3 px-4">
                          {ticket.companyPath ? (
                            <a
                              className={`hover:underline ${linkClass}`}
                              href={ticket.companyPath}
                            >
                              {ticket.companyName}
                            </a>
                          ) : (
                            <span className={secondaryClass}>
                              {ticket.companyName || "—"}
                            </span>
                          )}
                        </TableCell>
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${secondaryClass}`}
                        >
                          {ticket.entitlementName || "-"}
                        </TableCell>
                        {showLevelColumn && (
                          <TableCell
                            className={`whitespace-nowrap py-3 px-4 ${secondaryClass}`}
                          >
                            {ticket.levelName || "-"}
                          </TableCell>
                        )}
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${secondaryClass}`}
                        >
                          {ticket.affectsVersionName || "-"}
                        </TableCell>
                        {currentView === "closed" && (
                          <TableCell
                            className={`whitespace-nowrap py-3 px-4 ${secondaryClass}`}
                          >
                            {ticket.resolvedVersionName || "-"}
                          </TableCell>
                        )}
                      </TableRow>
                    );
                  },
                )}
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
