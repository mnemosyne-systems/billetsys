/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { MouseEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import useJson from "../hooks/useJson";
import DataState from "../components/common/DataState";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  DirectoryCompanyDetail as DirectoryCompanyDetailType,
  DirectoryUserRecord,
} from "../types/domain";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "../components/ui/card";

interface DirectoryUserReferenceListProps {
  users?: DirectoryUserRecord[];
}

function DirectoryUserReferenceList({
  users,
}: DirectoryUserReferenceListProps) {
  if (!users || users.length === 0) {
    return <p className="text-sm text-muted-foreground">No users.</p>;
  }

  return (
    <ul className="space-y-2 text-sm">
      {users.map((user: DirectoryUserRecord) => (
        <li
          key={user.id || `${user.username}-${user.type}`}
          className="flex items-center justify-between py-1 border-b last:border-0 border-border"
        >
          {user.detailPath ? (
            <SmartLink
              className="font-medium text-primary hover:underline"
              href={user.detailPath}
            >
              {user.displayName || user.username || "User"}
            </SmartLink>
          ) : (
            <span className="font-medium">
              {user.displayName || user.username || "User"}
            </span>
          )}
          <span className="text-muted-foreground ml-2">
            ({user.typeLabel || user.type || "User"})
          </span>
        </li>
      ))}
    </ul>
  );
}

interface DirectoryCompanyDetailPageProps extends SessionPageProps {
  apiBase: string;
  backFallback: string;
}

export default function DirectoryCompanyDetailPage({
  sessionState,
  apiBase,
  backFallback,
}: DirectoryCompanyDetailPageProps) {
  const navigate = useNavigate();
  const { id } = useParams();
  const companyState = useJson<DirectoryCompanyDetailType>(
    id ? `${apiBase}/${id}` : null,
  );
  const company = companyState.data;
  const resolvedBackHref = company?.backPath || backFallback;

  const handleBackClick = (event: MouseEvent<HTMLAnchorElement>) => {
    if (window.history.length > 1) {
      event.preventDefault();
      navigate(-1);
    }
  };

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <div className="flex items-center gap-4 pb-6 px-1">
        <SmartLink
          className="text-muted-foreground hover:text-foreground transition-colors flex items-center gap-1 text-sm font-medium"
          href={resolvedBackHref}
          onClick={handleBackClick}
        >
          ← Back
        </SmartLink>
        <h2 className="text-3xl font-bold tracking-tight">
          {company?.name || "Company details"}
        </h2>
      </div>

      <DataState
        state={companyState}
        emptyMessage="Company not found."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {company && (
          <div className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    Phone
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="font-medium text-lg">
                    {company.phoneNumber || "—"}
                  </p>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    Country
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="font-medium text-lg">
                    {company.countryName || "—"}
                  </p>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    Time zone
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="font-medium text-lg">
                    {company.timezoneName || "—"}
                  </p>
                </CardContent>
              </Card>
            </div>

            <div className="grid gap-6 md:grid-cols-2">
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Address</CardTitle>
                </CardHeader>
                <CardContent>
                  <ul className="space-y-1 text-sm">
                    {[
                      company.address1,
                      company.address2,
                      company.city,
                      company.state,
                      company.zip,
                    ].filter(Boolean).length === 0 ? (
                      <li className="text-muted-foreground">—</li>
                    ) : (
                      [
                        company.address1,
                        company.address2,
                        company.city,
                        company.state,
                        company.zip,
                      ]
                        .filter(Boolean)
                        .map((line) => <li key={line}>{line}</li>)
                    )}
                  </ul>
                </CardContent>
              </Card>
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Users</CardTitle>
                </CardHeader>
                <CardContent>
                  <DirectoryUserReferenceList users={company.users} />
                </CardContent>
              </Card>
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Superusers</CardTitle>
                </CardHeader>
                <CardContent>
                  <DirectoryUserReferenceList users={company.superusers} />
                </CardContent>
              </Card>
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">TAMs</CardTitle>
                </CardHeader>
                <CardContent>
                  <DirectoryUserReferenceList users={company.tamUsers} />
                </CardContent>
              </Card>
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Support users</CardTitle>
                </CardHeader>
                <CardContent>
                  <DirectoryUserReferenceList users={company.supportUsers} />
                </CardContent>
              </Card>
            </div>
          </div>
        )}
      </DataState>
    </section>
  );
}
