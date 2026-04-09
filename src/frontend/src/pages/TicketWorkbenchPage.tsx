/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import DataState from "../components/common/DataState";
import useJson from "../hooks/useJson";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  CollectionResponse,
  TicketWorkbenchListItem,
} from "../types/domain";
import { Card, CardContent } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";

export default function TicketWorkbenchPage({
  sessionState,
}: SessionPageProps) {
  const ticketState = useJson<CollectionResponse<TicketWorkbenchListItem>>(
    "/api/ticket-workbench",
  );
  const tickets = ticketState.data;

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <div className="flex flex-row items-center justify-between pb-6 px-1">
        <h2 className="text-3xl font-bold tracking-tight">
          {tickets?.title || "Tickets"}
        </h2>
        <div>
          {tickets?.createPath && (
            <Button asChild>
              <SmartLink href={tickets.createPath}>Create</SmartLink>
            </Button>
          )}
        </div>
      </div>

      <DataState
        state={ticketState}
        emptyMessage="No tickets are available."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {(tickets?.items || []).map((ticket: TicketWorkbenchListItem) => (
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
                <div className="flex gap-4 justify-end mt-4 pt-4 border-t">
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
      </DataState>
    </section>
  );
}
