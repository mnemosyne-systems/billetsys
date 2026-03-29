import { lazy } from 'react';
import { PATHS } from './paths';
import type { AppRoute, SessionState } from '../types/app';

const SupportTicketCreatePage = lazy(() => import('../pages/SupportTicketCreatePage'));
const SupportTicketDetailPage = lazy(() => import('../pages/SupportTicketDetailPage'));
const SupportTicketsPage = lazy(() => import('../pages/SupportTicketsPage'));
const TicketWorkbenchFormPage = lazy(() => import('../pages/TicketWorkbenchFormPage'));
const TicketWorkbenchPage = lazy(() => import('../pages/TicketWorkbenchPage'));

export function getTicketRoutes(sessionState: SessionState): AppRoute[] {
  return [
    { path: PATHS.workbenchTickets, element: <TicketWorkbenchPage sessionState={sessionState} />, requiresAuth: true },
    { path: PATHS.workbenchTicketsNew, element: <TicketWorkbenchFormPage sessionState={sessionState} />, requiresAuth: true },
    { path: `${PATHS.workbenchTickets}/:id/edit`, element: <TicketWorkbenchFormPage sessionState={sessionState} />, requiresAuth: true },

    { path: PATHS.supportTickets, element: <SupportTicketsPage sessionState={sessionState} view="assigned" />, requiresAuth: true, allowedRoles: ['support'] },
    { path: PATHS.supportTicketsOpen, element: <SupportTicketsPage sessionState={sessionState} view="open" />, requiresAuth: true, allowedRoles: ['support'] },
    { path: PATHS.supportTicketsClosed, element: <SupportTicketsPage sessionState={sessionState} view="closed" />, requiresAuth: true, allowedRoles: ['support'] },
    { path: PATHS.supportTicketsNew, element: <SupportTicketCreatePage sessionState={sessionState} />, requiresAuth: true, allowedRoles: ['support'] },
    { path: `${PATHS.supportTickets}/:id`, element: <SupportTicketDetailPage sessionState={sessionState} />, requiresAuth: true, allowedRoles: ['support'] },

    { path: PATHS.userTickets, element: <SupportTicketsPage sessionState={sessionState} view="assigned" apiBase="/api/user/tickets" createFallbackPath={PATHS.userTicketsNew} />, requiresAuth: true, allowedRoles: ['user', 'tam'] },
    { path: PATHS.userTicketsOpen, element: <SupportTicketsPage sessionState={sessionState} view="open" apiBase="/api/user/tickets" createFallbackPath={PATHS.userTicketsNew} />, requiresAuth: true, allowedRoles: ['user', 'tam'] },
    { path: PATHS.userTicketsClosed, element: <SupportTicketsPage sessionState={sessionState} view="closed" apiBase="/api/user/tickets" createFallbackPath={PATHS.userTicketsNew} />, requiresAuth: true, allowedRoles: ['user', 'tam'] },
    { path: PATHS.userTicketsNew, element: <SupportTicketCreatePage sessionState={sessionState} apiBase="/api/user/tickets/bootstrap" backPath={PATHS.userTickets} submitFallbackPath={PATHS.userTickets} title="New ticket" navigateTo={PATHS.userTickets} compactCreateActions hideEntitlementLevel />, requiresAuth: true, allowedRoles: ['user', 'tam'] },
    { path: `${PATHS.userTickets}/:id`, element: <SupportTicketDetailPage sessionState={sessionState} apiBase="/api/user/tickets" backPath={PATHS.userTickets} titleFallback="Ticket" secondaryUsersLabel="TAM" />, requiresAuth: true, allowedRoles: ['user', 'tam'] },

    { path: PATHS.superuserTickets, element: <SupportTicketsPage sessionState={sessionState} view="assigned" apiBase="/api/superuser/tickets" createFallbackPath={PATHS.superuserTicketsNew} />, requiresAuth: true, allowedRoles: ['superuser'] },
    { path: PATHS.superuserTicketsOpen, element: <SupportTicketsPage sessionState={sessionState} view="open" apiBase="/api/superuser/tickets" createFallbackPath={PATHS.superuserTicketsNew} />, requiresAuth: true, allowedRoles: ['superuser'] },
    { path: PATHS.superuserTicketsClosed, element: <SupportTicketsPage sessionState={sessionState} view="closed" apiBase="/api/superuser/tickets" createFallbackPath={PATHS.superuserTicketsNew} />, requiresAuth: true, allowedRoles: ['superuser'] },
    { path: PATHS.superuserTicketsNew, element: <SupportTicketCreatePage sessionState={sessionState} apiBase="/api/superuser/tickets/bootstrap" backPath={PATHS.superuserTickets} submitFallbackPath={PATHS.superuserTickets} navigateTo={PATHS.superuserTickets} />, requiresAuth: true, allowedRoles: ['superuser'] },
    { path: `${PATHS.superuserTickets}/:id`, element: <SupportTicketDetailPage sessionState={sessionState} apiBase="/api/superuser/tickets" backPath={PATHS.superuserTickets} titleFallback="Superuser ticket" secondaryUsersLabel="Superusers" />, requiresAuth: true, allowedRoles: ['superuser'] }
  ];
}
