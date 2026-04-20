/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { MouseEvent, ReactNode } from "react";
import { useState } from "react";
import type { Id } from "../../types/app";
import type { UserReference } from "../../types/domain";

interface TooltipState {
  left: number;
  top: number;
}

interface UserHoverLinkProps {
  user?: UserReference | null;
  className?: string;
  children?: ReactNode;
}

interface UserCollectionProps {
  users?: UserReference[];
}

interface SelectableUsersProps {
  title: string;
  users: UserReference[];
  selectedIds: Id[];
  onToggle: (userId: Id) => void;
  selectionMode?: "multiple" | "single";
}

export function UserHoverLink({
  user,
  className,
  children,
}: UserHoverLinkProps) {
  const [tooltipState, setTooltipState] = useState<TooltipState | null>(null);

  if (!user?.detailPath) {
    return children;
  }

  const updateTooltip = (event: MouseEvent<HTMLAnchorElement>) => {
    const pad = 12;
    const width = 240;
    const height = 140;
    let left = event.clientX + pad;
    let top = event.clientY + pad;
    if (left + width > window.innerWidth - pad) {
      left = event.clientX - width - pad;
    }
    if (top + height > window.innerHeight - pad) {
      top = event.clientY - height - pad;
    }
    setTooltipState({
      left,
      top,
    });
  };

  return (
    <>
      <a
        className={`text-primary hover:underline font-medium ${className || ""}`}
        href={user.detailPath}
        onMouseEnter={updateTooltip}
        onMouseMove={updateTooltip}
        onMouseLeave={() => setTooltipState(null)}
        onBlur={() => setTooltipState(null)}
      >
        {children}
      </a>
      {tooltipState && (
        <div
          className="fixed z-50 pointer-events-none"
          style={{ left: tooltipState.left, top: tooltipState.top }}
        >
          <div className="bg-popover text-popover-foreground border shadow-md rounded-lg p-3 w-[240px] flex flex-col space-y-2 animate-in fade-in zoom-in-95 duration-100">
            <div className="flex items-center space-x-3">
              <div className="h-10 w-10 shrink-0 overflow-hidden text-muted-foreground rounded-full border bg-muted flex items-center justify-center">
                {user.logoBase64 ? (
                  <img
                    src={user.logoBase64}
                    alt="avatar"
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.5"
                    className="h-5 w-5"
                  >
                    <circle cx="12" cy="8" r="4" />
                    <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" />
                  </svg>
                )}
              </div>
              <div className="flex flex-col min-w-0">
                <div className="text-sm font-semibold truncate">
                  {user.username || ""}
                </div>
                <div className="text-xs text-muted-foreground truncate">
                  {user.fullName || ""}
                </div>
              </div>
            </div>
            <div className="border-t pb-1 pt-1" />
            <div className="text-xs truncate text-muted-foreground flex items-center">
              {user.email ? `📧 ${user.email}` : ""}
            </div>
            <div className="text-xs truncate text-muted-foreground flex items-center">
              {user.countryName ? `🌎 ${user.countryName}` : ""}
            </div>
            <div className="text-xs truncate text-muted-foreground flex items-center">
              {user.timezoneName ? `🕐 ${user.timezoneName}` : ""}
            </div>
          </div>
        </div>
      )}
    </>
  );
}

export function UserReferenceList({ users }: UserCollectionProps) {
  if (!users || users.length === 0) {
    return <p className="text-muted-foreground">—</p>;
  }
  return (
    <ul className="space-y-1">
      {users.map((user) => (
        <li key={user.id} className="text-sm">
          {user.detailPath ? (
            <UserHoverLink user={user}>
              {user.displayName || user.username}
            </UserHoverLink>
          ) : (
            <span className="font-medium">
              {user.displayName || user.username}
            </span>
          )}
          {user.email && (
            <span className="text-muted-foreground"> — {user.email}</span>
          )}
        </li>
      ))}
    </ul>
  );
}

export function UserReferenceInlineList({ users }: UserCollectionProps) {
  if (!users || users.length === 0) {
    return (
      <input
        className="flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm text-muted-foreground outline-none"
        value="-"
        readOnly
      />
    );
  }

  return (
    <div className="flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm overflow-x-auto whitespace-nowrap items-center">
      {users.map((user, index) => (
        <span key={user.id}>
          {user.detailPath ? (
            <UserHoverLink user={user}>
              {user.username || user.displayName}
            </UserHoverLink>
          ) : (
            user.username || user.displayName
          )}
          {index < users.length - 1 ? ", " : ""}
        </span>
      ))}
    </div>
  );
}

export function SelectableUserPicker({
  title,
  users,
  selectedIds,
  onToggle,
  selectionMode = "multiple",
}: SelectableUsersProps) {
  return (
    <div className="space-y-4 rounded-lg border bg-card text-card-foreground shadow-sm p-6 overflow-hidden">
      <h3 className="text-lg font-semibold tracking-tight">{title}</h3>
      <div className="grid gap-3 pt-2">
        {users.length === 0 ? (
          <p className="text-sm text-muted-foreground italic">
            No users available.
          </p>
        ) : (
          users.map((user) => (
            <label
              key={user.id}
              className="flex items-center space-x-3 space-y-0 rounded-md border p-4 hover:bg-muted/50 transition-colors cursor-pointer"
            >
              <div className="flex items-center justify-center">
                <input
                  type={selectionMode === "single" ? "radio" : "checkbox"}
                  className="h-4 w-4 rounded-sm border-primary text-primary focus:ring-primary shadow-sm"
                  checked={selectedIds.includes(user.id)}
                  onChange={() => onToggle(user.id)}
                />
              </div>
              <div className="flex flex-col min-w-0">
                <span className="text-sm font-medium leading-none">
                  {user.displayName || user.username}
                </span>
                <span className="text-xs text-muted-foreground mt-1 truncate">
                  {user.email}
                </span>
              </div>
            </label>
          ))
        )}
      </div>
    </div>
  );
}

export function SelectableUserSummary({ users }: UserCollectionProps) {
  if (!users || users.length === 0) {
    return <p className="text-sm text-muted-foreground">—</p>;
  }

  return (
    <ul className="space-y-1">
      {users.map((user) => (
        <li key={user.id} className="text-sm text-muted-foreground">
          <span className="text-foreground font-medium">
            {user.displayName || user.username}
          </span>
          {user.email ? ` (${user.email})` : ""}
        </li>
      ))}
    </ul>
  );
}

export function OwnerUserList({ users }: UserCollectionProps) {
  if (!users || users.length === 0) {
    return <p className="text-sm text-muted-foreground">—</p>;
  }

  return (
    <ul className="space-y-2">
      {users.map((user) => (
        <li key={user.id}>
          <a
            className="text-primary hover:underline text-sm font-medium flex items-center space-x-2"
            href={user.profilePath}
          >
            {user.displayName || user.username}
          </a>
        </li>
      ))}
    </ul>
  );
}

export function OwnerSelector({
  title,
  users,
  selectedIds,
  onToggle,
}: SelectableUsersProps) {
  return (
    <div className="space-y-4">
      {title && <h3 className="text-sm font-medium">{title}</h3>}
      <div className="grid gap-2 max-h-[300px] overflow-y-auto px-1 pb-1">
        {users.length === 0 ? (
          <p className="text-sm text-muted-foreground italic">
            No users available.
          </p>
        ) : (
          users.map((user) => (
            <label
              key={user.id}
              className="flex items-center space-x-3 rounded-md border p-3 cursor-pointer hover:bg-muted/50 transition-colors"
            >
              <input
                type="checkbox"
                className="h-4 w-4 rounded-sm border-primary text-primary focus:ring-primary shadow-sm"
                checked={selectedIds.includes(user.id)}
                onChange={() => onToggle(user.id)}
              />
              <div className="flex flex-col min-w-0">
                <span className="text-sm font-medium leading-none">
                  {user.displayName || user.username}
                </span>
                <span className="text-xs text-muted-foreground mt-1 truncate">
                  {user.email}
                </span>
              </div>
            </label>
          ))
        )}
      </div>
    </div>
  );
}
