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
import { SmartLink } from "../utils/routing";
import { SelectableUserSummary } from "../components/users/UserComponents";
import {
  sortUsersByName,
  sortEntitlementAssignments,
} from "../utils/formatting";
import type { SessionPageProps } from "../types/app";
import type { CompanyAssignment, CompanyRecord } from "../types/domain";
import { Card, CardContent, CardFooter } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";

export default function CompanyDetailPage(props: SessionPageProps) {
  void props;
  const { id } = useParams();
  const companyState = useJson<CompanyRecord>(
    id ? `/api/companies/${id}` : null,
  );
  const company = companyState.data;
  const sortedEntitlements = sortEntitlementAssignments(
    company?.entitlementAssignments || [],
  );
  const sortedSuperusers = sortUsersByName(company?.selectedSuperusers || []);
  const sortedUsers = sortUsersByName(company?.selectedUsers || []);
  const sortedTams = sortUsersByName(company?.selectedTams || []);

  return (
    <section className="w-full mt-4">
      <DataState state={companyState} emptyMessage="Company not found.">
        {company && (
          <>
            <PageHeader title={company.name || "Company"} />
            <Card>
              <CardContent className="pt-6">
                <div className="grid gap-6 md:grid-cols-2">
                  <Field>
                    <FieldLabel>Name</FieldLabel>
                    <Input value={company.name || "—"} readOnly />
                  </Field>
                  <Field>
                    <FieldLabel>Phone</FieldLabel>
                    <Input value={company.phoneNumber || "—"} readOnly />
                  </Field>
                  <Field>
                    <FieldLabel>Country</FieldLabel>
                    <Input value={company.countryName || "—"} readOnly />
                  </Field>
                  <Field>
                    <FieldLabel>Time zone</FieldLabel>
                    <Input value={company.timezoneName || "—"} readOnly />
                  </Field>
                  <Field>
                    <FieldLabel>Address1</FieldLabel>
                    <Input value={company.address1 || "—"} readOnly />
                  </Field>
                  <Field>
                    <FieldLabel>Address2</FieldLabel>
                    <Input value={company.address2 || "—"} readOnly />
                  </Field>
                  <Field>
                    <FieldLabel>City</FieldLabel>
                    <Input value={company.city || "—"} readOnly />
                  </Field>
                  <Field>
                    <FieldLabel>State</FieldLabel>
                    <Input value={company.state || "—"} readOnly />
                  </Field>
                  <Field>
                    <FieldLabel>Zip</FieldLabel>
                    <Input value={company.zip || "—"} readOnly />
                  </Field>

                  <div className="md:col-span-2 bg-muted/30 p-5 rounded-lg border border-border">
                    <h3 className="font-semibold text-sm mb-3">Entitlements</h3>
                    <div>
                      {sortedEntitlements.length === 0 ? (
                        <p className="text-sm text-muted-foreground">—</p>
                      ) : (
                        <ul className="space-y-1 list-disc list-inside text-sm">
                          {sortedEntitlements.map(
                            (entry: CompanyAssignment, index) => (
                              <li
                                key={`${entry.entitlementId}-${entry.levelId}-${index}`}
                              >
                                {entry.entitlementName}{" "}
                                <span className="text-muted-foreground mx-1">
                                  •
                                </span>{" "}
                                {entry.levelName}
                              </li>
                            ),
                          )}
                        </ul>
                      )}
                    </div>
                  </div>

                  <div className="bg-muted/30 p-5 rounded-lg border border-border">
                    <h3 className="font-semibold text-sm mb-3">Superusers</h3>
                    <div className="text-sm">
                      <SelectableUserSummary users={sortedSuperusers} />
                    </div>
                  </div>

                  <div className="bg-muted/30 p-5 rounded-lg border border-border">
                    <h3 className="font-semibold text-sm mb-3">Users</h3>
                    <div className="text-sm">
                      <SelectableUserSummary users={sortedUsers} />
                    </div>
                  </div>

                  <div className="bg-muted/30 p-5 rounded-lg border border-border">
                    <h3 className="font-semibold text-sm mb-3">TAMs</h3>
                    <div className="text-sm">
                      <SelectableUserSummary users={sortedTams} />
                    </div>
                  </div>
                </div>
              </CardContent>

              {company.id && (
                <CardFooter className="flex justify-end pt-6 border-t bg-muted/20">
                  <Button asChild>
                    <SmartLink href={`/companies/${company.id}/edit`}>
                      Edit
                    </SmartLink>
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
