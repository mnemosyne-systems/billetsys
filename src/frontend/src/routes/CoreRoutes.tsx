/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import HomePage from "../pages/HomePage";
import LoginPage from "../pages/LoginPage";
import OwnerPage, { OwnerEditPage } from "../pages/OwnerPage";
import PasswordPage from "../pages/PasswordPage";
import ProfilePage from "../pages/ProfilePage";
import ReportsPage from "../pages/ReportsPage";
import StatusPage from "../pages/StatusPage";
import { PATHS } from "./paths";
import type { AppRoute, SessionState } from "../types/app";
import ForgotPasswordPage from "../pages/ForgotPasswordPage";
import ResetPasswordPage from "../pages/ResetPasswordPage";

export function getCoreRoutes(sessionState: SessionState): AppRoute[] {
  return [
    { path: PATHS.root, element: <HomePage sessionState={sessionState} /> },
    { path: PATHS.login, element: <LoginPage sessionState={sessionState} /> },
    {
      path: PATHS.forgotPassword,
      element: <ForgotPasswordPage sessionState={sessionState} />,
    },
    {
      path: PATHS.resetPassword,
      element: <ResetPasswordPage sessionState={sessionState} />,
    },
    {
      path: PATHS.error,
      element: (
        <StatusPage
          sessionState={sessionState}
          title="Something went wrong"
          message="An unexpected error interrupted the request before the React shell could finish loading the page."
        />
      ),
    },
    {
      path: PATHS.notFound,
      element: (
        <StatusPage
          sessionState={sessionState}
          title="Page not found"
          message="The page you requested does not exist or is no longer available in the React shell."
        />
      ),
    },
    {
      path: PATHS.profile,
      element: <ProfilePage sessionState={sessionState} />,
      requiresAuth: true,
    },
    {
      path: PATHS.profilePassword,
      element: <PasswordPage sessionState={sessionState} />,
      requiresAuth: true,
    },
    {
      path: PATHS.reports,
      element: <ReportsPage sessionState={sessionState} />,
      requiresAuth: true,
    },
    {
      path: PATHS.owner,
      element: <OwnerPage sessionState={sessionState} />,
      requiresAuth: true,
    },
    {
      path: PATHS.ownerEdit,
      element: <OwnerEditPage sessionState={sessionState} />,
      requiresAuth: true,
    },
  ];
}
