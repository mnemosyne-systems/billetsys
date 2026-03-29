import { lazy } from 'react';
import { PATHS } from './paths';
import type { AppRoute, SessionState } from '../types/app';

const AttachmentPage = lazy(() => import('../pages/AttachmentPage'));
const MessageFormPage = lazy(() => import('../pages/MessageFormPage'));
const MessagesPage = lazy(() => import('../pages/MessagesPage'));

export function getMessagingRoutes(sessionState: SessionState): AppRoute[] {
  return [
    { path: PATHS.messages, element: <MessagesPage sessionState={sessionState} />, requiresAuth: true },
    { path: PATHS.messagesNew, element: <MessageFormPage sessionState={sessionState} />, requiresAuth: true },
    { path: `${PATHS.messages}/:id/edit`, element: <MessageFormPage sessionState={sessionState} />, requiresAuth: true },
    { path: '/attachments/:id', element: <AttachmentPage sessionState={sessionState} />, requiresAuth: true }
  ];
}
