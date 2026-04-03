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

export default function TicketWorkbenchPage({
  sessionState,
}: SessionPageProps) {
  const ticketState = useJson<CollectionResponse<TicketWorkbenchListItem>>(
    "/api/ticket-workbench",
  );
  const tickets = ticketState.data;

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>{tickets?.title || "Tickets"}</h2>
        </div>
        <div className="button-row">
          {tickets?.createPath && (
            <SmartLink className="primary-button" href={tickets.createPath}>
              Create
            </SmartLink>
          )}
        </div>
      </div>

      <DataState
        state={ticketState}
        emptyMessage="No tickets are available."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        <div className="category-list">
          {(tickets?.items || []).map((ticket: TicketWorkbenchListItem) => (
            <article key={ticket.id} className="category-card">
              <div className="category-card-head">
                <div>
                  <div className="category-title-row">
                    <h3>{ticket.name}</h3>
                    <span className="status-pill">
                      {ticket.status || "No status"}
                    </span>
                  </div>
                  <p className="tag-copy">
                    {ticket.companyName || "No company"}
                  </p>
                  <p className="muted-text">
                    {ticket.requesterName || "No requester"} •{" "}
                    {ticket.categoryName || "No category"}
                  </p>
                  <p className="muted-text">
                    Latest message: {ticket.lastMessageLabel || "-"}
                  </p>
                  {ticket.externalIssueLink && (
                    <p className="muted-text">
                      <a
                        href={ticket.externalIssueLink}
                        target="_blank"
                        rel="noreferrer"
                      >
                        External issue
                      </a>
                    </p>
                  )}
                </div>
                <div className="button-row">
                  <SmartLink className="inline-link" href={ticket.detailPath}>
                    Open
                  </SmartLink>
                  <SmartLink className="inline-link" href={ticket.editPath}>
                    Edit
                  </SmartLink>
                </div>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </section>
  );
}
