/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useMemo, useState } from "react";
import DataState from "../components/common/DataState";
import PaginationControls from "../components/common/PaginationControls";
import SortableTableHead from "../components/common/SortableTableHead";
import PageHeader from "../components/layout/PageHeader";
import usePaginatedList from "../hooks/usePaginatedList";
import { shouldUseLightTextOnColor, toQueryString } from "../utils/formatting";
import useNumberShortcuts from "../hooks/useNumberShortcuts";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, TicketListItem } from "../types/domain";
import { Button } from "../components/ui/button";
import { useLocation } from "react-router-dom";
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
}: SupportTicketsPageProps) {
  const location = useLocation();
  const locationSearchTerm =
    new URLSearchParams(location.search).get("q") || "";

  const extraParams = useMemo(
    () => ({
      view: view !== "assigned" ? view : undefined,
      q: locationSearchTerm || undefined,
    }),
    [view, locationSearchTerm],
  );

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
    apiUrl: apiBase,
    extraParams,
    defaultPageSize,
    defaultSort: "name",
    defaultDir: "asc",
    sortKeys: SORT_KEYS,
  });

  const currentView = ticketsState.data?.view || view || "assigned";
  const activeSearch = ticketsState.data?.searchTerm || locationSearchTerm;
  const showLevelColumn = apiBase !== "/api/user/tickets";
  const showCreateButton = !(
    apiBase === "/api/user/tickets" && currentView === "closed"
  );
  const pageTitle =
    title ||
    (currentView === "open"
      ? "Open tickets"
      : currentView === "closed"
        ? "Closed tickets"
        : "Active tickets");
  const emptyMessage = activeSearch
    ? `No tickets matched "${activeSearch}".`
    : "No tickets";

  const shortcutTickets = (ticketsState.data?.items || []).slice(0, 10);

  useNumberShortcuts({
    items: shortcutTickets,
    getPath: (ticket) => ticket?.detailPath,
  });

  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);
  const urlQuery = toQueryString({
    view: currentView || undefined,
    sort: sort || undefined,
    dir: dir || undefined,
  });
  const exportUrl = useMemo(
    () => `${apiBase}/export${urlQuery}`,
    [apiBase, urlQuery],
  );

  const exportHandler = async () => {
    setExporting(true);
    setExportError(null);
    try {
      const res = await fetch(exportUrl, {
        credentials: "same-origin",
        headers: { "X-Billetsys-Client": "react" },
      });
      if (!res.ok) {
        throw new Error(
          res.status === 401
            ? "You need to sign in again."
            : "Unable to export.",
        );
      }
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "tickets-" + currentView + ".csv";
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (error) {
      setExportError(
        error instanceof Error ? error.message : "Unable to export tickets.",
      );
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="w-full mx-auto mt-2">
      <PageHeader
        title={pageTitle}
        actions={
          <div className="flex flex-wrap gap-2">
            <Button
              variant="outline"
              onClick={exportHandler}
              disabled={exporting || totalItems === 0}
            >
              {exporting ? "Exporting..." : "Export CSV"}
            </Button>
            {showCreateButton ? (
              <Button asChild>
                <SmartLink
                  href={ticketsState.data?.createPath || createFallbackPath}
                >
                  Create
                </SmartLink>
              </Button>
            ) : null}
          </div>
        }
      />

      {exportError ? (
        <p className="px-2 pt-2 text-sm font-medium text-destructive">
          {exportError}
        </p>
      ) : null}

      <div className="w-full">
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
