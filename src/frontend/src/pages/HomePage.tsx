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
      <section className="panel">
        <p>Loading session...</p>
      </section>
    );
  }

  if (sessionState.error) {
    return (
      <section className="panel">
        <p className="error-text">{sessionState.error}</p>
      </section>
    );
  }

  if (!session?.authenticated) {
    return <Navigate replace to="/login" />;
  }

  const homePath = normalizeClientPath(session.homePath) || "/";
  if (homePath !== "/") {
    return <Navigate replace to={homePath} />;
  }

  return (
    <section className="dashboard-panel">
      <div className="dashboard-card-grid">
        {adminLinks.map((link) => (
          <SmartLink
            key={link.href}
            className="dashboard-card"
            href={link.href}
          >
            {link.label}
          </SmartLink>
        ))}
      </div>
    </section>
  );
}
