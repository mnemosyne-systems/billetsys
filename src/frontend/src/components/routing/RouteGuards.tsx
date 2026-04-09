/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import LoadingSpinner from "../common/LoadingSpinner";
import StatusPage from "../../pages/StatusPage";
import { PATHS } from "../../routes/paths";
import type { Role, SessionPageProps } from "../../types/app";

function GuardLoading() {
  return (
    <section className="w-full max-w-5xl mx-auto mt-6 text-center text-muted-foreground py-10 border rounded-lg bg-card">
      <LoadingSpinner label="Loading session..." />
    </section>
  );
}

interface RequireAuthProps extends SessionPageProps {
  children?: ReactNode;
}

interface RequireRoleProps extends RequireAuthProps {
  allowedRoles: Role[];
}

export function RequireAuth({ sessionState, children }: RequireAuthProps) {
  const location = useLocation();

  if (sessionState.loading) {
    return <GuardLoading />;
  }

  if (!sessionState.data?.authenticated) {
    return <Navigate replace to={PATHS.login} state={{ from: location }} />;
  }

  return children;
}

export function RequireRole({
  sessionState,
  allowedRoles,
  children,
}: RequireRoleProps) {
  if (sessionState.loading) {
    return <GuardLoading />;
  }

  if (!sessionState.data?.authenticated) {
    return <Navigate replace to={PATHS.login} />;
  }

  const role = sessionState.data?.role;

  if (!role || !allowedRoles.includes(role)) {
    return (
      <StatusPage
        sessionState={sessionState}
        title="Access denied"
        message="You do not have permission to view this route in the React shell."
      />
    );
  }

  return children;
}
