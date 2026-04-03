/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { lazy } from "react";
import { PATHS } from "./paths";
import type { AppRoute, SessionState } from "../types/app";

const AttachmentPage = lazy(() => import("../pages/AttachmentPage"));
const MessageFormPage = lazy(() => import("../pages/MessageFormPage"));
const MessagesPage = lazy(() => import("../pages/MessagesPage"));

export function getMessagingRoutes(sessionState: SessionState): AppRoute[] {
  return [
    {
      path: PATHS.messages,
      element: <MessagesPage sessionState={sessionState} />,
      requiresAuth: true,
    },
    {
      path: PATHS.messagesNew,
      element: <MessageFormPage sessionState={sessionState} />,
      requiresAuth: true,
    },
    {
      path: `${PATHS.messages}/:id/edit`,
      element: <MessageFormPage sessionState={sessionState} />,
      requiresAuth: true,
    },
    {
      path: "/attachments/:id",
      element: <AttachmentPage sessionState={sessionState} />,
      requiresAuth: true,
    },
  ];
}
