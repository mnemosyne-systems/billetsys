import type { Role, Session, SessionNavigationLink } from '../types/app';

export function headerNavigation(session: Session | null): SessionNavigationLink[] {
  const navigation = Array.isArray(session?.navigation) ? session.navigation : [];
  const role = session?.role;

  if (role === 'admin') {
    return [];
  }

  if (role === 'support') {
    return filterNavigation(navigation, ['Tickets', 'Articles', 'Users']);
  }

  if (role === 'superuser' || role === 'tam') {
    return filterNavigation(navigation, ['Tickets', 'Articles', 'Reports']);
  }

  if (role === 'user') {
    return filterNavigation(navigation, ['Tickets', 'Articles']);
  }

  return navigation.filter(link => link.label !== 'Profile');
}

export function filterNavigation(navigation: SessionNavigationLink[], labels: string[]): SessionNavigationLink[] {
  const allowed = new Set(labels);
  const filtered = navigation.filter(link => allowed.has(link.label));
  return labels.flatMap(label => filtered.find(link => link.label === label) || []);
}

export function orderedNavigation(navigation: SessionNavigationLink[] | null | undefined, labels: string[]): SessionNavigationLink[] {
  if (!Array.isArray(navigation)) {
    return [];
  }
  const linksByLabel = new Map(navigation.map(link => [link.label, link]));
  return labels
    .map(label => linksByLabel.get(label))
    .filter((link): link is SessionNavigationLink => Boolean(link));
}

export function ticketCountsApiPath(role?: Role): string {
  if (role === 'support') {
    return '/api/support/tickets';
  }
  if (role === 'superuser') {
    return '/api/superuser/tickets';
  }
  if (role === 'tam' || role === 'user') {
    return '/api/user/tickets';
  }
  return '';
}

export function ticketLabelForRole(role: Role | undefined, assignedCount: number, openCount: number): string {
  if (role === 'support' || role === 'superuser' || role === 'tam' || role === 'user') {
    return `Tickets (${assignedCount}/${openCount})`;
  }
  return 'Tickets';
}

export function isRoleTicketRoute(role: Role | undefined, pathname: string): boolean {
  if (role === 'support') {
    return pathname.startsWith('/support/tickets');
  }
  if (role === 'superuser') {
    return pathname.startsWith('/superuser/tickets');
  }
  if (role === 'tam' || role === 'user') {
    return pathname.startsWith('/user/tickets');
  }
  return pathname === '/tickets';
}

export function showRoleTicketAlarm(role?: Role): boolean {
  return role === 'support' || role === 'superuser' || role === 'tam' || role === 'user';
}

export function rssPath(role?: Role): string {
  if (role === 'support') {
    return '/rss/support';
  }
  if (role === 'superuser') {
    return '/rss/superuser';
  }
  if (role === 'tam') {
    return '/rss/tam';
  }
  return '';
}
