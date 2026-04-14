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
import type { CategoryRecord, CollectionResponse } from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";

export default function CategoriesPage(props: SessionPageProps) {
  void props;
  const categoriesState =
    useJson<CollectionResponse<CategoryRecord>>("/api/categories");

  return (
    <section className="w-full mt-4">
      <PageHeader
        title="Categories"
        actions={
          categoriesState.data?.canCreate ? (
            <Button asChild>
              <SmartLink href={categoriesState.data.createPath}>
                Create
              </SmartLink>
            </Button>
          ) : null
        }
      />

      <DataState
        state={categoriesState}
        emptyMessage="No categories are available yet."
      >
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {categoriesState.data?.items.map((category: CategoryRecord) => (
            <Card
              key={category.id}
              className="hover:shadow-md transition-shadow"
            >
              <CardHeader>
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold leading-none tracking-tight">
                    <Link
                      className="text-primary hover:underline hover:text-primary/80"
                      to={`/categories/${category.id}`}
                    >
                      {category.name}
                    </Link>
                  </h3>
                  {category.isDefault && (
                    <Badge
                      variant="secondary"
                      className="whitespace-nowrap font-normal"
                    >
                      Default
                    </Badge>
                  )}
                </div>
                <p className="text-sm text-muted-foreground mt-2 line-clamp-2">
                  {category.descriptionPreview || "No description"}
                </p>
              </CardHeader>
            </Card>
          ))}
        </div>
      </DataState>
    </section>
  );
}
