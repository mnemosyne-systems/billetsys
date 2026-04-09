/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import AttachmentPicker from "../components/common/AttachmentPicker";
import DataState from "../components/common/DataState";
import { toast } from "sonner";
import LexicalEditor from "../components/editor/LexicalEditor";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import { postMultipart } from "../utils/api";
import { toQueryString } from "../utils/formatting";
import { resolvePostRedirectPath } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  CompanyEntitlementOption,
  NamedEntity,
  SupportTicketCreateBootstrap,
  VersionInfo,
} from "../types/domain";
import type { SupportTicketCreateFormState } from "../types/forms";
import type { FormEntries } from "../utils/api";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";

interface SupportTicketCreatePageProps extends SessionPageProps {
  apiBase?: string;
  backPath?: string;
  submitFallbackPath?: string;
  title?: string;
  description?: string;
  navigateTo?: string;
  compactCreateActions?: boolean;
  hideEntitlementLevel?: boolean;
}

export default function SupportTicketCreatePage({
  sessionState,
  apiBase = "/api/support/tickets/bootstrap",
  submitFallbackPath = "/support/tickets",
  title = "",
  description = "",
  navigateTo = "/support/tickets",
  hideEntitlementLevel = false,
}: SupportTicketCreatePageProps) {
  const navigate = useNavigate();

  const [selectedCompanyId, setSelectedCompanyId] = useState("");
  const [selectedCompanyEntitlementId, setSelectedCompanyEntitlementId] =
    useState("");
  const bootstrapState = useJson<SupportTicketCreateBootstrap>(
    `${apiBase}${toQueryString({
      companyId: selectedCompanyId || undefined,
      companyEntitlementId: selectedCompanyEntitlementId || undefined,
    })}`,
  );
  const bootstrap = bootstrapState.data;
  const [formState, setFormState] =
    useState<SupportTicketCreateFormState | null>(null);
  const [files, setFiles] = useState<File[]>([]);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const messageInputRef = useRef<HTMLTextAreaElement | null>(null);
  const submissionGuard = useSubmissionGuard();
  const showFixedCompany = sessionState.data?.role === "superuser";
  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState((current) => {
      const initialCompanyId = bootstrap.selectedCompanyId
        ? String(bootstrap.selectedCompanyId)
        : "";
      const initialEntitlementId = bootstrap.selectedCompanyEntitlementId
        ? String(bootstrap.selectedCompanyEntitlementId)
        : "";
      const initialAffectsVersionId = bootstrap.defaultAffectsVersion?.id
        ? String(bootstrap.defaultAffectsVersion.id)
        : "";
      if (!current) {
        setSelectedCompanyId(initialCompanyId);
        setSelectedCompanyEntitlementId(initialEntitlementId);
        return {
          ticketName: bootstrap.ticketName || "",
          title: bootstrap.title || "",
          companyId: initialCompanyId,
          companyEntitlementId: initialEntitlementId,
          categoryId: bootstrap.defaultCategoryId
            ? String(bootstrap.defaultCategoryId)
            : "",
          affectsVersionId: initialAffectsVersionId,
          message: "",
        };
      }
      const validEntitlementIds = (bootstrap.companyEntitlements || []).map(
        (entry) => String(entry.id),
      );
      const validVersionIds = (bootstrap.versions || []).map((version) =>
        String(version.id),
      );
      const nextEntitlementId = validEntitlementIds.includes(
        current.companyEntitlementId,
      )
        ? current.companyEntitlementId
        : initialEntitlementId;
      setSelectedCompanyEntitlementId(nextEntitlementId);
      return {
        ...current,
        ticketName: bootstrap.ticketName || current.ticketName,
        title: current.title,
        companyId: initialCompanyId || current.companyId,
        companyEntitlementId: nextEntitlementId,
        categoryId:
          current.categoryId ||
          (bootstrap.defaultCategoryId
            ? String(bootstrap.defaultCategoryId)
            : ""),
        affectsVersionId: validVersionIds.includes(current.affectsVersionId)
          ? current.affectsVersionId
          : initialAffectsVersionId,
      };
    });
  }, [bootstrap]);

  const updateFormState = <K extends keyof SupportTicketCreateFormState>(
    field: K,
    value: SupportTicketCreateFormState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const entries: FormEntries = [
        ["status", "Open"],
        ["title", formState.title],
        ["message", formState.message],
        ["companyId", formState.companyId],
        ["companyEntitlementId", formState.companyEntitlementId],
        ["categoryId", formState.categoryId || null],
        ["affectsVersionId", formState.affectsVersionId || null],
        ...files.map((file): [string, File] => ["attachments", file]),
      ];
      const response = await postMultipart(
        bootstrap?.submitPath || submitFallbackPath,
        entries,
        {
          headers: { "X-Billetsys-Client": "react" },
        },
      );
      toast.success("Support ticket created successfully.");
      navigate(await resolvePostRedirectPath(response, navigateTo));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to create ticket.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to create ticket.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      {bootstrap?.ticketName || title || description ? (
        <div className="flex flex-col pb-6 px-1">
          <h2 className="text-3xl font-bold tracking-tight">
            {bootstrap?.ticketName || title}
          </h2>
          {description && (
            <p className="text-muted-foreground mt-2">{description}</p>
          )}
        </div>
      ) : null}

      <DataState
        state={bootstrapState}
        emptyMessage="A company is required before creating a support ticket."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {formState && bootstrap && (
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
                    <FieldLabel>
                      Company <span className="text-destructive">*</span>
                    </FieldLabel>
                    {showFixedCompany ? (
                      <Input
                        value={
                          (bootstrap.companies || []).find(
                            (company: NamedEntity) =>
                              String(company.id) === formState.companyId,
                          )?.name || ""
                        }
                        readOnly
                      />
                    ) : (
                      <Select
                        value={formState.companyId || undefined}
                        onValueChange={(value) => {
                          const nextCompanyId = value;
                          setSelectedCompanyId(nextCompanyId);
                          setSelectedCompanyEntitlementId("");
                          setFormState((current) =>
                            current
                              ? {
                                  ...current,
                                  companyId: nextCompanyId,
                                  companyEntitlementId: "",
                                  affectsVersionId: "",
                                }
                              : current,
                          );
                        }}
                        required
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
                    )}
                  </Field>
                  <Field>
                    <FieldLabel>
                      Entitlement <span className="text-destructive">*</span>
                    </FieldLabel>
                    <Select
                      value={formState.companyEntitlementId || undefined}
                      onValueChange={(value) => {
                        const nextEntitlementId = value;
                        setSelectedCompanyEntitlementId(nextEntitlementId);
                        setFormState((current) =>
                          current
                            ? {
                                ...current,
                                companyEntitlementId: nextEntitlementId,
                                affectsVersionId: "",
                              }
                            : current,
                        );
                      }}
                      required
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select entitlement" />
                      </SelectTrigger>
                      <SelectContent>
                        {(bootstrap.companyEntitlements || []).map(
                          (entry: CompanyEntitlementOption) => (
                            <SelectItem key={entry.id} value={String(entry.id)}>
                              {entry.name}
                              {!hideEntitlementLevel && entry.levelName
                                ? ` • ${entry.levelName}`
                                : ""}
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
                    <FieldLabel>Affects Version</FieldLabel>
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
                              {version.date ? ` (${version.date})` : ""}
                            </SelectItem>
                          ),
                        )}
                      </SelectContent>
                    </Select>
                  </Field>
                </div>

                <Field>
                  <FieldLabel>
                    Message <span className="text-destructive">*</span>
                  </FieldLabel>
                  <LexicalEditor
                    value={formState.message}
                    onChange={(value) => updateFormState("message", value)}
                    inputRef={messageInputRef}
                    rows={10}
                    required
                  />
                </Field>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Attachments</CardTitle>
              </CardHeader>
              <CardContent>
                <AttachmentPicker files={files} onFilesChange={setFiles} />
              </CardContent>
            </Card>

            <div className="flex items-center justify-end space-x-3 pt-4 border-t">
              <Button type="submit" disabled={saveState.saving}>
                {saveState.saving ? "Creating..." : "Create Request"}
              </Button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}
