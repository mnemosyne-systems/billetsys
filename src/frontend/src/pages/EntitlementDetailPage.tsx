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
import { Button } from "../components/ui/button";
import { Field } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { Table, TableBody, TableCell, TableRow } from "../components/ui/table";

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
            <div className="space-y-6">
              <div className="grid gap-6 md:grid-cols-2">
                <Field>
                  <div className="text-sm font-medium text-[var(--color-header-bg)]">
                    Name
                  </div>
                  <Input value={entitlement.name || "\u2014"} readOnly />
                </Field>
                <Field>
                  <div className="text-sm font-medium text-[var(--color-header-bg)]">
                    Description
                  </div>
                  <div className="min-h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm">
                    {entitlement.description ? (
                      <div className="prose prose-sm dark:prose-invert max-w-none">
                        <MarkdownContent>
                          {entitlement.description}
                        </MarkdownContent>
                      </div>
                    ) : (
                      <span className="text-muted-foreground">\u2014</span>
                    )}
                  </div>
                </Field>
              </div>

              <div className="grid gap-6 md:grid-cols-2">
                <div>
                  <div className="mb-3 text-sm font-medium text-[var(--color-header-bg)]">
                    Levels
                  </div>
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
                          {level.fromLabel} <span className="mx-1">&bull;</span>{" "}
                          {level.toLabel}
                        </span>
                      </div>
                    ))}
                    {supportLevels.length === 0 && (
                      <p className="text-sm text-muted-foreground">
                        No levels.
                      </p>
                    )}
                  </div>
                </div>

                <div>
                  <div className="mb-3 text-sm font-medium text-[var(--color-header-bg)]">
                    Versions
                  </div>
                  {(entitlement.versions || []).length > 0 ? (
                    <Table className="text-sm">
                      <TableBody>
                        {(entitlement.versions || []).map(
                          (version: VersionInfo) => (
                            <TableRow
                              key={
                                version.id || `${version.name}-${version.date}`
                              }
                            >
                              <TableCell className="font-medium">
                                {version.name || "\u2014"}
                              </TableCell>
                              <TableCell className="text-muted-foreground">
                                {version.date || "\u2014"}
                              </TableCell>
                            </TableRow>
                          ),
                        )}
                      </TableBody>
                    </Table>
                  ) : (
                    <p className="text-sm text-muted-foreground">
                      No versions.
                    </p>
                  )}
                </div>
              </div>

              {entitlement.editPath && (
                <div className="flex justify-end pt-6">
                  <Button asChild>
                    <SmartLink href={entitlement.editPath}>Edit</SmartLink>
                  </Button>
                </div>
              )}
            </div>
          </>
        )}
      </DataState>
    </section>
  );
}
