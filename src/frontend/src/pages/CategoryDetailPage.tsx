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
import type { SessionPageProps } from "../types/app";
import type { CategoryRecord } from "../types/domain";
import { Card, CardContent, CardFooter } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { Badge } from "../components/ui/badge";
import { Textarea } from "../components/ui/textarea";

export default function CategoryDetailPage(props: SessionPageProps) {
  void props;
  const { id } = useParams();
  const categoryState = useJson<CategoryRecord>(
    id ? `/api/categories/${id}` : null,
  );
  const category = categoryState.data;

  return (
    <section className="w-full mt-4">
      <PageHeader
        title={
          <span className="flex items-center gap-3">
            <span>{category?.name || "Category"}</span>
            {category?.isDefault && (
              <Badge variant="secondary" className="font-normal">
                Default
              </Badge>
            )}
          </span>
        }
      />

      <DataState state={categoryState} emptyMessage="Category not found.">
        {category && (
          <Card>
            <CardContent className="pt-6">
              <div className="grid gap-6 md:grid-cols-2">
                <Field>
                  <FieldLabel>Name</FieldLabel>
                  <Input value={category.name || "—"} readOnly />
                </Field>
                <Field>
                  <FieldLabel>Default</FieldLabel>
                  <Input value={category.isDefault ? "Yes" : "No"} readOnly />
                </Field>
                <Field className="md:col-span-2">
                  <FieldLabel>Description</FieldLabel>
                  <Textarea
                    value={category.description || "—"}
                    readOnly
                    rows={10}
                    className="resize-none"
                  />
                </Field>
              </div>
            </CardContent>

            {category.editPath && (
              <CardFooter className="flex justify-end pt-6 border-t bg-muted/20">
                <Button asChild>
                  <SmartLink href={category.editPath}>Edit</SmartLink>
                </Button>
              </CardFooter>
            )}
          </Card>
        )}
      </DataState>
    </section>
  );
}
