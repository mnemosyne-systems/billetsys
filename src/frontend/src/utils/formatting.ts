import type { Id } from '../types/app';
import type { UserReference, VersionInfo } from '../types/domain';

interface QueryValueMap {
  [key: string]: string | number | boolean | null | undefined;
}

interface AssignmentSummary {
  entitlementName?: string;
  levelName?: string;
}

export function formatFileSize(size: number | null | undefined): string {
  if (!size) {
    return '0 B';
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = size;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value >= 10 || unitIndex === 0 ? Math.round(value) : value.toFixed(1)} ${units[unitIndex]}`;
}

export function formatPhone(phoneNumber?: string | null, extension?: string | null): string {
  if (!phoneNumber && !extension) {
    return '—';
  }
  if (!extension) {
    return phoneNumber || '—';
  }
  return `${phoneNumber || ''} ext. ${extension}`.trim();
}

export function durationLabel(value: string | number): string {
  return String(value) === '1' ? 'Monthly' : 'Yearly';
}

export function versionLabel(versions: VersionInfo[] | null | undefined, selectedId: Id | null | undefined): string {
  if (!selectedId) {
    return '';
  }
  const version = (versions || []).find(option => String(option.id) === String(selectedId));
  return version ? `${version.name} (${version.date})` : '';
}

export function isWhiteColorValue(color?: string | null): boolean {
  if (!color) {
    return true;
  }
  const normalized = color.replace(/\s+/g, '').toLowerCase();
  return normalized === 'white'
    || normalized === '#fff'
    || normalized === '#ffffff'
    || normalized === 'rgb(255,255,255)'
    || normalized === 'rgba(255,255,255,1)'
    || normalized === 'rgba(255,255,255,1.0)';
}

export function toQueryString(params: QueryValueMap): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `?${query}` : '';
}

export function profileInitial(fullName?: string | null, username?: string | null, email?: string | null): string {
  const firstName = (fullName || '')
    .trim()
    .split(/\s+/)
    .find(Boolean);
  const source = firstName || username || email || '?';
  return source.charAt(0).toUpperCase();
}

export function levelColorMarker(color?: string | null): string {
  switch ((color || '').toLowerCase()) {
    case 'black':
      return '⬛';
    case 'silver':
    case 'white':
      return '⬜';
    case 'gray':
      return '◻️';
    case 'maroon':
    case 'red':
      return '🟥';
    case 'purple':
    case 'fuchsia':
      return '🟪';
    case 'green':
    case 'lime':
      return '🟩';
    case 'olive':
    case 'yellow':
      return '🟨';
    case 'navy':
    case 'blue':
    case 'teal':
    case 'aqua':
      return '🟦';
    default:
      return '◻️';
  }
}

export function sortUsersByName(users: UserReference[]): UserReference[] {
  return [...users].sort((left, right) =>
    (left.displayName || left.username || '').localeCompare(right.displayName || right.username || '', undefined, {
      sensitivity: 'base'
    })
  );
}

export function sortEntitlementAssignments(assignments: AssignmentSummary[]): AssignmentSummary[] {
  return [...assignments].sort((left, right) => {
    const entitlementComparison = (left.entitlementName || '').localeCompare(right.entitlementName || '', undefined, {
      sensitivity: 'base'
    });
    if (entitlementComparison !== 0) {
      return entitlementComparison;
    }
    return (left.levelName || '').localeCompare(right.levelName || '', undefined, { sensitivity: 'base' });
  });
}
