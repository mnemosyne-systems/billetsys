/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useMemo } from "react";
import DataState from "../components/common/DataState";
import PaginationControls from "../components/common/PaginationControls";
import SortableTableHead from "../components/common/SortableTableHead";
import PageHeader from "../components/layout/PageHeader";
import usePaginatedList from "../hooks/usePaginatedList";
import { shouldUseLightTextOnColor } from "../utils/formatting";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, TicketListItem } from "../types/domain";
import { Button } from "../components/ui/button";
import { useLocation, useNavigate } from "react-router-dom";
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

function isEditableTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) {
    return false;
  }
  const tagName = target.tagName.toLowerCase();
  return (
    target.isContentEditable ||
    tagName === "input" ||
    tagName === "textarea" ||
    tagName === "select" ||
    Boolean(target.closest("[contenteditable='true']"))
  );
}

export default function SupportTicketsPage({
  title,
  view,
  sessionState,
  apiBase = "/api/support/tickets",
  createFallbackPath = "/support/tickets/new",
}: SupportTicketsPageProps) {
  const location = useLocation();
  const navigate = useNavigate();
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

  useEffect(() => {
    if (shortcutTickets.length === 0) {
      return undefined;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (
        event.defaultPrevented ||
        !event.ctrlKey ||
        event.metaKey ||
        event.altKey ||
        event.shiftKey ||
        isEditableTarget(event.target)
      ) {
        return;
      }
      const shortcutIndex =
        event.key === "0" ? 10 : Number.parseInt(event.key, 10);
      if (
        Number.isNaN(shortcutIndex) ||
        shortcutIndex < 1 ||
        shortcutIndex > 10
      ) {
        return;
      }
      const ticket = shortcutTickets[shortcutIndex - 1];
      if (!ticket?.detailPath) {
        return;
      }
      event.preventDefault();
      navigate(ticket.detailPath);
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [navigate, shortcutTickets]);

  return (
    <div className="w-full mx-auto mt-2">
      <PageHeader
        title={pageTitle}
        actions={
          <div className="flex flex-wrap gap-2">
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
