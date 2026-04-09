/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Navigate } from "react-router-dom";
import { SmartLink, normalizeClientPath } from "../utils/routing";
import { orderedNavigation } from "../utils/navigation";
import type { SessionPageProps } from "../types/app";
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

  if (sessionState.loading) {
    return (
      <Card className="w-full max-w-5xl mx-auto mt-4">
        <CardContent className="p-6 flex items-center gap-2 text-muted-foreground">
          <Spinner />
          <span className="text-sm font-medium">Loading session...</span>
        </CardContent>
      </Card>
    );
  }

  if (sessionState.error) {
    return (
      <Card className="w-full max-w-5xl mx-auto mt-4">
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
          <Shield className="w-6 h-6 text-muted-foreground mb-3 group-hover:text-foreground transition-colors" />
        );
      case "companies":
        return (
          <Building2 className="w-6 h-6 text-muted-foreground mb-3 group-hover:text-foreground transition-colors" />
        );
      case "users":
        return (
          <Users className="w-6 h-6 text-muted-foreground mb-3 group-hover:text-foreground transition-colors" />
        );
      case "entitlements":
        return (
          <Key className="w-6 h-6 text-muted-foreground mb-3 group-hover:text-foreground transition-colors" />
        );
      case "levels":
        return (
          <Layers className="w-6 h-6 text-muted-foreground mb-3 group-hover:text-foreground transition-colors" />
        );
      case "categories":
        return (
          <Tags className="w-6 h-6 text-muted-foreground mb-3 group-hover:text-foreground transition-colors" />
        );
      case "articles":
        return (
          <BookOpen className="w-6 h-6 text-muted-foreground mb-3 group-hover:text-foreground transition-colors" />
        );
      case "reports":
        return (
          <BarChart3 className="w-6 h-6 text-muted-foreground mb-3 group-hover:text-foreground transition-colors" />
        );
      default:
        return (
          <Settings className="w-6 h-6 text-muted-foreground mb-3 group-hover:text-foreground transition-colors" />
        );
    }
  };

  const homePath = normalizeClientPath(session.homePath) || "/";
  if (homePath !== "/") {
    return <Navigate replace to={homePath} />;
  }

  return (
    <div className="w-full max-w-6xl mx-auto mt-2 pb-12">
      <div className="mb-8 px-2 border-b pb-4">
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">
          System Administration
        </h1>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 px-2">
        {adminLinks.map((link) => (
          <SmartLink
            key={link.href}
            href={link.href}
            className="group flex flex-col p-5 h-36 rounded-sm bg-card border shadow-sm hover:border-foreground/30 transition-all overflow-hidden"
          >
            <div className="relative z-10 flex flex-col h-full">
              {getIconForLabel(link.label)}

              <h3 className="mt-auto text-[15px] font-medium tracking-tight text-foreground/90 group-hover:text-foreground transition-colors">
                {link.label}
              </h3>
            </div>
          </SmartLink>
        ))}
      </div>
    </div>
  );
}
