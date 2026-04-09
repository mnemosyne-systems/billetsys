/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import useJson from "../../hooks/useJson";
import useText from "../../hooks/useText";
import { SmartLink, normalizeClientPath } from "../../utils/routing";
import {
  ticketCountsApiPath,
  ticketLabelForRole,
  isRoleTicketRoute,
  showRoleTicketAlarm,
  headerNavigation,
  rssPath,
} from "../../utils/navigation";
import type { Session } from "../../types/app";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import { Button } from "../ui/button";

interface TicketCounts {
  assignedCount?: number;
  openCount?: number;
}

interface AuthenticatedHeaderProps {
  session: Session | null;
}

export default function AuthenticatedHeader({
  session,
}: AuthenticatedHeaderProps) {
  const [now, setNow] = useState(() => new Date());
  const location = useLocation();
  const role = session?.role;
  const showTicketMenu =
    role === "support" ||
    role === "user" ||
    role === "superuser" ||
    role === "tam";

  const ticketMenuBasePath =
    role === "support"
      ? "/support/tickets"
      : role === "superuser"
        ? "/superuser/tickets"
        : "/user/tickets";

  const ticketCountsState = useJson<TicketCounts>(ticketCountsApiPath(role));
  const ticketAlarmState = useText(
    showRoleTicketAlarm(role) ? "/tickets/alarm/status" : "",
  );

  useEffect(() => {
    const timerId = window.setInterval(() => setNow(new Date()), 1000);
    return () => window.clearInterval(timerId);
  }, []);

  const navigation = headerNavigation(session);
  const brandHref = normalizeClientPath(session?.homePath) || "/";
  const userName = session?.displayName || session?.username || "Guest";
  const assignedCount = ticketCountsState.data?.assignedCount ?? 0;
  const openCount = ticketCountsState.data?.openCount ?? 0;
  const ticketLabel = ticketLabelForRole(role, assignedCount, openCount);
  const showTicketAlarm =
    String(ticketAlarmState.data || "")
      .trim()
      .toLowerCase() === "true";
  const isTicketRoute = isRoleTicketRoute(role, location.pathname);
  const rssHref = rssPath(role);

  return (
    <header className="bg-header-bg text-header-text px-5 py-3 flex items-center justify-between gap-4 flex-wrap">
      <div className="flex items-center gap-4 flex-wrap">
        <SmartLink
          className="flex items-center gap-2.5 text-header-text no-underline text-xl font-bold"
          href={brandHref}
        >
          <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
            className="w-6 h-6 fill-none stroke-current stroke-2"
          >
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <path d="M7 8h10M7 12h10M7 16h6" />
            <path d="M6 8l1 1 2-2" />
          </svg>
          {session?.installationCompanyName || "billetsys"}
        </SmartLink>
        {navigation.length > 0 && (
          <nav
            className="flex items-center gap-3 flex-wrap"
            aria-label="Primary"
          >
            {navigation.map((link) => {
              if (showTicketMenu && link.label === "Tickets") {
                return (
                  <DropdownMenu key={link.href}>
                    <DropdownMenuTrigger asChild>
                      <button
                        className={`text-header-text flex items-center gap-1.5 cursor-pointer no-underline font-semibold hover:opacity-80 outline-none ${isTicketRoute ? "underline" : ""}`}
                      >
                        {ticketLabel}
                        <span
                          className={`${showTicketAlarm ? "inline-block animate-ticket-alarm-blink" : "hidden"} ml-0.5`}
                          aria-hidden={!showTicketAlarm}
                          title="SLA alarm"
                        >
                          🚨
                        </span>
                      </button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent>
                      <DropdownMenuItem asChild>
                        <SmartLink href={ticketMenuBasePath}>
                          Active tickets
                        </SmartLink>
                      </DropdownMenuItem>
                      <DropdownMenuItem asChild>
                        <SmartLink href={`${ticketMenuBasePath}/open`}>
                          Open tickets
                        </SmartLink>
                      </DropdownMenuItem>
                      <DropdownMenuItem asChild>
                        <SmartLink href={`${ticketMenuBasePath}/closed`}>
                          Closed tickets
                        </SmartLink>
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                );
              }

              if (link.label === "Tickets") {
                return (
                  <SmartLink
                    key={link.href}
                    className={`text-header-text no-underline font-semibold hover:opacity-80 flex items-center ${isTicketRoute ? "underline" : ""}`}
                    href={link.href}
                  >
                    {ticketLabel}
                    <span
                      className={`${showTicketAlarm ? "inline-block animate-ticket-alarm-blink" : "hidden"} ml-0.5`}
                      aria-hidden={!showTicketAlarm}
                      title="SLA alarm"
                    >
                      🚨
                    </span>
                  </SmartLink>
                );
              }

              return (
                <SmartLink
                  key={link.href}
                  className="text-header-text no-underline font-semibold hover:opacity-80"
                  href={link.href}
                >
                  {link.label}
                </SmartLink>
              );
            })}
          </nav>
        )}
      </div>
      <div className="flex items-center gap-3 justify-end">
        {session?.authenticated && (
          <>
            {rssHref && (
              <Button
                variant="ghost"
                size="icon"
                asChild
                className="text-header-text hover:bg-white/20 hover:text-white"
              >
                <a href={rssHref} title="RSS feed" aria-label="RSS feed">
                  <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                    className="w-5 h-5 fill-none stroke-current stroke-2"
                  >
                    <circle
                      cx="6"
                      cy="18"
                      r="1.8"
                      fill="currentColor"
                      stroke="none"
                    />
                    <path d="M4 11a9 9 0 0 1 9 9" />
                    <path d="M4 5a15 15 0 0 1 15 15" />
                  </svg>
                </a>
              </Button>
            )}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  className="flex items-center cursor-pointer list-none text-header-text outline-none rounded-full"
                  aria-label={userName}
                >
                  {session?.logoBase64 ? (
                    <img
                      className="w-7 h-7 rounded-full object-cover"
                      src={session.logoBase64}
                      alt="User logo"
                    />
                  ) : (
                    <span
                      className="w-7 h-7 rounded-full inline-flex items-center justify-center bg-white/20 font-bold"
                      aria-hidden="true"
                    >
                      <svg
                        viewBox="0 0 24 24"
                        className="w-4 h-4 fill-none stroke-current stroke-2"
                      >
                        <circle cx="12" cy="8" r="4" />
                        <path d="M5 19c1.8-3.1 4.4-4.7 7-4.7s5.2 1.6 7 4.7" />
                      </svg>
                    </span>
                  )}
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem asChild>
                  <SmartLink href="/profile">Profile</SmartLink>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <SmartLink href="/profile/password">Password</SmartLink>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <a href="/logout">Sign out</a>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </>
        )}
        <span className="font-semibold tabular-nums">
          {now.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })}
        </span>
      </div>
    </header>
  );
}
