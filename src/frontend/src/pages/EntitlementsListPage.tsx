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
import type { CollectionResponse, EntitlementRecord } from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";

type SupportLevelListItem = string | { id?: string | number; name?: string };

function supportLevelLabel(level: SupportLevelListItem): string {
  return typeof level === "string" ? level : level.name || "Unnamed level";
}

function sortedSupportLevels(
  supportLevels?: Array<SupportLevelListItem>,
): SupportLevelListItem[] {
  return [...(supportLevels || [])].sort((left, right) =>
    supportLevelLabel(left).localeCompare(supportLevelLabel(right), undefined, {
      sensitivity: "base",
    }),
  );
}

export default function EntitlementsListPage(props: SessionPageProps) {
  void props;
  const entitlementsState =
    useJson<CollectionResponse<EntitlementRecord>>("/api/entitlements");

  return (
    <section className="w-full mt-4">
      <PageHeader
        title="Entitlements"
        actions={
          <Button asChild>
            <SmartLink href="/entitlements/new">Create</SmartLink>
          </Button>
        }
      />

      <DataState
        state={entitlementsState}
        emptyMessage="No entitlements are available yet."
      >
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {entitlementsState.data?.items.map(
            (entitlement: EntitlementRecord) => (
              <Card
                key={entitlement.id}
                className="hover:shadow-md transition-shadow"
              >
                <CardHeader>
                  <h3 className="font-semibold leading-none tracking-tight">
                    <Link
                      className="text-primary hover:underline hover:text-primary/80"
                      to={`/entitlements/${entitlement.id}`}
                    >
                      {entitlement.name}
                    </Link>
                  </h3>
                  <p className="text-sm text-muted-foreground mt-2 line-clamp-2">
                    {entitlement.descriptionPreview || "No description"}
                  </p>
                  <div className="flex flex-wrap gap-2 mt-3">
                    {sortedSupportLevels(
                      (entitlement.supportLevels ||
                        []) as Array<SupportLevelListItem>,
                    ).map((level) => (
                      <Badge
                        variant="secondary"
                        className="font-normal"
                        key={
                          typeof level === "string" ? level : String(level.id)
                        }
                      >
                        {supportLevelLabel(level)}
                      </Badge>
                    ))}
                    {(!entitlement.supportLevels ||
                      entitlement.supportLevels.length === 0) && (
                      <span className="text-sm text-muted-foreground">
                        No support levels
                      </span>
                    )}
                  </div>
                </CardHeader>
              </Card>
            ),
          )}
        </div>
      </DataState>
    </section>
  );
}
