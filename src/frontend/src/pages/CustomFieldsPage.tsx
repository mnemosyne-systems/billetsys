/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useState } from "react";
import { toast } from "sonner";
import useJson from "../hooks/useJson";
import useClientPagination from "../hooks/useClientPagination";
import useNumberShortcuts from "../hooks/useNumberShortcuts";
import DataState from "../components/common/DataState";
import PaginationControls from "../components/common/PaginationControls";
import SortDropdown from "../components/common/SortDropdown";
import PageHeader from "../components/layout/PageHeader";
import { SmartLink } from "../utils/routing";
import { postForm } from "../utils/api";
import { toQueryString } from "../utils/formatting";
import type { SessionPageProps } from "../types/app";
import type {
  CustomFieldListResponse,
  CustomFieldSummary,
} from "../types/domain";
import { Card, CardHeader } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "../components/ui/alert-dialog";

const SORT_OPTIONS = [{ key: "name", label: "Name" }];

const SORT_CONFIGS = [
  {
    key: "name",
    comparator: (a: CustomFieldSummary, b: CustomFieldSummary) =>
      (a.name || "").localeCompare(b.name || "", undefined, {
        sensitivity: "base",
      }),
  },
];

export default function CustomFieldsPage({ sessionState }: SessionPageProps) {
  const [refreshNonce, setRefreshNonce] = useState(0);
  const [deactivatingId, setDeactivatingId] = useState<string | null>(null);
  const customFieldsState = useJson<CustomFieldListResponse>(
    `/api/custom-fields${toQueryString({
      refresh: refreshNonce || undefined,
    })}`,
  );

  const defaultPageSize = sessionState.data?.defaultPageSize ?? undefined;

  const {
    pageItems,
    page,
    pageSize,
    totalItems,
    totalPages,
    sort,
    dir,
    setPage,
    setPageSize,
    setSort,
    pageSizeOptions,
  } = useClientPagination<CustomFieldSummary>({
    items: customFieldsState.data?.items || [],
    defaultPageSize,
    sortConfigs: SORT_CONFIGS,
    defaultSort: "name",
  });

  useNumberShortcuts({
    items: pageItems.slice(0, 10),
    getPath: (field) => field.editPath || `/custom-fields/${field.id}/edit`,
  });

  const deactivate = async (field: CustomFieldSummary) => {
    const key = String(field.id);
    setDeactivatingId(key);
    try {
      await postForm(
        field.deactivatePath || `/custom-fields/${field.id}/deactivate`,
        [],
        { headers: { "X-Billetsys-Client": "react" } },
      );
      toast.success("Custom field deactivated.");
      setRefreshNonce((current) => current + 1);
    } catch (error: unknown) {
      toast.error(
        error instanceof Error
          ? error.message
          : "Unable to deactivate custom field.",
      );
    } finally {
      setDeactivatingId(null);
    }
  };

  return (
    <section className="w-full mt-4">
      <PageHeader
        title="Custom fields"
        actions={
          customFieldsState.data?.canCreate ? (
            <Button asChild>
              <SmartLink href={customFieldsState.data.createPath}>
                Create
              </SmartLink>
            </Button>
          ) : null
        }
      />

      <DataState
        state={customFieldsState}
        emptyMessage="No custom fields are available yet."
      >
        <div className="flex items-center justify-between mb-4">
          <SortDropdown
            options={SORT_OPTIONS}
            currentSort={sort}
            currentDir={dir}
            onSort={setSort}
          />
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {pageItems.map((field: CustomFieldSummary) => (
            <Card key={field.id} className="hover:shadow-md transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold leading-none tracking-tight">
                    <SmartLink
                      className="text-[var(--color-header-bg)] hover:underline hover:opacity-80"
                      href={field.editPath || `/custom-fields/${field.id}/edit`}
                    >
                      {field.name}
                    </SmartLink>
                  </h3>
                  <Badge
                    variant="secondary"
                    className="whitespace-nowrap font-normal"
                  >
                    {field.fieldType}
                  </Badge>
                </div>

                <p className="text-sm text-muted-foreground mt-2 line-clamp-2">
                  {field.description || "No description"}
                </p>

                <div className="mt-3 flex flex-wrap gap-2">
                  {field.required && <Badge>Required</Badge>}
                  <Badge variant={field.active ? "default" : "secondary"}>
                    {field.active ? "Active" : "Inactive"}
                  </Badge>
                  {field.fieldType === "SELECT" && (
                    <Badge variant="outline">
                      {(field.options || []).length} option
                      {(field.options || []).length === 1 ? "" : "s"}
                    </Badge>
                  )}
                </div>

                <div className="mt-4 flex flex-wrap items-center justify-end gap-2">
                  <Button variant="outline" size="sm" asChild>
                    <SmartLink
                      href={field.editPath || `/custom-fields/${field.id}/edit`}
                    >
                      Edit
                    </SmartLink>
                  </Button>
                  {field.active && (
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <Button
                          type="button"
                          variant="destructive"
                          size="sm"
                          disabled={deactivatingId === String(field.id)}
                        >
                          {deactivatingId === String(field.id)
                            ? "Deactivating..."
                            : "Deactivate"}
                        </Button>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>
                            Deactivate custom field?
                          </AlertDialogTitle>
                          <AlertDialogDescription>
                            This field will disappear from create and edit
                            forms. Existing ticket values stay visible in
                            read-only mode.
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>Cancel</AlertDialogCancel>
                          <AlertDialogAction
                            onClick={() => deactivate(field)}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                          >
                            Deactivate
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  )}
                </div>
              </CardHeader>
            </Card>
          ))}
        </div>

        <PaginationControls
          page={page}
          pageSize={pageSize}
          totalItems={totalItems}
          totalPages={totalPages}
          pageSizeOptions={pageSizeOptions}
          onPageChange={setPage}
          onPageSizeChange={setPageSize}
        />
      </DataState>
    </section>
  );
}
