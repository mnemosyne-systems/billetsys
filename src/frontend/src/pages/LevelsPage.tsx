/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Link } from "react-router-dom";
import DataState from "../components/common/DataState";
import { LevelColorBadge } from "../components/common/LevelColorBadge";
import useJson from "../hooks/useJson";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { CollectionResponse, LevelRecord } from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";

export default function LevelsPage({ sessionState }: SessionPageProps) {
  const levelsState = useJson<CollectionResponse<LevelRecord>>("/api/levels");

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <div className="flex flex-row items-center justify-between pb-6 px-1">
        <h2 className="text-3xl font-bold tracking-tight">Support levels</h2>
        <div>
          <Button asChild>
            <SmartLink href="/levels/new">Create</SmartLink>
          </Button>
        </div>
      </div>

      <DataState
        state={levelsState}
        emptyMessage="No support levels are available yet."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {levelsState.data?.items.map((level: LevelRecord) => (
            <Card key={level.id} className="hover:shadow-md transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold leading-none tracking-tight">
                    <Link
                      className="text-primary hover:underline hover:text-primary/80"
                      to={`/levels/${level.id}`}
                    >
                      {level.name}
                    </Link>
                  </h3>
                  <LevelColorBadge
                    color={level.color}
                    display={level.colorDisplay}
                  />
                </div>
                <p className="text-sm text-muted-foreground mt-3 line-clamp-2">
                  {level.descriptionPreview || "No description"}
                </p>
                <p className="text-sm text-muted-foreground mt-2">
                  Level {level.level} • {level.fromLabel} - {level.toLabel}
                </p>
                <p className="text-sm text-muted-foreground mt-1">
                  {level.countryName || "No country"} •{" "}
                  {level.timezoneName || "No time zone"}
                </p>
              </CardHeader>
            </Card>
          ))}
        </div>
      </DataState>
    </section>
  );
}
