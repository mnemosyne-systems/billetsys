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
import useJson from "../hooks/useJson";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { LevelRecord } from "../types/domain";
import { Card, CardContent, CardFooter } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { Textarea } from "../components/ui/textarea";

export default function LevelDetailPage({ sessionState }: SessionPageProps) {
  const { id } = useParams();
  const levelState = useJson<LevelRecord>(id ? `/api/levels/${id}` : null);
  const level = levelState.data;

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <div className="flex items-center justify-between pb-6 px-1">
        <h2 className="text-3xl font-bold tracking-tight">
          {level?.name || "Level details"}
        </h2>
      </div>

      <DataState
        state={levelState}
        emptyMessage="Level not found."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {level && (
          <Card>
            <CardContent className="grid gap-6 md:grid-cols-2 pt-6">
              <Field>
                <FieldLabel>Name</FieldLabel>
                <Input value={level.name || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel>Business level</FieldLabel>
                <Input value={level.level ?? "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel>Color</FieldLabel>
                <div className="h-10 px-3 py-2 border rounded-md bg-muted/50 flex items-center">
                  <LevelColorFieldValue
                    color={level.color}
                    display={level.colorDisplay}
                  />
                </div>
              </Field>
              <Field>
                <FieldLabel>Country</FieldLabel>
                <Input value={level.countryName || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel>From</FieldLabel>
                <Input value={level.fromLabel || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel>To</FieldLabel>
                <Input value={level.toLabel || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel>Time zone</FieldLabel>
                <Input value={level.timezoneName || "—"} readOnly />
              </Field>
              <div className="hidden md:block" aria-hidden="true" />
              <Field className="md:col-span-2">
                <FieldLabel>Description</FieldLabel>
                <Textarea
                  value={level.description || "—"}
                  readOnly
                  rows={6}
                  className="resize-none"
                />
              </Field>
            </CardContent>

            {level.editPath && (
              <CardFooter className="flex justify-end pt-6 border-t bg-muted/20">
                <Button asChild>
                  <SmartLink href={level.editPath}>Edit</SmartLink>
                </Button>
              </CardFooter>
            )}
          </Card>
        )}
      </DataState>
    </section>
  );
}
