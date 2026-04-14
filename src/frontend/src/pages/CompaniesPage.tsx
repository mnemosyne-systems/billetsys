/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Link } from "react-router-dom";
import useJson from "../hooks/useJson";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, CompanyRecord } from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";

export default function CompaniesPage(props: SessionPageProps) {
  void props;
  const companiesState =
    useJson<CollectionResponse<CompanyRecord>>("/api/companies");

  return (
    <section className="w-full mt-4">
      <PageHeader
        title="Companies"
        actions={
          <Button asChild>
            <SmartLink
              href={companiesState.data?.createPath || "/companies/new"}
            >
              Create
            </SmartLink>
          </Button>
        }
      />

      <DataState
        state={companiesState}
        emptyMessage="No companies are available yet."
      >
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {companiesState.data?.items.map((company: CompanyRecord) => (
            <Card
              key={company.id}
              className="hover:shadow-md transition-shadow"
            >
              <CardHeader>
                <h3 className="font-semibold leading-none tracking-tight">
                  <Link
                    className="text-primary hover:underline hover:text-primary/80"
                    to={`/companies/${company.id}`}
                  >
                    {company.name}
                  </Link>
                </h3>
                <p className="text-sm text-muted-foreground mt-2">
                  {[company.countryName, company.timezoneName]
                    .filter(Boolean)
                    .join(" • ") || "No locale configured"}
                </p>
                <p className="text-sm text-muted-foreground mt-1">
                  {company.superuserCount} superuser
                  {company.superuserCount === 1 ? "" : "s"} •{" "}
                  {company.userCount} users • {company.tamCount} TAMs
                </p>
              </CardHeader>
            </Card>
          ))}
        </div>
      </DataState>
    </section>
  );
}
