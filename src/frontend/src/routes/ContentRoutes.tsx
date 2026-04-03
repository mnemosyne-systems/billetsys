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

const ArticleDetailPage = lazy(() => import("../pages/ArticleDetailPage"));
const ArticleFormPage = lazy(() => import("../pages/ArticleFormPage"));
const ArticlesPage = lazy(() => import("../pages/ArticlesPage"));
const CategoriesPage = lazy(() => import("../pages/CategoriesPage"));
const CategoryDetailPage = lazy(() => import("../pages/CategoryDetailPage"));
const CategoryFormPage = lazy(() => import("../pages/CategoryFormPage"));
const CompaniesPage = lazy(() => import("../pages/CompaniesPage"));
const CompanyDetailPage = lazy(() => import("../pages/CompanyDetailPage"));
const CompanyFormPage = lazy(() => import("../pages/CompanyFormPage"));
const EntitlementDetailPage = lazy(
  () => import("../pages/EntitlementDetailPage"),
);
const EntitlementFormPage = lazy(
  () => import("../pages/EntitlementEditorPage"),
);
const EntitlementsPage = lazy(() => import("../pages/EntitlementsListPage"));
const LevelDetailPage = lazy(() => import("../pages/LevelDetailPage"));
const LevelFormPage = lazy(() => import("../pages/LevelFormPage"));
const LevelsPage = lazy(() => import("../pages/LevelsPage"));

export function getContentRoutes(sessionState: SessionState): AppRoute[] {
  return [
    {
      path: "/companies",
      element: <CompaniesPage sessionState={sessionState} />,
    },
    {
      path: "/companies/new",
      element: <CompanyFormPage sessionState={sessionState} mode="create" />,
    },
    {
      path: "/companies/:id",
      element: <CompanyDetailPage sessionState={sessionState} />,
    },
    {
      path: "/companies/:id/edit",
      element: <CompanyFormPage sessionState={sessionState} mode="edit" />,
    },

    {
      path: "/articles",
      element: <ArticlesPage sessionState={sessionState} />,
    },
    {
      path: "/articles/new",
      element: <ArticleFormPage sessionState={sessionState} mode="create" />,
    },
    {
      path: "/articles/:id",
      element: <ArticleDetailPage sessionState={sessionState} />,
    },
    {
      path: "/articles/:id/edit",
      element: <ArticleFormPage sessionState={sessionState} mode="edit" />,
    },

    {
      path: "/categories",
      element: <CategoriesPage sessionState={sessionState} />,
    },
    {
      path: "/categories/new",
      element: <CategoryFormPage sessionState={sessionState} mode="create" />,
    },
    {
      path: "/categories/:id",
      element: <CategoryDetailPage sessionState={sessionState} />,
    },
    {
      path: "/categories/:id/edit",
      element: <CategoryFormPage sessionState={sessionState} mode="edit" />,
    },

    {
      path: "/entitlements",
      element: <EntitlementsPage sessionState={sessionState} />,
    },
    {
      path: "/entitlements/new",
      element: (
        <EntitlementFormPage sessionState={sessionState} mode="create" />
      ),
    },
    {
      path: "/entitlements/:id",
      element: <EntitlementDetailPage sessionState={sessionState} />,
    },
    {
      path: "/entitlements/:id/edit",
      element: <EntitlementFormPage sessionState={sessionState} mode="edit" />,
    },

    {
      path: PATHS.levels,
      element: <LevelsPage sessionState={sessionState} />,
      requiresAuth: true,
    },
    {
      path: PATHS.levelsNew,
      element: <LevelFormPage sessionState={sessionState} mode="create" />,
      requiresAuth: true,
    },
    {
      path: `${PATHS.levels}/:id`,
      element: <LevelDetailPage sessionState={sessionState} />,
      requiresAuth: true,
    },
    {
      path: `${PATHS.levels}/:id/edit`,
      element: <LevelFormPage sessionState={sessionState} mode="edit" />,
      requiresAuth: true,
    },
  ];
}
