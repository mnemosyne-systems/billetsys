/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { MouseEvent, ReactNode } from 'react';
import { useState } from 'react';
import type { Id } from '../../types/app';
import type { UserReference } from '../../types/domain';

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
}

export function UserHoverLink({ user, className, children }: UserHoverLinkProps) {
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
      top
    });
  };

  return (
    <>
      <a
        className={className}
        href={user.detailPath}
        onMouseEnter={updateTooltip}
        onMouseMove={updateTooltip}
        onMouseLeave={() => setTooltipState(null)}
        onBlur={() => setTooltipState(null)}
      >
        {children}
      </a>
      {tooltipState && (
        <div className="user-tooltip" style={{ left: tooltipState.left, top: tooltipState.top }}>
          <div className="user-tooltip-inner">
            <div className="user-tooltip-header">
              <div className="user-tooltip-avatar">
                {user.logoBase64 ? (
                  <img src={user.logoBase64} alt="avatar" />
                ) : (
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                    <circle cx="12" cy="8" r="4" />
                    <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" />
                  </svg>
                )}
              </div>
              <div>
                <div className="user-tooltip-name">{user.username || ''}</div>
                <div className="user-tooltip-fullname">{user.fullName || ''}</div>
              </div>
            </div>
            <div className="user-tooltip-divider" />
            <div className="user-tooltip-meta">{user.email ? `📧 ${user.email}` : ''}</div>
            <div className="user-tooltip-meta">{user.countryName ? `🌎 ${user.countryName}` : ''}</div>
            <div className="user-tooltip-meta">{user.timezoneName ? `🕐 ${user.timezoneName}` : ''}</div>
          </div>
        </div>
      )}
    </>
  );
}

export function UserReferenceList({ users }: UserCollectionProps) {
  if (!users || users.length === 0) {
    return <p className="muted-text">—</p>;
  }
  return (
    <ul className="plain-list">
      {users.map(user => (
        <li key={user.id}>
          {user.detailPath ? (
            <UserHoverLink user={user} className="inline-link">
              {user.displayName || user.username}
            </UserHoverLink>
          ) : (
            user.displayName || user.username
          )}
          {user.email && <span className="muted-text"> — {user.email}</span>}
        </li>
      ))}
    </ul>
  );
}

export function UserReferenceInlineList({ users }: UserCollectionProps) {
  if (!users || users.length === 0) {
    return <input value="-" readOnly />;
  }

  return (
    <div>
      {users.map((user, index) => (
        <span key={user.id}>
          {user.detailPath ? (
            <UserHoverLink user={user} className="inline-link">
              {user.username || user.displayName}
            </UserHoverLink>
          ) : (
            user.username || user.displayName
          )}
          {index < users.length - 1 ? ', ' : ''}
        </span>
      ))}
    </div>
  );
}

export function SelectableUserPicker({ title, users, selectedIds, onToggle }: SelectableUsersProps) {
  return (
    <section className="detail-card">
      <h3>{title}</h3>
      <div className="checkbox-list">
        {users.length === 0 ? (
          <p className="muted-text">No users available.</p>
        ) : (
          users.map(user => (
            <label key={user.id} className="checkbox-card">
              <input type="checkbox" checked={selectedIds.includes(user.id)} onChange={() => onToggle(user.id)} />
              <span>
                <strong>{user.displayName || user.username}</strong>
                <small>{user.email}</small>
              </span>
            </label>
          ))
        )}
      </div>
    </section>
  );
}

export function SelectableUserSummary({ users }: UserCollectionProps) {
  if (!users || users.length === 0) {
    return <p className="muted-text">—</p>;
  }

  return (
    <ul className="plain-list">
      {users.map(user => (
        <li key={user.id}>
          {(user.displayName || user.username) + (user.email ? ` (${user.email})` : '')}
        </li>
      ))}
    </ul>
  );
}

export function OwnerUserList({ users }: UserCollectionProps) {
  if (!users || users.length === 0) {
    return <p className="muted-text">—</p>;
  }

  return (
    <ul className="plain-list">
      {users.map(user => (
        <li key={user.id}>
          <a href={user.profilePath}>{user.displayName || user.username}</a>
        </li>
      ))}
    </ul>
  );
}

export function OwnerSelector({ title, users, selectedIds, onToggle }: SelectableUsersProps) {
  return (
    <section className="detail-card">
      <h3>{title}</h3>
      <div className="checkbox-list">
        {users.length === 0 ? (
          <p className="muted-text">No users available.</p>
        ) : (
          users.map(user => (
            <label key={user.id} className="checkbox-card">
              <input type="checkbox" checked={selectedIds.includes(user.id)} onChange={() => onToggle(user.id)} />
              <span>
                <strong>{user.displayName || user.username}</strong>
                <small>{user.email}</small>
              </span>
            </label>
          ))
        )}
      </div>
    </section>
  );
}
