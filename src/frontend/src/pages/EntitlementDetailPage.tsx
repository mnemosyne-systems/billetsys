/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useParams } from "react-router-dom";
import useJson from "../hooks/useJson";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import MarkdownContent from "../components/markdown/MarkdownContent";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  EntitlementRecord,
  LevelRecord,
  VersionInfo,
} from "../types/domain";
import { Card, CardContent, CardFooter } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";

export default function EntitlementDetailPage(props: SessionPageProps) {
  void props;
  const { id } = useParams();
  const entitlementState = useJson<EntitlementRecord>(
    id ? `/api/entitlements/${id}` : null,
  );
  const entitlement = entitlementState.data;
  const supportLevels = (entitlement?.supportLevels || []) as LevelRecord[];

  return (
    <section className="w-full mt-4">
      <DataState state={entitlementState} emptyMessage="Entitlement not found.">
        {entitlement && (
          <>
            <PageHeader title={entitlement.name || "Entitlement"} />
            <Card>
              <CardContent className="p-6 md:p-8 space-y-6">
                {/* Top row: Name + Description side by side */}
                <div className="grid gap-6 md:grid-cols-[1fr_2fr]">
                  <Field>
                    <FieldLabel>Name</FieldLabel>
                    <Input value={entitlement.name || "\u2014"} readOnly />
                  </Field>
                  <div className="bg-muted/30 p-4 rounded-lg border border-border">
                    <h3 className="font-semibold text-sm mb-2">Description</h3>
                    {entitlement.description ? (
                      <div className="prose prose-sm dark:prose-invert max-w-none">
                        <MarkdownContent>
                          {entitlement.description}
                        </MarkdownContent>
                      </div>
                    ) : (
                      <p className="text-sm text-muted-foreground">
                        No description.
                      </p>
                    )}
                  </div>
                </div>

                {/* Bottom row: Support levels + Versions side by side */}
                <div className="grid gap-6 md:grid-cols-2">
                  <div className="bg-muted/30 p-4 rounded-lg border border-border">
                    <h3 className="font-semibold text-sm mb-3">
                      Support levels
                    </h3>
                    <div>
                      {supportLevels.map((level) => (
                        <div
                          key={level.id}
                          className="flex items-center justify-between py-2 border-b last:border-0 border-border"
                        >
                          <span className="font-medium text-sm">
                            {level.name}
                          </span>
                          <span className="text-sm text-muted-foreground">
                            {level.fromLabel}{" "}
                            <span className="mx-1">&bull;</span> {level.toLabel}
                          </span>
                        </div>
                      ))}
                      {supportLevels.length === 0 && (
                        <p className="text-sm text-muted-foreground">
                          No support levels.
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="bg-muted/30 p-4 rounded-lg border border-border">
                    <h3 className="font-semibold text-sm mb-3">Versions</h3>
                    {(entitlement.versions || []).length > 0 ? (
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="border-b border-border text-muted-foreground">
                            <th className="text-left font-medium pb-2">
                              Version
                            </th>
                            <th className="text-left font-medium pb-2">Date</th>
                          </tr>
                        </thead>
                        <tbody>
                          {(entitlement.versions || []).map(
                            (version: VersionInfo) => (
                              <tr
                                key={
                                  version.id ||
                                  `${version.name}-${version.date}`
                                }
                                className="border-b last:border-0 border-border"
                              >
                                <td className="py-2 font-medium">
                                  {version.name || "\u2014"}
                                </td>
                                <td className="py-2 text-muted-foreground">
                                  {version.date || "\u2014"}
                                </td>
                              </tr>
                            ),
                          )}
                        </tbody>
                      </table>
                    ) : (
                      <p className="text-sm text-muted-foreground">
                        No versions.
                      </p>
                    )}
                  </div>
                </div>
              </CardContent>

              {entitlement.editPath && (
                <CardFooter className="flex justify-end pt-6 border-t bg-muted/20">
                  <Button asChild>
                    <SmartLink href={entitlement.editPath}>Edit</SmartLink>
                  </Button>
                </CardFooter>
              )}
            </Card>
          </>
        )}
      </DataState>
    </section>
  );
}
