/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import DataState from "../components/common/DataState";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import { postForm } from "../utils/api";
import { toQueryString } from "../utils/formatting";
import { resolvePostRedirectPath, SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  CompanyEntitlementOption,
  NamedEntity,
  TicketWorkbenchBootstrap,
  VersionInfo,
} from "../types/domain";
import type { TicketWorkbenchFormState } from "../types/forms";
import { SUPPORT_TICKET_STATUSES } from "../types/tickets";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
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
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";

export default function TicketWorkbenchFormPage({
  sessionState,
}: SessionPageProps) {
  const navigate = useNavigate();

  const { id } = useParams();
  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const requestedCompanyId = query.get("companyId") || "";
  const [formState, setFormState] = useState<TicketWorkbenchFormState | null>(
    null,
  );
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const submissionGuard = useSubmissionGuard();
  const bootstrapState = useJson<TicketWorkbenchBootstrap>(
    `/api/ticket-workbench/bootstrap${toQueryString({ ticketId: id, companyId: formState?.companyId || requestedCompanyId })}`,
  );
  const bootstrap = bootstrapState.data;

  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState((current) => {
      if (
        !current ||
        String(current.id || "") !== String(bootstrap.ticket?.id || "")
      ) {
        return {
          id: bootstrap.ticket?.id ? String(bootstrap.ticket.id) : "",
          title: bootstrap.ticket?.title || "",
          status: bootstrap.ticket?.status || "Open",
          companyId: bootstrap.ticket?.companyId
            ? String(bootstrap.ticket.companyId)
            : "",
          companyEntitlementId: bootstrap.ticket?.companyEntitlementId
            ? String(bootstrap.ticket.companyEntitlementId)
            : "",
          categoryId: bootstrap.ticket?.categoryId
            ? String(bootstrap.ticket.categoryId)
            : "",
          externalIssueLink: bootstrap.ticket?.externalIssueLink || "",
          affectsVersionId: bootstrap.ticket?.affectsVersionId
            ? String(bootstrap.ticket.affectsVersionId)
            : "",
          resolvedVersionId: bootstrap.ticket?.resolvedVersionId
            ? String(bootstrap.ticket.resolvedVersionId)
            : "",
        };
      }
      const entitlements = bootstrap.entitlements || [];
      const versions = bootstrap.versions || [];
      return {
        ...current,
        companyEntitlementId: entitlements.some(
          (option) => String(option.id) === current.companyEntitlementId,
        )
          ? current.companyEntitlementId
          : entitlements[0]?.id
            ? String(entitlements[0].id)
            : "",
        affectsVersionId: versions.some(
          (option) => String(option.id) === current.affectsVersionId,
        )
          ? current.affectsVersionId
          : versions[0]?.id
            ? String(versions[0].id)
            : "",
        resolvedVersionId: versions.some(
          (option) => String(option.id) === current.resolvedVersionId,
        )
          ? current.resolvedVersionId
          : "",
      };
    });
  }, [bootstrap]);

  const updateFormState = <K extends keyof TicketWorkbenchFormState>(
    field: K,
    value: TicketWorkbenchFormState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !bootstrap || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(bootstrap.submitPath, [
        ["title", formState.title],
        ["status", formState.status],
        ["companyId", formState.companyId],
        ["companyEntitlementId", formState.companyEntitlementId],
        ["categoryId", formState.categoryId],
        ["externalIssueLink", formState.externalIssueLink],
        ["affectsVersionId", formState.affectsVersionId],
        ["resolvedVersionId", formState.resolvedVersionId],
      ]);
      toast.success(
        bootstrap.edit
          ? "Ticket updated successfully."
          : "Ticket created successfully.",
      );
      navigate(await resolvePostRedirectPath(response, "/tickets"));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to save ticket.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to save ticket.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const deleteTicket = async () => {
    if (!id || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(`/tickets/${id}/delete`, []);
      toast.success("Ticket deleted.");
      navigate(await resolvePostRedirectPath(response, "/tickets"));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to delete ticket.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to delete ticket.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <div className="flex items-center justify-between pb-6 px-1">
        <h2 className="text-3xl font-bold tracking-tight">
          {bootstrap?.title || "Ticket form"}
        </h2>
        {id && (
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                type="button"
                variant="destructive"
                disabled={saveState.saving}
              >
                Delete ticket
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                <AlertDialogDescription>
                  This action cannot be undone. This will permanently delete
                  this ticket.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction
                  onClick={deleteTicket}
                  className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                >
                  Delete
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        )}
      </div>

      <DataState
        state={bootstrapState}
        emptyMessage="Unable to load the ticket form."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {bootstrap && formState && (
          <form className="space-y-6 pb-20" onSubmit={submit}>
            <Card>
              <CardHeader>
                <CardTitle>Ticket Details</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-6">
                <Field>
                  <FieldLabel>
                    Title <span className="text-destructive">*</span>
                  </FieldLabel>
                  <Input
                    value={formState.title}
                    onChange={(event) =>
                      updateFormState("title", event.target.value)
                    }
                    required
                  />
                </Field>

                <div className="grid gap-6 md:grid-cols-2">
                  <Field>
                    <FieldLabel>Status</FieldLabel>
                    <Select
                      value={formState.status || undefined}
                      onValueChange={(value) =>
                        updateFormState("status", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select status" />
                      </SelectTrigger>
                      <SelectContent>
                        {SUPPORT_TICKET_STATUSES.filter(
                          (status) => status !== "Resolved",
                        ).map((status) => (
                          <SelectItem key={status} value={status}>
                            {status}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field>
                    <FieldLabel>Company</FieldLabel>
                    <Select
                      value={formState.companyId || undefined}
                      onValueChange={(value) =>
                        setFormState((current) =>
                          current
                            ? {
                                ...current,
                                companyId: value,
                                companyEntitlementId: "",
                                affectsVersionId: "",
                                resolvedVersionId: "",
                              }
                            : current,
                        )
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select company" />
                      </SelectTrigger>
                      <SelectContent>
                        {(bootstrap.companies || []).map(
                          (company: NamedEntity) => (
                            <SelectItem
                              key={company.id}
                              value={String(company.id)}
                            >
                              {company.name}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field>
                    <FieldLabel>Entitlement</FieldLabel>
                    <Select
                      value={formState.companyEntitlementId || undefined}
                      onValueChange={(value) =>
                        updateFormState("companyEntitlementId", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select entitlement" />
                      </SelectTrigger>
                      <SelectContent>
                        {(bootstrap.entitlements || []).map(
                          (entitlement: CompanyEntitlementOption) => (
                            <SelectItem
                              key={entitlement.id}
                              value={String(entitlement.id)}
                            >
                              {entitlement.name}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field>
                    <FieldLabel>Category</FieldLabel>
                    <Select
                      value={formState.categoryId || undefined}
                      onValueChange={(value) =>
                        updateFormState("categoryId", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select category" />
                      </SelectTrigger>
                      <SelectContent>
                        {(bootstrap.categories || []).map(
                          (category: NamedEntity) => (
                            <SelectItem
                              key={category.id}
                              value={String(category.id)}
                            >
                              {category.name}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field>
                    <FieldLabel>Affects version</FieldLabel>
                    <Select
                      value={formState.affectsVersionId || undefined}
                      onValueChange={(value) =>
                        updateFormState("affectsVersionId", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select version" />
                      </SelectTrigger>
                      <SelectContent>
                        {(bootstrap.versions || []).map(
                          (version: VersionInfo) => (
                            <SelectItem
                              key={version.id}
                              value={String(version.id)}
                            >
                              {version.name}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field>
                    <FieldLabel>Resolved version</FieldLabel>
                    <Select
                      value={formState.resolvedVersionId || undefined}
                      onValueChange={(value) =>
                        updateFormState("resolvedVersionId", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select version" />
                      </SelectTrigger>
                      <SelectContent>
                        {(bootstrap.versions || []).map(
                          (version: VersionInfo) => (
                            <SelectItem
                              key={version.id}
                              value={String(version.id)}
                            >
                              {version.name}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  </Field>
                </div>

                <Field>
                  <FieldLabel>External issue</FieldLabel>
                  <Input
                    value={formState.externalIssueLink}
                    onChange={(event) =>
                      updateFormState("externalIssueLink", event.target.value)
                    }
                    placeholder="https://github.com/..."
                  />
                </Field>
              </CardContent>
            </Card>

            {bootstrap.edit && (
              <Card>
                <CardHeader>
                  <CardTitle>Messages</CardTitle>
                </CardHeader>
                <CardContent>
                  <ul className="space-y-3">
                    {(bootstrap.messages || []).map((message) => (
                      <li key={message.id} className="text-sm">
                        <strong className="font-medium text-foreground">
                          {message.dateLabel || "-"}
                        </strong>{" "}
                        <span className="text-muted-foreground">
                          - {message.body || "No message body"}
                        </span>
                      </li>
                    ))}
                    {(!bootstrap.messages ||
                      bootstrap.messages.length === 0) && (
                      <li className="text-sm text-muted-foreground italic">
                        No messages yet.
                      </li>
                    )}
                  </ul>
                </CardContent>
              </Card>
            )}

            <div className="flex items-center justify-end space-x-3 pt-4 border-t">
              <SmartLink
                className="inline-flex h-10 items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground"
                href="/tickets"
              >
                Cancel
              </SmartLink>
              <Button type="submit" disabled={saveState.saving}>
                {saveState.saving
                  ? "Saving..."
                  : bootstrap.edit
                    ? "Save Ticket"
                    : "Create Ticket"}
              </Button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}
