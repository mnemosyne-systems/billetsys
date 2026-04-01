/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { MouseEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import useJson from '../../hooks/useJson';
import useText from '../../hooks/useText';
import { SmartLink, normalizeClientPath } from '../../utils/routing';
import {
  headerNavigation,
  ticketCountsApiPath,
  ticketLabelForRole,
  isRoleTicketRoute,
  showRoleTicketAlarm,
  rssPath
} from '../../utils/navigation';
import type { Session } from '../../types/app';

interface TicketCounts {
  assignedCount?: number;
  openCount?: number;
}

interface AuthenticatedHeaderProps {
  session: Session | null;
}

export default function AuthenticatedHeader({ session }: AuthenticatedHeaderProps) {
  const [now, setNow] = useState(() => new Date());
  const location = useLocation();
  const ticketMenuRef = useRef<HTMLDetailsElement | null>(null);
  const profileMenuRef = useRef<HTMLDetailsElement | null>(null);
  const role = session?.role;
  const showTicketMenu = role === 'support' || role === 'user';
  const ticketMenuBasePath = role === 'support' ? '/support/tickets' : '/user/tickets';
  const ticketCountsState = useJson<TicketCounts>(ticketCountsApiPath(role));
  const ticketAlarmState = useText(showRoleTicketAlarm(role) ? '/tickets/alarm/status' : '');

  useEffect(() => {
    const timerId = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timerId);
  }, []);

  const navigation = headerNavigation(session);
  const brandHref = normalizeClientPath(session?.homePath) || '/';
  const userName = session?.displayName || session?.username || 'Guest';
  const assignedCount = ticketCountsState.data?.assignedCount ?? 0;
  const openCount = ticketCountsState.data?.openCount ?? 0;
  const ticketLabel = ticketLabelForRole(role, assignedCount, openCount);
  const showTicketAlarm = String(ticketAlarmState.data || '').trim().toLowerCase() === 'true';
  const isTicketRoute = isRoleTicketRoute(role, location.pathname);
  const rssHref = rssPath(role);
  const closeDetailsMenu = (event: MouseEvent<HTMLElement>) => {
    const menu = event.currentTarget.closest('details');
    if (menu) {
      menu.open = false;
    }
  };

  useEffect(() => {
    if (ticketMenuRef.current) {
      ticketMenuRef.current.open = false;
    }
    if (profileMenuRef.current) {
      profileMenuRef.current.open = false;
    }
  }, [location.pathname, location.search]);

  useEffect(() => {
    const handlePointerDown = (event: PointerEvent) => {
      [ticketMenuRef.current, profileMenuRef.current].forEach(menu => {
        if (!menu || !menu.open || menu.contains(event.target as Node)) {
          return;
        }
        menu.open = false;
      });
    };
    document.addEventListener('pointerdown', handlePointerDown);
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown);
    };
  }, []);

  return (
    <header className="shell-header">
      <div className="header-left">
        <SmartLink className="shell-brand" href={brandHref}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <path d="M7 8h10M7 12h10M7 16h6" />
            <path d="M6 8l1 1 2-2" />
          </svg>
          {session?.installationCompanyName || 'billetsys'}
        </SmartLink>
        {navigation.length > 0 && (
          <nav className="shell-nav" aria-label="Primary">
            {navigation.map(link => {
              if (showTicketMenu && link.label === 'Tickets') {
                return (
                  <details key={link.href} className="shell-nav-menu" ref={ticketMenuRef}>
                    <summary className={`shell-nav-link shell-nav-summary${isTicketRoute ? ' active' : ''}`}>
                      {ticketLabel}
                      <span
                        className={`ticket-alarm${showTicketAlarm ? ' is-visible' : ''}`}
                        aria-hidden={!showTicketAlarm}
                        title="SLA alarm"
                      >
                        🚨
                      </span>
                    </summary>
                    <div className="shell-nav-dropdown">
                      <SmartLink className="shell-nav-dropdown-link" href={ticketMenuBasePath} onClick={closeDetailsMenu}>
                        Active tickets
                      </SmartLink>
                      <SmartLink className="shell-nav-dropdown-link" href={`${ticketMenuBasePath}/open`} onClick={closeDetailsMenu}>
                        Open tickets
                      </SmartLink>
                      <SmartLink className="shell-nav-dropdown-link" href={`${ticketMenuBasePath}/closed`} onClick={closeDetailsMenu}>
                        Closed tickets
                      </SmartLink>
                    </div>
                  </details>
                );
              }

              if (link.label === 'Tickets') {
                return (
                  <SmartLink key={link.href} className={`shell-nav-link${isTicketRoute ? ' active' : ''}`} href={link.href}>
                    {ticketLabel}
                    <span
                      className={`ticket-alarm${showTicketAlarm ? ' is-visible' : ''}`}
                      aria-hidden={!showTicketAlarm}
                      title="SLA alarm"
                    >
                      🚨
                    </span>
                  </SmartLink>
                );
              }

              return (
                <SmartLink key={link.href} className="shell-nav-link" href={link.href}>
                  {link.label}
                </SmartLink>
              );
            })}
          </nav>
        )}
      </div>
      <div className="header-actions">
        {session?.authenticated && (
          <>
            {rssHref && (
              <a className="icon-link" href={rssHref} title="RSS feed" aria-label="RSS feed">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="6" cy="18" r="1.8" fill="currentColor" stroke="none" />
                  <path d="M4 11a9 9 0 0 1 9 9" />
                  <path d="M4 5a15 15 0 0 1 15 15" />
                </svg>
              </a>
            )}
            <details className="profile-menu" ref={profileMenuRef}>
              <summary className="user-summary" aria-label={userName}>
                {session?.logoBase64 ? (
                  <img className="user-logo" src={session.logoBase64} alt="User logo" />
                ) : (
                  <span className="user-avatar" aria-hidden="true">
                    <svg viewBox="0 0 24 24">
                      <circle cx="12" cy="8" r="4" />
                      <path d="M5 19c1.8-3.1 4.4-4.7 7-4.7s5.2 1.6 7 4.7" />
                    </svg>
                  </span>
                )}
              </summary>
              <div className="profile-dropdown">
                <SmartLink className="profile-link" href="/profile" onClick={closeDetailsMenu}>
                  Profile
                </SmartLink>
                <SmartLink className="profile-link" href="/profile/password" onClick={closeDetailsMenu}>
                  Password
                </SmartLink>
                <a className="profile-link" href="/logout" onClick={closeDetailsMenu}>
                  Sign out
                </a>
              </div>
            </details>
          </>
        )}
        <span className="header-clock">
          {now.toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit'
          })}
        </span>
      </div>
    </header>
  );
}
