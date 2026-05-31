/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import useJson from "../../hooks/useJson";
import useText from "../../hooks/useText";
import { useDarkModeToggle } from "../../hooks/useDarkModeToggle";
import { installationCompanyName } from "../../utils/installationBranding";
import { formatInstallationClock, toQueryString } from "../../utils/formatting";
import { SmartLink, normalizeClientPath } from "../../utils/routing";
import { isEditableTarget } from "../../utils/keyboard";
import {
  ticketCountsApiPath,
  ticketLabelForRole,
  isRoleTicketRoute,
  showRoleTicketAlarm,
  headerNavigation,
  rssPath,
  ticketShortcutPaths,
} from "../../utils/navigation";
import type { Session } from "../../types/app";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuShortcut,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu";
import { Button } from "../ui/button";
import { MoonIcon, SearchIcon, SunIcon } from "lucide-react";

interface TicketCounts {
  assignedCount?: number;
  openCount?: number;
}

interface SearchSuggestion {
  id?: number;
  name?: string;
  title?: string;
  detailPath?: string;
}

interface SuggestionResponse {
  items?: SearchSuggestion[];
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
  const shortcuts = ticketShortcutPaths(role);

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

  // --- Universal search ---
  const ticketSuggestApiBase = ticketCountsApiPath(role);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const searchContainerRef = useRef<HTMLDivElement>(null);
  const [searchValue, setSearchValue] = useState("");
  const [searchFocused, setSearchFocused] = useState(false);
  const normalizedSearch = searchValue.trim();
  const shouldFetch = normalizedSearch.length >= 2;

  const ticketSuggestionState = useJson<SuggestionResponse>(
    shouldFetch && ticketSuggestApiBase
      ? `${ticketSuggestApiBase}/suggest${toQueryString({ q: normalizedSearch })}`
      : null,
  );
  const articleSuggestionState = useJson<SuggestionResponse>(
    shouldFetch
      ? `/api/articles/suggest${toQueryString({ q: normalizedSearch })}`
      : null,
  );
  const ticketSuggestions = ticketSuggestionState.data?.items || [];
  const articleSuggestions = articleSuggestionState.data?.items || [];
  const allSuggestions: SearchSuggestion[] = [
    ...ticketSuggestions,
    ...articleSuggestions,
  ].sort((a, b) =>
    (a.name || "").localeCompare(b.name || "", undefined, {
      sensitivity: "base",
    }),
  );
  const hasResults = allSuggestions.length > 0;
  const showDropdown = searchFocused && shouldFetch;
  const [activeIndex, setActiveIndex] = useState(-1);

  function selectSuggestion(suggestion: SearchSuggestion) {
    if (suggestion.detailPath) {
      navigate(suggestion.detailPath);
      setSearchValue("");
      setActiveIndex(-1);
      searchInputRef.current?.blur();
    }
  }

  function handleSearchKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Escape") {
      event.preventDefault();
      setSearchValue("");
      searchInputRef.current?.blur();
    } else if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((prev) =>
        prev < allSuggestions.length - 1 ? prev + 1 : 0,
      );
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((prev) =>
        prev > 0 ? prev - 1 : allSuggestions.length - 1,
      );
    } else if (event.key === "Enter") {
      event.preventDefault();
      if (activeIndex >= 0 && activeIndex < allSuggestions.length) {
        selectSuggestion(allSuggestions[activeIndex]);
      }
    }
  }

  useEffect(() => {
    if (!session?.authenticated) {
      return undefined;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchInputRef.current?.focus();
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [session?.authenticated]);

  // Close dropdown when clicking outside
  useEffect(() => {
    if (!searchFocused) {
      return undefined;
    }
    const handleMouseDown = (event: MouseEvent) => {
      if (
        searchContainerRef.current &&
        !searchContainerRef.current.contains(event.target as Node)
      ) {
        setSearchFocused(false);
      }
    };
    document.addEventListener("mousedown", handleMouseDown);
    return () => {
      document.removeEventListener("mousedown", handleMouseDown);
    };
  }, [searchFocused]);

  useEffect(() => {
    if (!shortcuts) {
      return undefined;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      const isCreateShortcut =
        event.key === "+" ||
        event.code === "NumpadAdd" ||
        (event.code === "Equal" && event.shiftKey);
      if (
        event.defaultPrevented ||
        !event.ctrlKey ||
        event.metaKey ||
        event.altKey ||
        (event.shiftKey && !isCreateShortcut) ||
        isEditableTarget(event.target)
      ) {
        return;
      }
      const key = event.key.toLowerCase();
      const destination =
        key === "a"
          ? shortcuts.active
          : key === "o"
            ? shortcuts.open
            : key === "c"
              ? shortcuts.closed
              : isCreateShortcut
                ? shortcuts.create
                : null;
      if (!destination) {
        return;
      }
      event.preventDefault();
      navigate(destination);
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [navigate, shortcuts]);

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
                        <SmartLink
                          href={shortcuts?.active || ticketMenuBasePath}
                          className="flex w-full items-center"
                        >
                          Active tickets
                          <DropdownMenuShortcut>Ctrl+A</DropdownMenuShortcut>
                        </SmartLink>
                      </DropdownMenuItem>
                      <DropdownMenuItem asChild>
                        <SmartLink
                          href={shortcuts?.open || `${ticketMenuBasePath}/open`}
                          className="flex w-full items-center"
                        >
                          Open tickets
                          <DropdownMenuShortcut>Ctrl+O</DropdownMenuShortcut>
                        </SmartLink>
                      </DropdownMenuItem>
                      <DropdownMenuItem asChild>
                        <SmartLink
                          href={
                            shortcuts?.closed || `${ticketMenuBasePath}/closed`
                          }
                          className="flex w-full items-center"
                        >
                          Closed tickets
                          <DropdownMenuShortcut>Ctrl+C</DropdownMenuShortcut>
                        </SmartLink>
                      </DropdownMenuItem>
                      {shortcuts && (
                        <DropdownMenuItem asChild>
                          <SmartLink
                            href={shortcuts.create}
                            className="flex w-full items-center"
                          >
                            Create ticket
                            <DropdownMenuShortcut>Ctrl++</DropdownMenuShortcut>
                          </SmartLink>
                        </DropdownMenuItem>
                      )}
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
            <div ref={searchContainerRef} className="relative">
              <div className="relative flex items-center">
                <SearchIcon className="pointer-events-none absolute left-3 size-4 shrink-0 text-muted-foreground" />
                <input
                  ref={searchInputRef}
                  type="text"
                  value={searchValue}
                  onChange={(e) => {
                    setSearchValue(e.target.value);
                    setActiveIndex(-1);
                  }}
                  onFocus={() => setSearchFocused(true)}
                  onKeyDown={handleSearchKeyDown}
                  placeholder="Search..."
                  className="h-10 w-full min-w-48 rounded-xl border border-input bg-background pl-9 pr-3 text-sm text-foreground placeholder:text-muted-foreground outline-none focus:ring-2 focus:ring-ring md:min-w-72"
                  aria-label="Search"
                />
              </div>
              {showDropdown && (
                <div className="absolute top-full left-0 z-50 mt-1 w-full min-w-72 overflow-hidden rounded-lg bg-popover text-sm text-popover-foreground shadow-lg ring-1 ring-foreground/10">
                  {!hasResults && (
                    <div className="px-4 py-3 text-center text-muted-foreground">
                      No matches.
                    </div>
                  )}
                  {allSuggestions.map((suggestion, index) => (
                    <button
                      key={`${suggestion.detailPath}-${suggestion.id}`}
                      type="button"
                      className={`flex w-full items-center gap-2 px-3 py-2 text-left cursor-pointer ${
                        activeIndex === index
                          ? "bg-accent text-accent-foreground"
                          : "hover:bg-accent hover:text-accent-foreground"
                      }`}
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() => selectSuggestion(suggestion)}
                      onMouseEnter={() => setActiveIndex(index)}
                    >
                      <SearchIcon className="size-4 shrink-0 text-muted-foreground" />
                      <div className="flex min-w-0 flex-col">
                        <span className="truncate font-medium">
                          {suggestion.name}
                        </span>
                        {suggestion.title &&
                          suggestion.title !== suggestion.name && (
                            <span className="truncate text-xs text-muted-foreground">
                              {suggestion.title}
                            </span>
                          )}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
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
