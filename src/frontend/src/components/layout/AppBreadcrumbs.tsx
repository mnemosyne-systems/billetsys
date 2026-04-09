/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Link, useLocation } from "react-router-dom";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";

/**
 * Route prefixes that act as role namespaces, not real pages.
 * These are stripped from the breadcrumb trail so users never see
 * a link to a non-existent "/superuser" or "/user" page.
 */
const ROLE_PREFIXES = ["superuser", "user", "support"];

/**
 * Formats a raw URL segment into a human-readable label.
 * - Replaces hyphens/underscores with spaces
 * - Capitalizes each word
 */
function formatSegment(segment: string): string {
  return segment
    .replace(/[-_]/g, " ")
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

/**
 * Global dynamic breadcrumb component.
 *
 * Reads the current URL and automatically generates a breadcrumb trail.
 * Handles role-prefixed routes (e.g. /superuser/tickets) by stripping
 * the non-navigable role segment and treating the role's ticket page
 * as the effective home.
 */
export default function AppBreadcrumbs() {
  const { pathname } = useLocation();

  const rawSegments = pathname.split("/").filter(Boolean);

  if (rawSegments.length === 0) return null;

  // Detect and strip the role prefix (superuser, user, support)
  const hasRolePrefix = ROLE_PREFIXES.includes(rawSegments[0]);
  const rolePrefix = hasRolePrefix ? rawSegments[0] : null;
  const segments = hasRolePrefix ? rawSegments.slice(1) : rawSegments;

  // For role-prefixed routes, the first real segment (e.g. "tickets")
  // is their home page -- hide breadcrumbs when on home
  if (segments.length === 0) return null;
  if (hasRolePrefix && segments.length === 1) return null;

  // Build the base path prefix for href generation
  const basePath = rolePrefix ? `/${rolePrefix}` : "";

  // Dynamic layout alignment: breadcrumbs should match the maximum width of the page container beneath them.
  let wrapperClass = "w-full max-w-5xl mx-auto px-1";

  if (hasRolePrefix && segments[0] === "tickets") {
    const subRoute = segments[1];

    // SupportTicketCreatePage
    if (subRoute === "new") {
      wrapperClass = "w-full max-w-5xl mx-auto px-1";
    }
    // SupportTicketsPage lists
    else if (!subRoute || ["open", "closed", "assigned"].includes(subRoute)) {
      wrapperClass = "w-full px-1";
    }
    // SupportTicketDetailPage
    else {
      wrapperClass = "w-full px-4 md:px-8 xl:px-12 mx-auto";
    }
  } else if (hasRolePrefix && segments[0] === "reports") {
    wrapperClass = "w-full max-w-7xl mx-auto px-1";
  }

  return (
    <div className={wrapperClass}>
      <Breadcrumb>
        <BreadcrumbList>
          {!hasRolePrefix && (
            <span key="home" className="inline-flex items-center gap-1.5">
              <BreadcrumbItem>
                <BreadcrumbLink asChild>
                  <Link to="/">Home</Link>
                </BreadcrumbLink>
              </BreadcrumbItem>
              <BreadcrumbSeparator />
            </span>
          )}
          {segments.map((segment, index) => {
            const href =
              basePath + "/" + segments.slice(0, index + 1).join("/");
            const isLast = index === segments.length - 1;

            return (
              <span key={href} className="inline-flex items-center gap-1.5">
                {index > 0 && <BreadcrumbSeparator />}
                <BreadcrumbItem>
                  {isLast ? (
                    <BreadcrumbPage>{formatSegment(segment)}</BreadcrumbPage>
                  ) : (
                    <BreadcrumbLink asChild>
                      <Link to={href}>{formatSegment(segment)}</Link>
                    </BreadcrumbLink>
                  )}
                </BreadcrumbItem>
              </span>
            );
          })}
        </BreadcrumbList>
      </Breadcrumb>
    </div>
  );
}
