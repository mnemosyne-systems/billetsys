/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useParams } from "react-router-dom";
import DataState from "../components/common/DataState";
import { LevelColorFieldValue } from "../components/common/LevelColorBadge";
import PageHeader from "../components/layout/PageHeader";
import useJson from "../hooks/useJson";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { LevelRecord } from "../types/domain";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { Textarea } from "../components/ui/textarea";

export default function LevelDetailPage(props: SessionPageProps) {
  void props;
  const { id } = useParams();
  const levelState = useJson<LevelRecord>(id ? `/api/levels/${id}` : null);
  const level = levelState.data;

  return (
    <section className="w-full mt-4">
      <PageHeader title={level?.name || "Support level"} />

      <DataState state={levelState} emptyMessage="Level not found.">
        {level && (
          <div className="space-y-6">
            <div className="grid gap-6 md:grid-cols-2">
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Name
                </FieldLabel>
                <Input value={level.name || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Level
                </FieldLabel>
                <Input value={level.level ?? "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Color
                </FieldLabel>
                <div className="h-10 px-3 py-2 border rounded-md bg-muted/50 flex items-center">
                  <LevelColorFieldValue
                    color={level.color}
                    display={level.colorDisplay}
                  />
                </div>
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Country
                </FieldLabel>
                <Input value={level.countryName || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  From
                </FieldLabel>
                <Input value={level.fromLabel || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  To
                </FieldLabel>
                <Input value={level.toLabel || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Time zone
                </FieldLabel>
                <Input value={level.timezoneName || "—"} readOnly />
              </Field>
              <div className="hidden md:block" aria-hidden="true" />
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Description
                </FieldLabel>
                <Textarea
                  value={level.description || "—"}
                  readOnly
                  rows={6}
                  className="resize-none"
                />
              </Field>
            </div>

            {level.editPath && (
              <div className="flex justify-end pt-6">
                <Button asChild>
                  <SmartLink href={level.editPath}>Edit</SmartLink>
                </Button>
              </div>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}
