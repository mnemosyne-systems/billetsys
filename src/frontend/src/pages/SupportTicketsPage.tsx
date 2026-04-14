/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import useJson from "../hooks/useJson";
import { isWhiteColorValue, toQueryString } from "../utils/formatting";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, TicketListItem } from "../types/domain";
import { Button } from "../components/ui/button";
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
}

interface TicketListResponse extends CollectionResponse<TicketListItem> {
  view?: "assigned" | "open" | "closed";
}

export default function SupportTicketsPage({
  view,
  apiBase = "/api/support/tickets",
  createFallbackPath = "/support/tickets/new",
}: SupportTicketsPageProps) {
  const query = view && view !== "assigned" ? toQueryString({ view }) : "";
  const ticketsState = useJson<TicketListResponse>(`${apiBase}${query}`);
  const currentView = ticketsState.data?.view || view || "assigned";
  const showLevelColumn = apiBase !== "/api/user/tickets";
  const showCreateButton = !(
    apiBase === "/api/user/tickets" && currentView === "closed"
  );
  const pageTitle =
    currentView === "open"
      ? "Open tickets"
      : currentView === "closed"
        ? "Closed tickets"
        : "Active tickets";

  return (
    <div className="w-full mx-auto mt-2">
      <PageHeader
        title={pageTitle}
        actions={
          showCreateButton ? (
            <Button asChild>
              <SmartLink
                href={ticketsState.data?.createPath || createFallbackPath}
              >
                Create
              </SmartLink>
            </Button>
          ) : null
        }
      />

      <div className="w-full">
        <DataState state={ticketsState} emptyMessage="No tickets">
          <div className="max-w-full overflow-x-auto">
            <Table className="text-base">
              <TableHeader>
                <TableRow className="bg-muted/50 hover:bg-muted/50">
                  <TableHead className="whitespace-nowrap">Name</TableHead>
                  <TableHead className="min-w-[160px]">Title</TableHead>
                  <TableHead className="whitespace-nowrap">Date</TableHead>
                  <TableHead className="whitespace-nowrap">Status</TableHead>
                  <TableHead className="whitespace-nowrap">Category</TableHead>
                  <TableHead className="whitespace-nowrap">Support</TableHead>
                  <TableHead className="whitespace-nowrap">Company</TableHead>
                  <TableHead className="whitespace-nowrap">
                    Entitlement
                  </TableHead>
                  {showLevelColumn && (
                    <TableHead className="whitespace-nowrap">Level</TableHead>
                  )}
                  <TableHead className="whitespace-nowrap">Affects</TableHead>
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
                    const useLightText =
                      ticket.slaColor && !isWhiteColorValue(ticket.slaColor);
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
                                color: useLightText ? "#ffffff" : undefined,
                              }
                            : undefined
                        }
                      >
                        <TableCell className="font-medium py-3 px-4">
                          <div className="flex items-center gap-1.5 whitespace-nowrap">
                            <SmartLink
                              className={`font-semibold hover:underline ${useLightText ? "text-white" : "text-primary"}`}
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
                        <TableCell
                          className={`py-3 px-4 ${useLightText ? "" : "text-muted-foreground"}`}
                        >
                          {ticket.title || "-"}
                        </TableCell>
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${useLightText ? "" : "text-muted-foreground"}`}
                        >
                          {ticket.messageDateLabel || "-"}
                        </TableCell>
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${useLightText ? "" : "text-muted-foreground"}`}
                        >
                          {ticket.status || "-"}
                        </TableCell>
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${useLightText ? "" : "text-muted-foreground"}`}
                        >
                          {ticket.categoryName || "-"}
                        </TableCell>
                        <TableCell className="whitespace-nowrap py-3 px-4">
                          {ticket.supportUser ? (
                            <a
                              className={`hover:underline ${useLightText ? "text-white" : "text-primary"}`}
                              href={ticket.supportUser.detailPath}
                            >
                              {ticket.supportUser.displayName ||
                                ticket.supportUser.username}
                            </a>
                          ) : (
                            <span
                              className={
                                useLightText ? "" : "text-muted-foreground"
                              }
                            >
                              —
                            </span>
                          )}
                        </TableCell>
                        <TableCell className="whitespace-nowrap py-3 px-4">
                          {ticket.companyPath ? (
                            <a
                              className={`hover:underline ${useLightText ? "text-white" : "text-primary"}`}
                              href={ticket.companyPath}
                            >
                              {ticket.companyName}
                            </a>
                          ) : (
                            <span
                              className={
                                useLightText ? "" : "text-muted-foreground"
                              }
                            >
                              {ticket.companyName || "—"}
                            </span>
                          )}
                        </TableCell>
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${useLightText ? "" : "text-muted-foreground"}`}
                        >
                          {ticket.entitlementName || "-"}
                        </TableCell>
                        {showLevelColumn && (
                          <TableCell
                            className={`whitespace-nowrap py-3 px-4 ${useLightText ? "" : "text-muted-foreground"}`}
                          >
                            {ticket.levelName || "-"}
                          </TableCell>
                        )}
                        <TableCell
                          className={`whitespace-nowrap py-3 px-4 ${useLightText ? "" : "text-muted-foreground"}`}
                        >
                          {ticket.affectsVersionName || "-"}
                        </TableCell>
                        {currentView === "closed" && (
                          <TableCell
                            className={`whitespace-nowrap py-3 px-4 ${useLightText ? "" : "text-muted-foreground"}`}
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
        </DataState>
      </div>
    </div>
  );
}
