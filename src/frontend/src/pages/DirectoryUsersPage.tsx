/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useLocation, useNavigate } from "react-router-dom";
import DataState from "../components/common/DataState";
import useJson from "../hooks/useJson";
import { toQueryString } from "../utils/formatting";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  DirectoryUserRecord,
  DirectoryUsersResponse,
  NamedEntity,
} from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";

interface DirectoryUsersPageProps extends SessionPageProps {
  apiBase: string;
  basePath: string;
  titleFallback: string;
  description?: string;
}

export default function DirectoryUsersPage({
  sessionState,
  apiBase,
  titleFallback,
  description = "",
}: DirectoryUsersPageProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const companyId = new URLSearchParams(location.search).get("companyId") || "";
  const dataState = useJson<DirectoryUsersResponse>(
    `${apiBase}${toQueryString({ companyId })}`,
  );
  const directory = dataState.data;

  const selectCompany = (nextCompanyId: string) => {
    navigate(
      `${location.pathname}${toQueryString({ companyId: nextCompanyId })}`,
    );
  };

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <div className="flex flex-row items-center justify-between pb-6 px-1">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">
            {directory?.title || titleFallback}
          </h2>
          {directory?.description || description ? (
            <p className="text-muted-foreground mt-1">
              {directory?.description || description}
            </p>
          ) : null}
        </div>
        <div>
          {directory?.createPath && (
            <Button asChild>
              <SmartLink href={directory.createPath}>Create</SmartLink>
            </Button>
          )}
        </div>
      </div>

      <DataState
        state={dataState}
        emptyMessage="No users are available."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        <div className="grid gap-6">
          {directory?.showCompanySelector && (
            <div className="grid gap-1.5 max-w-xs">
              <label className="text-sm font-medium leading-none">
                Company
              </label>
              <Select
                value={
                  directory.selectedCompanyId
                    ? String(directory.selectedCompanyId)
                    : undefined
                }
                onValueChange={selectCompany}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select company" />
                </SelectTrigger>
                <SelectContent position="popper">
                  {(directory.companies || []).map((company: NamedEntity) => (
                    <SelectItem
                      key={company.id}
                      value={company.id ? String(company.id) : "none"}
                    >
                      {company.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {(directory?.items || []).map((user: DirectoryUserRecord) => (
              <Card key={user.id} className="hover:shadow-md transition-shadow">
                <CardHeader>
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-semibold leading-none tracking-tight">
                      {user.detailPath ? (
                        <SmartLink
                          className="text-primary hover:underline hover:text-primary/80"
                          href={user.detailPath}
                        >
                          {user.displayName ||
                            user.fullName ||
                            user.username ||
                            "User"}
                        </SmartLink>
                      ) : (
                        user.displayName ||
                        user.fullName ||
                        user.username ||
                        "User"
                      )}
                    </h3>
                    <Badge
                      variant="secondary"
                      className="whitespace-nowrap font-normal"
                    >
                      {user.typeLabel || user.type || "User"}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground mt-3 line-clamp-2">
                    {user.email || "No email"}
                  </p>
                  <p className="text-sm text-muted-foreground mt-1">
                    @{user.username || "unknown"}
                  </p>
                </CardHeader>
              </Card>
            ))}
          </div>

          {(!directory?.items || directory.items.length === 0) && (
            <p className="text-muted-foreground">
              No users are available for the selected company.
            </p>
          )}
        </div>
      </DataState>
    </section>
  );
}
