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
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, EntitlementRecord } from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";

type SupportLevel = NonNullable<EntitlementRecord["supportLevels"]>[number];

function supportLevelLabel(level: SupportLevel): string {
  return typeof level === "string" ? level : level.name || "Unnamed level";
}

function sortedSupportLevels(
  supportLevels: EntitlementRecord["supportLevels"],
): SupportLevel[] {
  return [...(supportLevels || [])].sort((left, right) =>
    supportLevelLabel(left).localeCompare(supportLevelLabel(right), undefined, {
      sensitivity: "base",
    }),
  );
}

export default function EntitlementsPage({ sessionState }: SessionPageProps) {
  const entitlementsState =
    useJson<CollectionResponse<EntitlementRecord>>("/api/entitlements");

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <div className="flex flex-row items-center justify-between pb-6 px-1">
        <h2 className="text-3xl font-bold tracking-tight">Entitlements</h2>
        <div>
          <Button asChild>
            <SmartLink href="/entitlements/new">Create</SmartLink>
          </Button>
        </div>
      </div>

      <DataState
        state={entitlementsState}
        emptyMessage="No entitlements are available yet."
        signInHref={sessionState.data?.homePath || "/login"}
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
                    {sortedSupportLevels(entitlement.supportLevels).map(
                      (level) => (
                        <Badge
                          variant="secondary"
                          className="font-normal"
                          key={
                            typeof level === "string" ? level : String(level.id)
                          }
                        >
                          {supportLevelLabel(level)}
                        </Badge>
                      ),
                    )}
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
