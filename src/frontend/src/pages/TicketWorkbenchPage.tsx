/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import DataState from "../components/common/DataState";
import PaginationControls from "../components/common/PaginationControls";
import SortDropdown from "../components/common/SortDropdown";
import PageHeader from "../components/layout/PageHeader";
import useJson from "../hooks/useJson";
import useClientPagination from "../hooks/useClientPagination";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  CollectionResponse,
  TicketWorkbenchListItem,
} from "../types/domain";
import { Card, CardContent } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";

const SORT_OPTIONS = [
  { key: "name", label: "Name" },
  { key: "status", label: "Status" },
  { key: "company", label: "Company" },
  { key: "category", label: "Category" },
];

const SORT_CONFIGS = [
  {
    key: "name",
    comparator: (a: TicketWorkbenchListItem, b: TicketWorkbenchListItem) =>
      (a.name || "").localeCompare(b.name || "", undefined, {
        sensitivity: "base",
      }),
  },
  {
    key: "status",
    comparator: (a: TicketWorkbenchListItem, b: TicketWorkbenchListItem) =>
      (a.status || "").localeCompare(b.status || "", undefined, {
        sensitivity: "base",
      }),
  },
  {
    key: "company",
    comparator: (a: TicketWorkbenchListItem, b: TicketWorkbenchListItem) =>
      (a.companyName || "").localeCompare(b.companyName || "", undefined, {
        sensitivity: "base",
      }),
  },
  {
    key: "category",
    comparator: (a: TicketWorkbenchListItem, b: TicketWorkbenchListItem) =>
      (a.categoryName || "").localeCompare(b.categoryName || "", undefined, {
        sensitivity: "base",
      }),
  },
];

export default function TicketWorkbenchPage({
  sessionState,
}: SessionPageProps) {
  const ticketState = useJson<CollectionResponse<TicketWorkbenchListItem>>(
    "/api/ticket-workbench",
  );
  const tickets = ticketState.data;

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
  } = useClientPagination<TicketWorkbenchListItem>({
    items: tickets?.items || [],
    defaultPageSize,
    sortConfigs: SORT_CONFIGS,
    defaultSort: "name",
  });

  return (
    <section className="w-full mt-4">
      <PageHeader
        title={tickets?.title || "Tickets"}
        actions={
          <div className="flex flex-wrap gap-2">
            {tickets?.createPath ? (
              <Button asChild>
                <SmartLink href={tickets.createPath}>Create</SmartLink>
              </Button>
            ) : null}
          </div>
        }
      />

      <DataState state={ticketState} emptyMessage="No tickets are available.">
        <div className="flex items-center justify-between mb-4">
          <SortDropdown
            options={SORT_OPTIONS}
            currentSort={sort}
            currentDir={dir}
            onSort={setSort}
          />
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {pageItems.map((ticket: TicketWorkbenchListItem) => (
            <Card key={ticket.id} className="hover:shadow-md transition-shadow">
              <CardContent className="flex flex-col h-full justify-between p-6">
                <div>
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <h3 className="font-semibold leading-tight tracking-tight">
                      {ticket.name}
                    </h3>
                    <Badge
                      variant="secondary"
                      className="font-normal whitespace-nowrap"
                    >
                      {ticket.status || "No status"}
                    </Badge>
                  </div>
                  <p className="text-sm font-medium mb-1">
                    {ticket.companyName || "No company"}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {ticket.requesterName || "No requester"} •{" "}
                    {ticket.categoryName || "No category"}
                  </p>
                  <p className="text-sm text-muted-foreground mt-1">
                    Latest message: {ticket.lastMessageLabel || "-"}
                  </p>
                  {ticket.externalIssueLink && (
                    <p className="text-sm text-muted-foreground mt-1">
                      <a
                        href={ticket.externalIssueLink}
                        target="_blank"
                        rel="noreferrer"
                        className="text-primary hover:underline hover:text-primary/80"
                      >
                        External issue
                      </a>
                    </p>
                  )}
                </div>
                <div className="flex gap-4 justify-end mt-4 pt-4">
                  <SmartLink
                    className="text-sm font-medium text-primary hover:underline hover:text-primary/80"
                    href={ticket.detailPath}
                  >
                    Open
                  </SmartLink>
                  <SmartLink
                    className="text-sm font-medium text-primary hover:underline hover:text-primary/80"
                    href={ticket.editPath}
                  >
                    Edit
                  </SmartLink>
                </div>
              </CardContent>
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
