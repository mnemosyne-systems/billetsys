/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import DataState from '../components/common/DataState';
import useJson from '../hooks/useJson';
import { isWhiteColorValue, toQueryString } from '../utils/formatting';
import { SmartLink } from '../utils/routing';
import type { SessionPageProps } from '../types/app';
import type { CollectionResponse, TicketListItem } from '../types/domain';

interface SupportTicketsPageProps extends SessionPageProps {
  view: 'assigned' | 'open' | 'closed';
  apiBase?: string;
  createFallbackPath?: string;
}

interface TicketListResponse extends CollectionResponse<TicketListItem> {
  view?: 'assigned' | 'open' | 'closed';
}

export default function SupportTicketsPage({
  sessionState,
  view,
  apiBase = '/api/support/tickets',
  createFallbackPath = '/support/tickets/new'
}: SupportTicketsPageProps) {
  const query = view && view !== 'assigned' ? toQueryString({ view }) : '';
  const ticketsState = useJson<TicketListResponse>(`${apiBase}${query}`);
  const currentView = ticketsState.data?.view || view || 'assigned';
  const showLevelColumn = apiBase !== '/api/user/tickets';
  const showCreateButton = !(apiBase === '/api/user/tickets' && currentView === 'closed');

  return (
    <section className="panel">
      {showCreateButton && (
        <div className="button-row support-ticket-actions">
          <SmartLink className="primary-button" href={ticketsState.data?.createPath || createFallbackPath}>
            Create
          </SmartLink>
        </div>
      )}

      <DataState state={ticketsState} emptyMessage="No tickets are available in this queue." signInHref={sessionState.data?.homePath || '/login'}>
        <div className="ticket-table-wrap">
          <table className="support-ticket-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Date</th>
                <th>Status</th>
                <th>Category</th>
                <th>Support</th>
                <th>Company</th>
                <th>Entitlement</th>
                {showLevelColumn && <th>Level</th>}
                <th>Affects</th>
                {currentView === 'closed' && <th>Resolved</th>}
              </tr>
            </thead>
            <tbody>
              {(ticketsState.data?.items || []).map((ticket: TicketListItem) => {
                const useLightText = ticket.slaColor && !isWhiteColorValue(ticket.slaColor);
                return (
                  <tr
                    key={ticket.id}
                    className={useLightText ? 'ticket-row-highlight' : undefined}
                    style={ticket.slaColor ? { backgroundColor: ticket.slaColor, color: useLightText ? '#ffffff' : undefined } : undefined}
                  >
                    <td>
                      <SmartLink className="inline-link" href={ticket.detailPath}>
                        {ticket.name}
                      </SmartLink>
                      {ticket.messageDirectionArrow && <span className="ticket-direction">{ticket.messageDirectionArrow}</span>}
                    </td>
                    <td>{ticket.messageDateLabel || '-'}</td>
                    <td>{ticket.status || '-'}</td>
                    <td>{ticket.categoryName || '-'}</td>
                    <td>
                      {ticket.supportUser ? (
                        <a className="inline-link" href={ticket.supportUser.detailPath}>
                          {ticket.supportUser.displayName || ticket.supportUser.username}
                        </a>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td>
                      {ticket.companyPath ? (
                        <a className="inline-link" href={ticket.companyPath}>
                          {ticket.companyName}
                        </a>
                      ) : (
                        ticket.companyName || '—'
                      )}
                    </td>
                    <td>{ticket.entitlementName || '-'}</td>
                    {showLevelColumn && <td>{ticket.levelName || '-'}</td>}
                    <td>{ticket.affectsVersionName || '-'}</td>
                    {currentView === 'closed' && <td>{ticket.resolvedVersionName || '-'}</td>}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </DataState>
    </section>
  );
}

