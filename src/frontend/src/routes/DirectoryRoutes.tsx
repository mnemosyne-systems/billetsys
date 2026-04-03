/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { lazy } from "react";
import type { AppRoute, SessionState } from "../types/app";

const DirectoryCompanyDetailPage = lazy(
  () => import("../pages/DirectoryCompanyDetailPage"),
);
const DirectoryUserDetailPageRoute = lazy(
  () => import("../pages/DirectoryUserDetailPage"),
);
const DirectoryUserFormPageRoute = lazy(
  () => import("../pages/DirectoryUserFormPage"),
);
const DirectoryUsersPageRoute = lazy(
  () => import("../pages/DirectoryUsersPage"),
);

export function getDirectoryRoutes(sessionState: SessionState): AppRoute[] {
  return [
    {
      path: "/users",
      element: (
        <DirectoryUsersPageRoute
          sessionState={sessionState}
          apiBase="/api/admin/users"
          basePath="/users"
          titleFallback="Users"
          description=""
        />
      ),
      requiresAuth: true,
    },
    {
      path: "/users/new",
      element: (
        <DirectoryUserFormPageRoute
          sessionState={sessionState}
          bootstrapBase="/api/admin/users/bootstrap"
          navigateFallback="/users"
        />
      ),
      requiresAuth: true,
    },
    {
      path: "/users/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/admin/users"
          backFallback="/users"
        />
      ),
      requiresAuth: true,
    },
    {
      path: "/users/:id/edit",
      element: (
        <DirectoryUserFormPageRoute
          sessionState={sessionState}
          bootstrapBase="/api/admin/users/bootstrap"
          navigateFallback="/users"
        />
      ),
      requiresAuth: true,
    },

    {
      path: "/support/users",
      element: (
        <DirectoryUsersPageRoute
          sessionState={sessionState}
          apiBase="/api/support/users"
          basePath="/support/users"
          titleFallback="Users"
          description=""
        />
      ),
      requiresAuth: true,
      allowedRoles: ["support"],
    },
    {
      path: "/support/users/new",
      element: (
        <DirectoryUserFormPageRoute
          sessionState={sessionState}
          bootstrapBase="/api/support/users/bootstrap"
          navigateFallback="/support/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["support"],
    },
    {
      path: "/support/support-users/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/support/support-users"
          backFallback="/support/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["support"],
    },
    {
      path: "/support/tam-users/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/support/tam-users"
          backFallback="/support/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["support"],
    },
    {
      path: "/support/superuser-users/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/support/superuser-users"
          backFallback="/support/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["support"],
    },
    {
      path: "/support/user-profiles/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/support/user-profiles"
          backFallback="/support/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["support"],
    },
    {
      path: "/support/companies/:id",
      element: (
        <DirectoryCompanyDetailPage
          sessionState={sessionState}
          apiBase="/api/support/companies"
          backFallback="/support/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["support"],
    },

    {
      path: "/tam/users",
      element: (
        <DirectoryUsersPageRoute
          sessionState={sessionState}
          apiBase="/api/tam/users"
          basePath="/tam/users"
          titleFallback="Users"
          description=""
        />
      ),
      requiresAuth: true,
      allowedRoles: ["tam"],
    },
    {
      path: "/tam/users/new",
      element: (
        <DirectoryUserFormPageRoute
          sessionState={sessionState}
          bootstrapBase="/api/tam/users/bootstrap"
          navigateFallback="/tam/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["tam"],
    },

    {
      path: "/user/support-users/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/user/support-users"
          backFallback="/user/tickets"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["user", "tam"],
    },
    {
      path: "/user/tam-users/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/user/tam-users"
          backFallback="/user/tickets"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["user", "tam"],
    },
    {
      path: "/user/superuser-users/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/user/superuser-users"
          backFallback="/user/tickets"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["user", "tam"],
    },
    {
      path: "/user/user-profiles/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/user/user-profiles"
          backFallback="/user/tickets"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["user", "tam"],
    },
    {
      path: "/user/companies/:id",
      element: (
        <DirectoryCompanyDetailPage
          sessionState={sessionState}
          apiBase="/api/user/companies"
          backFallback="/user/tickets"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["user", "tam"],
    },

    {
      path: "/superuser/users",
      element: (
        <DirectoryUsersPageRoute
          sessionState={sessionState}
          apiBase="/api/superuser/users"
          basePath="/superuser/users"
          titleFallback="Users"
          description=""
        />
      ),
      requiresAuth: true,
      allowedRoles: ["superuser"],
    },
    {
      path: "/superuser/users/new",
      element: (
        <DirectoryUserFormPageRoute
          sessionState={sessionState}
          bootstrapBase="/api/superuser/users/bootstrap"
          navigateFallback="/superuser/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["superuser"],
    },
    {
      path: "/superuser/support-users/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/superuser/support-users"
          backFallback="/superuser/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["superuser"],
    },
    {
      path: "/superuser/superuser-users/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/superuser/superuser-users"
          backFallback="/superuser/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["superuser"],
    },
    {
      path: "/superuser/user-profiles/:id",
      element: (
        <DirectoryUserDetailPageRoute
          sessionState={sessionState}
          apiBase="/api/superuser/user-profiles"
          backFallback="/superuser/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["superuser"],
    },
    {
      path: "/superuser/companies/:id",
      element: (
        <DirectoryCompanyDetailPage
          sessionState={sessionState}
          apiBase="/api/superuser/companies"
          backFallback="/superuser/users"
        />
      ),
      requiresAuth: true,
      allowedRoles: ["superuser"],
    },
  ];
}
