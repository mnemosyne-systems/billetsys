/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import useJson from "../../hooks/useJson";
import useText from "../../hooks/useText";
import { useDarkModeToggle } from "../../hooks/useDarkModeToggle";
import { installationCompanyName } from "../../utils/installationBranding";
import { formatInstallationClock, toQueryString } from "../../utils/formatting";
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
import {
  Command,
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
} from "../ui/command";
import { MoonIcon, SearchIcon, SunIcon, XIcon } from "lucide-react";

interface TicketCounts {
  assignedCount?: number;
  openCount?: number;
}

interface TicketSuggestion {
  id?: number;
  name?: string;
  title?: string;
  detailPath?: string;
}

interface TicketSuggestionResponse {
  items?: TicketSuggestion[];
}

interface AuthenticatedHeaderProps {
  session: Session | null;
}

export default function AuthenticatedHeader({
  session,
}: AuthenticatedHeaderProps) {
  const [now, setNow] = useState(() => new Date());
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const { darkModeToggleRef, toggleDarkMode } = useDarkModeToggle();
  const location = useLocation();
  const navigate = useNavigate();
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
  const brandHref = session?.authenticated
    ? normalizeClientPath(session.homePath) || "/"
    : "/login";
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
  const ticketSearchContext = resolveTicketSearchContext(
    role,
    location.pathname,
  );
  const ticketSearchApiBase = ticketSearchContext?.apiBase;
  const ticketSearchPath = ticketSearchContext?.searchPath;
  const ticketSearchBasePath = ticketSearchContext?.basePath;
  const locationSearchTerm =
    new URLSearchParams(location.search).get("q") || "";
  const [ticketSearchOpen, setTicketSearchOpen] = useState(false);
  const [ticketSearchValue, setTicketSearchValue] = useState("");
  const ticketSuggestionState = useJson<TicketSuggestionResponse>(
    ticketSearchOpen &&
      ticketSearchApiBase &&
      ticketSearchValue.trim().length >= 2
      ? `${ticketSearchApiBase}/suggest${toQueryString({ q: ticketSearchValue.trim() })}`
      : null,
  );
  const ticketSuggestions = ticketSuggestionState.data?.items || [];
  const normalizedTicketSearch = ticketSearchValue.trim();

  function openTicketSearch() {
    setTicketSearchValue("");
    setTicketSearchOpen(true);
  }

  function closeTicketSearch() {
    setTicketSearchOpen(false);
    setTicketSearchValue("");
  }

  function submitTicketSearch(searchValue: string) {
    if (!ticketSearchPath) {
      return;
    }
    const nextParams = new URLSearchParams(location.search);
    const normalizedSearch = searchValue.trim();
    if (normalizedSearch) {
      nextParams.set("q", normalizedSearch);
    } else {
      nextParams.delete("q");
    }
    closeTicketSearch();
    navigate({
      pathname: ticketSearchPath,
      search: nextParams.toString() ? `?${nextParams.toString()}` : "",
    });
  }

  function applyTicketSuggestion(suggestion: TicketSuggestion) {
    submitTicketSearch((suggestion.name || "").trim());
  }

  function clearTicketSearch() {
    if (!ticketSearchBasePath) {
      return;
    }
    const nextParams = new URLSearchParams(location.search);
    nextParams.delete("q");
    closeTicketSearch();
    navigate({
      pathname: ticketSearchBasePath,
      search: nextParams.toString() ? `?${nextParams.toString()}` : "",
    });
  }

  function handleTicketSearchKeyDown(
    event: React.KeyboardEvent<HTMLInputElement>,
  ) {
    if (event.key === "Escape") {
      event.preventDefault();
      closeTicketSearch();
    }
  }

  useEffect(() => {
    if (!ticketSearchApiBase) {
      return undefined;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        openTicketSearch();
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [ticketSearchApiBase]);

  return (
    <header className="bg-[var(--header-bg)] px-5 py-3 text-[var(--header-text)] flex items-center justify-between gap-4 flex-wrap">
      <div className="flex items-center gap-4 flex-wrap">
        <SmartLink
          className="flex items-center gap-2.5 text-[var(--header-text)] no-underline text-xl font-bold"
          href={brandHref}
        >
          {session?.installationLogoBase64 ? (
            <img
              src={session.installationLogoBase64}
              alt={`${installationCompanyName(session.installationCompanyName)} logo`}
              className="h-7 w-7 shrink-0 object-contain"
            />
          ) : (
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
              className="w-6 h-6 fill-none stroke-current stroke-2"
            >
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <path d="M7 8h10M7 12h10M7 16h6" />
              <path d="M6 8l1 1 2-2" />
            </svg>
          )}
          {installationCompanyName(session?.installationCompanyName)}
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
                        className={`rounded-md bg-[var(--color-buttons-bg)] px-3 py-2 text-[var(--color-buttons-text)] flex items-center gap-1.5 cursor-pointer no-underline font-semibold hover:bg-[var(--color-buttons-hover)] outline-none ${isTicketRoute ? "underline" : ""}`}
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
                    className={`text-[var(--header-text)] no-underline font-semibold hover:opacity-80 flex items-center ${isTicketRoute ? "underline" : ""}`}
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
                  className="text-[var(--header-text)] no-underline font-semibold hover:opacity-80"
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
            {ticketSearchContext && (
              <>
                <Button
                  type="button"
                  variant="outline"
                  onClick={openTicketSearch}
                  className="hidden h-10 min-w-72 items-center justify-start rounded-xl px-3 text-left text-sm shadow-none md:flex"
                >
                  <SearchIcon className="mr-2 size-4 shrink-0" />
                  <span className="truncate">Search tickets...</span>
                  <kbd className="ml-auto rounded border border-[var(--color-buttons-text)]/20 bg-[var(--color-buttons-text)]/10 px-1.5 py-0.5 text-[10px] font-medium text-[var(--color-buttons-text)]/80">
                    Ctrl K
                  </kbd>
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={openTicketSearch}
                  className="md:hidden"
                  aria-label="Search tickets"
                >
                  <SearchIcon className="size-5" />
                </Button>
                <CommandDialog
                  open={ticketSearchOpen}
                  onOpenChange={(open) => {
                    if (open) {
                      openTicketSearch();
                      return;
                    }
                    closeTicketSearch();
                  }}
                  title="Search tickets"
                  description="Search tickets by identifier or message content."
                  className="max-w-2xl border-white/10 bg-popover/95 p-0 shadow-2xl backdrop-blur"
                >
                  <Command shouldFilter={false} className="bg-transparent">
                    <CommandInput
                      value={ticketSearchValue}
                      onValueChange={setTicketSearchValue}
                      onKeyDown={handleTicketSearchKeyDown}
                      placeholder="Search ticket number or message text..."
                    />
                    <CommandList className="max-h-[22rem]">
                      <CommandEmpty>
                        {ticketSearchValue.trim()
                          ? "No matching tickets."
                          : "Type at least 2 characters to search tickets."}
                      </CommandEmpty>
                      {normalizedTicketSearch && (
                        <CommandGroup heading="Search">
                          <CommandItem
                            value={`search ${normalizedTicketSearch}`}
                            className="data-selected:bg-neutral-200 data-selected:text-neutral-950"
                            onSelect={() =>
                              submitTicketSearch(normalizedTicketSearch)
                            }
                          >
                            <SearchIcon className="size-4 text-[var(--color-header-bg)] opacity-80" />
                            <span className="truncate">
                              Search all tickets for {normalizedTicketSearch}
                            </span>
                          </CommandItem>
                        </CommandGroup>
                      )}
                      {locationSearchTerm && (
                        <>
                          <CommandSeparator />
                          <CommandGroup heading="Current filter">
                            <CommandItem
                              value={`clear ${locationSearchTerm}`}
                              className="data-selected:bg-neutral-200 data-selected:text-neutral-950"
                              onSelect={clearTicketSearch}
                            >
                              <XIcon className="size-4 text-destructive opacity-80" />
                              <div className="flex min-w-0 flex-col">
                                <span className="truncate font-medium">
                                  Clear current search
                                </span>
                                <span className="truncate text-xs text-muted-foreground">
                                  Showing results for {locationSearchTerm}
                                </span>
                              </div>
                            </CommandItem>
                          </CommandGroup>
                        </>
                      )}
                      {ticketSuggestions.length > 0 && (
                        <CommandGroup heading="Suggestions">
                          {ticketSuggestions.map((suggestion, index) => (
                            <CommandItem
                              key={`${suggestion.id}-${suggestion.name}`}
                              value={`${index} ${suggestion.name} ${suggestion.title || ""}`}
                              className="data-selected:bg-neutral-200 data-selected:text-neutral-950"
                              onSelect={() => applyTicketSuggestion(suggestion)}
                            >
                              <SearchIcon className="size-4 text-[var(--color-header-bg)] opacity-80" />
                              <div className="flex min-w-0 flex-col">
                                <span className="truncate font-medium">
                                  {suggestion.name}
                                </span>
                                <span className="truncate text-xs text-muted-foreground">
                                  {suggestion.title || "Ticket"}
                                </span>
                              </div>
                            </CommandItem>
                          ))}
                        </CommandGroup>
                      )}
                    </CommandList>
                  </Command>
                </CommandDialog>
              </>
            )}
            {rssHref && (
              <Button variant="ghost" size="icon" asChild className="">
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
            <button
              ref={darkModeToggleRef}
              type="button"
              onClick={toggleDarkMode}
              aria-pressed="false"
              className="group relative inline-flex h-8 w-16 items-center rounded-full border border-[var(--color-buttons-bg)] bg-[var(--color-buttons-bg)] p-1 text-[var(--color-buttons-text)] transition-opacity hover:bg-[var(--color-buttons-hover)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-buttons-bg)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--header-bg)]"
              aria-label="Toggle dark mode"
              title="Toggle dark mode"
            >
              <SunIcon className="absolute left-2 size-3.5 opacity-90" />
              <MoonIcon className="absolute right-2 size-3.5 opacity-90" />
              <span className="pointer-events-none inline-block h-6 w-6 translate-x-0 rounded-full bg-[var(--color-buttons-text)] shadow-sm transition-transform duration-200 ease-out group-aria-[pressed=true]:translate-x-8" />
            </button>
            <DropdownMenu open={userMenuOpen} onOpenChange={setUserMenuOpen}>
              <DropdownMenuTrigger asChild>
                <button
                  className="flex items-center cursor-pointer list-none text-[var(--color-buttons-text)] outline-none rounded-full bg-[var(--color-buttons-bg)] p-0.5"
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
                      className="w-7 h-7 rounded-full inline-flex items-center justify-center bg-[var(--color-buttons-text)]/20 font-bold"
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
                  <SmartLink
                    href="/profile"
                    onClick={() => setUserMenuOpen(false)}
                  >
                    Profile
                  </SmartLink>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <SmartLink
                    href="/profile/password"
                    onClick={() => setUserMenuOpen(false)}
                  >
                    Password
                  </SmartLink>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <a href="/logout" onClick={() => setUserMenuOpen(false)}>
                    Sign out
                  </a>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </>
        )}
        <span className="font-semibold tabular-nums">
          {formatInstallationClock(now, session?.installationUse24HourClock)}
        </span>
      </div>
    </header>
  );
}

function resolveTicketSearchContext(role: Session["role"], pathname: string) {
  if (role === "support") {
    if (pathname.startsWith("/support/tickets")) {
      return {
        apiBase: "/api/support/tickets",
        basePath: "/support/tickets",
        searchPath: "/support/tickets/search",
      };
    }
  }
  if (role === "superuser") {
    if (pathname.startsWith("/superuser/tickets")) {
      return {
        apiBase: "/api/superuser/tickets",
        basePath: "/superuser/tickets",
        searchPath: "/superuser/tickets/search",
      };
    }
  }
  if (role === "tam" || role === "user") {
    if (pathname.startsWith("/user/tickets")) {
      return {
        apiBase: "/api/user/tickets",
        basePath: "/user/tickets",
        searchPath: "/user/tickets/search",
      };
    }
  }
  return null;
}
