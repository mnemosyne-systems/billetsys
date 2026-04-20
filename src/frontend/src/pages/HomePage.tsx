/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Navigate } from "react-router-dom";
import useJson from "../hooks/useJson";
import { SmartLink, normalizeClientPath } from "../utils/routing";
import { orderedNavigation } from "../utils/navigation";
import type { SessionPageProps } from "../types/app";
import type {
  CollectionResponse,
  DirectoryUsersResponse,
  NamedEntity,
  OwnerCompany,
} from "../types/domain";
import PageHeader from "../components/layout/PageHeader";
import { Card, CardContent } from "../components/ui/card";
import { Spinner } from "../components/ui/spinner";
import {
  Building2,
  Users,
  Key,
  Layers,
  Tags,
  BookOpen,
  BarChart3,
  Shield,
  Settings,
} from "lucide-react";

export default function HomePage({ sessionState }: SessionPageProps) {
  const session = sessionState.data;
  const homePath = normalizeClientPath(session?.homePath) || "/";
  const isAdminDashboard = session?.authenticated && homePath === "/";
  const adminLinks = orderedNavigation(session?.navigation, [
    "Owner",
    "Companies",
    "Users",
    "Entitlements",
    "Levels",
    "Categories",
    "Articles",
    "Reports",
  ]);
  const ownerState = useJson<OwnerCompany>(
    isAdminDashboard ? "/api/owner" : null,
  );
  const companiesState = useJson<CollectionResponse<NamedEntity>>(
    isAdminDashboard ? "/api/companies" : null,
  );
  const usersState = useJson<DirectoryUsersResponse>(
    isAdminDashboard ? "/api/admin/users" : null,
  );
  const entitlementsState = useJson<CollectionResponse<NamedEntity>>(
    isAdminDashboard ? "/api/entitlements" : null,
  );
  const levelsState = useJson<CollectionResponse<NamedEntity>>(
    isAdminDashboard ? "/api/levels" : null,
  );
  const categoriesState = useJson<CollectionResponse<NamedEntity>>(
    isAdminDashboard ? "/api/categories" : null,
  );
  const articlesState = useJson<CollectionResponse<NamedEntity>>(
    isAdminDashboard ? "/api/articles" : null,
  );

  if (sessionState.loading) {
    return (
      <Card className="w-full mt-4">
        <CardContent className="p-6 flex items-center gap-2 text-muted-foreground">
          <Spinner />
          <span className="text-sm font-medium">Loading session...</span>
        </CardContent>
      </Card>
    );
  }

  if (sessionState.error) {
    return (
      <Card className="w-full mt-4">
        <CardContent className="p-6">
          <p className="text-destructive font-semibold">{sessionState.error}</p>
        </CardContent>
      </Card>
    );
  }

  if (!session?.authenticated) {
    return <Navigate replace to="/login" />;
  }

  const getIconForLabel = (label: string) => {
    switch (label.toLowerCase()) {
      case "owner":
        return (
          <Shield className="h-6 w-6 shrink-0 text-[var(--color-header-bg)] transition-colors group-hover:text-[var(--color-primary-dark)]" />
        );
      case "companies":
        return (
          <Building2 className="h-6 w-6 shrink-0 text-sky-600 transition-colors group-hover:text-sky-700" />
        );
      case "users":
        return (
          <Users className="h-6 w-6 shrink-0 text-emerald-600 transition-colors group-hover:text-emerald-700" />
        );
      case "entitlements":
        return (
          <Key className="h-6 w-6 shrink-0 text-amber-600 transition-colors group-hover:text-amber-700" />
        );
      case "levels":
        return (
          <Layers className="h-6 w-6 shrink-0 text-violet-600 transition-colors group-hover:text-violet-700" />
        );
      case "categories":
        return (
          <Tags className="h-6 w-6 shrink-0 text-rose-600 transition-colors group-hover:text-rose-700" />
        );
      case "articles":
        return (
          <BookOpen className="h-6 w-6 shrink-0 text-indigo-600 transition-colors group-hover:text-indigo-700" />
        );
      case "reports":
        return (
          <BarChart3 className="h-6 w-6 shrink-0 text-orange-600 transition-colors group-hover:text-orange-700" />
        );
      default:
        return (
          <Settings className="h-6 w-6 shrink-0 text-slate-600 transition-colors group-hover:text-slate-700" />
        );
    }
  };

  const countLabel = (items?: unknown[]) =>
    Array.isArray(items) ? String(items.length) : "";

  const summaryForLabel = (label: string) => {
    switch (label.toLowerCase()) {
      case "owner":
        return ownerState.data?.name || session?.installationCompanyName || "";
      case "companies":
        return countLabel(companiesState.data?.items);
      case "users":
        return countLabel(usersState.data?.items);
      case "entitlements":
        return countLabel(entitlementsState.data?.items);
      case "levels":
        return countLabel(levelsState.data?.items);
      case "categories":
        return countLabel(categoriesState.data?.items);
      case "articles":
        return countLabel(articlesState.data?.items);
      default:
        return "";
    }
  };

  if (homePath !== "/") {
    return <Navigate replace to={homePath} />;
  }

  return (
    <div className="w-full mt-2 pb-12">
      <PageHeader title="Administration" />

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 px-2">
        {adminLinks.map((link) => (
          <SmartLink
            key={link.href}
            href={link.href}
            className="group flex flex-col p-5 h-36 rounded-sm bg-card border shadow-sm hover:border-foreground/30 transition-all overflow-hidden"
          >
            <div className="relative z-10 flex h-full flex-col justify-between gap-4">
              <div className="flex items-center gap-3">
                {getIconForLabel(link.label)}
                <h3 className="min-w-0 flex-1 text-[15px] font-medium tracking-tight text-foreground/90 transition-colors group-hover:text-foreground">
                  {link.label}
                </h3>
              </div>

              {summaryForLabel(link.label) ? (
                <p className="line-clamp-2 text-lg font-medium leading-snug text-muted-foreground">
                  {summaryForLabel(link.label)}
                </p>
              ) : null}
            </div>
          </SmartLink>
        ))}
      </div>
    </div>
  );
}
