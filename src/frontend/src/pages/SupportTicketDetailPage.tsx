/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ChangeEvent, FormEvent } from "react";
import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import MarkdownContent from "../components/markdown/MarkdownContent";
import LexicalEditor from "../components/editor/LexicalEditor";
import {
  UserHoverLink,
  UserReferenceInlineList,
} from "../components/users/UserComponents";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import { postForm, postMultipart } from "../utils/api";
import {
  formatFileSize,
  toQueryString,
  versionLabel,
} from "../utils/formatting";
import { resolvePostRedirectPath } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  AttachmentDetail,
  AttachmentReference,
  CrossReferenceEntry,
  CrossReferencesResponse,
  MessageReference,
  NamedEntity,
  SupportTicketDetailRecord,
  VersionInfo,
} from "../types/domain";
import type { SupportTicketDetailState } from "../types/forms";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";

interface SupportTicketDetailPageProps extends SessionPageProps {
  apiBase?: string;
  backPath?: string;
  titleFallback?: string;
  secondaryUsersLabel?: string;
  enableAttachmentPreviews?: boolean;
}

function attachmentPreviewKind(
  attachment: AttachmentReference,
): "image" | "text" | "markdown" | "pdf" | null {
  const mimeType = (attachment.mimeType || "").toLowerCase();
  const name = (attachment.name || "").toLowerCase();

  if (mimeType.startsWith("image/")) {
    return "image";
  }
  if (mimeType === "application/pdf" || name.endsWith(".pdf")) {
    return "pdf";
  }
  if (
    mimeType === "text/markdown" ||
    mimeType === "text/x-markdown" ||
    name.endsWith(".md") ||
    name.endsWith(".markdown") ||
    name.endsWith(".mdown") ||
    name.endsWith(".mkdn")
  ) {
    return "markdown";
  }
  if (
    mimeType === "text/plain" ||
    mimeType.startsWith("text/plain;") ||
    name.endsWith(".txt") ||
    name.endsWith(".log")
  ) {
    return "text";
  }
  return null;
}

function attachmentPreviewText(detail: AttachmentDetail | null): string {
  if (!detail) {
    return "";
  }
  return (
    (detail.lines || []).map((line) => line.content).join("\n") ||
    detail.messageBody ||
    ""
  );
}

function AttachmentSummary({
  attachment,
}: {
  attachment: AttachmentReference;
}) {
  return (
    <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
      <span className="min-w-0 text-sm font-semibold text-[var(--color-header-bg)]">
        <a
          className="break-all hover:underline"
          href={attachment.downloadPath}
          target="_blank"
          rel="noreferrer"
        >
          {attachment.name}
        </a>
      </span>
      <span className="shrink-0 text-xs text-muted-foreground">
        {attachment.mimeType || "application/octet-stream"} -{" "}
        {attachment.sizeLabel}
      </span>
    </div>
  );
}

function AttachmentPreview({
  attachment,
}: {
  attachment: AttachmentReference;
}) {
  const [previewRequested, setPreviewRequested] = useState(false);
  const previewKind = attachmentPreviewKind(attachment);
  const previewState = useJson<AttachmentDetail>(
    previewRequested && previewKind && attachment.id
      ? `/api/attachments/${attachment.id}`
      : null,
  );
  const preview = previewState.data;

  if (!previewKind) {
    return <AttachmentSummary attachment={attachment} />;
  }

  return (
    <div
      className="group relative"
      onMouseEnter={() => setPreviewRequested(true)}
      onFocusCapture={() => setPreviewRequested(true)}
    >
      <AttachmentSummary attachment={attachment} />

      <div className="pointer-events-auto absolute left-10 top-full z-30 hidden w-80 max-w-[calc(100vw-4rem)] pt-2 group-hover:block group-focus-within:block md:w-96">
        {previewState.loading ? (
          <div className="rounded-md border bg-background p-4 text-sm text-muted-foreground shadow-lg">
            Loading preview...
          </div>
        ) : previewState.error ||
          previewState.unauthorized ||
          previewState.forbidden ||
          !preview ? (
          <div className="rounded-md border bg-background p-4 text-sm text-muted-foreground shadow-lg">
            Preview unavailable.
          </div>
        ) : previewKind === "image" && preview.downloadPath ? (
          <div className="overflow-hidden rounded-md border bg-background shadow-lg">
            <img
              src={preview.downloadPath}
              alt={attachment.name}
              className="max-h-80 w-full object-contain"
            />
          </div>
        ) : previewKind === "pdf" && preview.downloadPath ? (
          <iframe
            className="h-96 w-full rounded-md border bg-background shadow-lg"
            src={preview.downloadPath}
            title={`Preview of ${attachment.name || "attachment"}`}
          />
        ) : previewKind === "markdown" ? (
          <div className="max-h-96 overflow-auto rounded-md border bg-background p-4 shadow-lg">
            <MarkdownContent>{attachmentPreviewText(preview)}</MarkdownContent>
          </div>
        ) : (
          <pre className="max-h-96 overflow-auto rounded-md border bg-background p-4 text-xs shadow-lg">
            {attachmentPreviewText(preview)}
          </pre>
        )}
      </div>
    </div>
  );
}

function TicketMessageCard({
  message,
  enableAttachmentPreviews,
  crossReferences,
}: {
  message: MessageReference;
  enableAttachmentPreviews: boolean;
  crossReferences?: CrossReferenceEntry[];
}) {
  const author = message.author;
  const authorLabel =
    author?.displayName || author?.username || author?.name || "Unknown";

  return (
    <article className="overflow-visible rounded-md border border-border/80 bg-card shadow-sm">
      <div className="flex items-center justify-between gap-4 bg-[var(--color-buttons-bg)] px-4 py-3 text-sm font-semibold text-[var(--color-buttons-text)]">
        <span>{message.dateLabel || "-"}</span>
        <span className="text-right">
          {author?.detailPath ? (
            <UserHoverLink
              user={author}
              className="text-[var(--color-buttons-text)] hover:text-[var(--color-buttons-text)] hover:underline"
            >
              {authorLabel}
            </UserHoverLink>
          ) : (
            authorLabel
          )}
        </span>
      </div>

      <div className="space-y-4 px-4 py-4">
        <div className="prose prose-sm max-w-none text-foreground dark:prose-invert [&>p:first-child]:mt-0 [&>p:last-child]:mb-0">
          <MarkdownContent crossReferences={crossReferences}>
            {message.body || ""}
          </MarkdownContent>
        </div>
      </div>

      {(message.attachments || []).length > 0 && (
        <div className="border-t border-border/70 bg-[var(--color-surface-accent)]/60">
          {(message.attachments || []).map(
            (attachment: AttachmentReference) => (
              <div
                key={attachment.id}
                className="border-b border-border/50 px-4 py-3 last:border-b-0"
              >
                {enableAttachmentPreviews ? (
                  <AttachmentPreview attachment={attachment} />
                ) : (
                  <AttachmentSummary attachment={attachment} />
                )}
              </div>
            ),
          )}
        </div>
      )}
    </article>
  );
}

export default function SupportTicketDetailPage({
  sessionState,
  apiBase = "/api/support/tickets",
  titleFallback = "Support ticket",
  secondaryUsersLabel = "TAM",
  enableAttachmentPreviews = false,
}: SupportTicketDetailPageProps) {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const [refreshNonce, setRefreshNonce] = useState(0);
  const ticketState = useJson<SupportTicketDetailRecord>(
    id ? `${apiBase}/${id}${toQueryString({ refresh: refreshNonce })}` : null,
  );
  const ticket = ticketState.data;
  const refsState = useJson<CrossReferencesResponse>(
    id
      ? `${apiBase}/${id}/references${toQueryString({ refresh: refreshNonce })}`
      : null,
  );
  const [formState, setFormState] = useState<SupportTicketDetailState | null>(
    null,
  );
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const submissionGuard = useSubmissionGuard();
  const [replyState, setReplyState] = useState({ saving: false, error: "" });
  const [replyBody, setReplyBody] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const replyInputRef = useRef<HTMLTextAreaElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const messagesHeadingRef = useRef<HTMLHeadingElement | null>(null);
  const [scrollToMessages, setScrollToMessages] = useState(false);
  const isClosed = ticket?.displayStatus === "Closed";
  const canEditStatus = ticket?.editableStatus ?? true;
  const canEditCategory = ticket?.editableCategory ?? true;
  const canEditExternalIssue = ticket?.editableExternalIssue ?? true;
  const canEditAffectsVersion = ticket?.editableAffectsVersion ?? true;
  const canEditResolvedVersion = ticket?.editableResolvedVersion ?? true;
  const showLevelField =
    apiBase !== "/api/user/tickets" || sessionState.data?.role === "tam";
  const ticketHeading = ticket?.name || ticket?.title || titleFallback;
  const showSummaryField =
    Boolean(ticket?.title || formState?.title) &&
    (ticket?.title || formState?.title) !== ticket?.name;
  const crossReferences = [
    ...(refsState.data?.references ?? []),
    ...(refsState.data?.referencedBy ?? []),
  ];
  const relatedTickets = Array.from(
    new Map(
      crossReferences.map((reference) => [reference.ticketId, reference]),
    ).values(),
  ).sort((left, right) => left.ticketName.localeCompare(right.ticketName));

  useEffect(() => {
    if (!ticket) {
      return;
    }
    setFormState({
      title: ticket.title || "",
      status: ticket.displayStatus || "Open",
      categoryId: ticket.categoryId ? String(ticket.categoryId) : "none",
      externalIssueLink: ticket.externalIssueLink || "",
      affectsVersionId: ticket.affectsVersionId
        ? String(ticket.affectsVersionId)
        : "none",
      resolvedVersionId: ticket.resolvedVersionId
        ? String(ticket.resolvedVersionId)
        : "none",
    });
  }, [ticket]);

  useEffect(() => {
    if (!ticket) {
      return;
    }
    const shouldScrollFromQuery = new URLSearchParams(location.search).has(
      "replyAdded",
    );
    if (!scrollToMessages && !shouldScrollFromQuery) {
      return;
    }
    messagesHeadingRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
    setScrollToMessages(false);
    if (shouldScrollFromQuery) {
      window.history.replaceState(
        {},
        "",
        ticket.actionPath || location.pathname,
      );
    }
  }, [ticket, scrollToMessages, location.search, location.pathname]);

  const updateFormState = <K extends keyof SupportTicketDetailState>(
    field: K,
    value: SupportTicketDetailState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  const saveTicket = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!ticket || !formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(
        ticket.actionPath || `${apiBase}/${id || ""}`,
        [
          ["title", formState.title],
          ["status", formState.status],
          ["companyId", ticket.companyId],
          ["companyEntitlementId", ticket.companyEntitlementId],
          [
            "categoryId",
            formState.categoryId && formState.categoryId !== "none"
              ? formState.categoryId
              : null,
          ],
          ["externalIssueLink", formState.externalIssueLink],
          [
            "affectsVersionId",
            formState.affectsVersionId === "none"
              ? ""
              : formState.affectsVersionId,
          ],
          [
            "resolvedVersionId",
            formState.resolvedVersionId &&
            formState.resolvedVersionId !== "none"
              ? formState.resolvedVersionId
              : null,
          ],
        ],
        {
          headers: { "X-Billetsys-Client": "react" },
        },
      );
      const redirectPath = await resolvePostRedirectPath(
        response,
        ticket.actionPath || `${apiBase}/${id || ""}`,
      );
      if (redirectPath !== location.pathname) {
        toast.success("Ticket updated successfully.");
        navigate(redirectPath);
      } else {
        setRefreshNonce((current) => current + 1);
        toast.success("Ticket updated successfully.");
      }
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

  const addReplyFiles = (event: ChangeEvent<HTMLInputElement>) => {
    const nextFiles = Array.from(event.target.files || []);
    if (nextFiles.length === 0) {
      return;
    }
    setFiles((current) => [...current, ...nextFiles]);
    event.target.value = "";
  };

  const removeReplyFile = (index: number) => {
    setFiles((current) =>
      current.filter((_, fileIndex) => fileIndex !== index),
    );
  };

  const submitReply = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!ticket || !ticket.messageActionPath || !submissionGuard.tryEnter()) {
      return;
    }

    try {
      setReplyState({ saving: true, error: "" });

      const response = await postMultipart(
        ticket.messageActionPath,
        [
          ["body", replyBody],
          ...files.map((file): [string, File] => ["attachments", file]),
        ],
        {
          headers: { "X-Billetsys-Client": "react" },
        },
      );

      const redirectPath = await resolvePostRedirectPath(
        response,
        location.pathname,
      );

      if (redirectPath !== location.pathname) {
        toast.success("Reply added successfully.");
        navigate(redirectPath);
      } else {
        setRefreshNonce((current) => current + 1);
        setReplyBody("");
        setFiles([]);
        if (fileInputRef.current) {
          fileInputRef.current.value = "";
        }
        setScrollToMessages(true);
        toast.success("Reply added successfully.");
      }
    } catch (error: unknown) {
      setReplyState({
        saving: false,
        error: error instanceof Error ? error.message : "Unable to add reply.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to add reply.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }

    setReplyState({ saving: false, error: "" });
  };

  return (
    <section className="w-full px-4 md:px-8 xl:px-12 mx-auto mt-4 mb-16">
      <DataState state={ticketState} emptyMessage="Ticket not found.">
        {ticket && formState && (
          <div className="space-y-8">
            <PageHeader
              title={ticketHeading}
              subtitle={
                showSummaryField ? ticket.title || formState.title : undefined
              }
            />
            <form onSubmit={saveTicket} className="space-y-8">
              <div className="grid gap-5 md:grid-cols-2">
                <Field>
                  <FieldLabel>Title</FieldLabel>
                  <Input value={ticket.name || ticketHeading} readOnly />
                </Field>
                <Field>
                  <FieldLabel>Company</FieldLabel>
                  {ticket.companyId ? (
                    <div className="flex h-10 items-center rounded-md border bg-muted/50 px-3">
                      <a
                        className="text-sm font-medium text-primary hover:underline"
                        href={`/support/companies/${ticket.companyId}`}
                      >
                        {ticket.companyName || "-"}
                      </a>
                    </div>
                  ) : (
                    <Input value={ticket.companyName || "-"} readOnly />
                  )}
                </Field>
                {showSummaryField && (
                  <Field className="md:col-span-2">
                    <FieldLabel>Summary</FieldLabel>
                    {isClosed ? (
                      <Input
                        value={formState.title || ticket.title || ""}
                        readOnly
                      />
                    ) : (
                      <Input
                        value={formState.title}
                        onChange={(event) =>
                          updateFormState("title", event.target.value)
                        }
                        required
                      />
                    )}
                  </Field>
                )}
                <Field>
                  <FieldLabel>Category</FieldLabel>
                  {isClosed || !canEditCategory ? (
                    <Input value={ticket.categoryName || "-"} readOnly />
                  ) : (
                    <Select
                      value={formState.categoryId}
                      onValueChange={(value) =>
                        updateFormState("categoryId", value)
                      }
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select a category" />
                      </SelectTrigger>
                      <SelectContent>
                        {(ticket.categories || []).map(
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
                  )}
                </Field>
                <Field>
                  <FieldLabel>Entitlement</FieldLabel>
                  <Input
                    value={ticket.entitlementName || "-"}
                    readOnly
                    className={
                      ticket.ticketEntitlementExpired
                        ? "text-destructive border-destructive"
                        : ""
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel>Status</FieldLabel>
                  {isClosed || !canEditStatus ? (
                    <Input value={formState.status || "-"} readOnly />
                  ) : (
                    <Select
                      value={formState.status}
                      onValueChange={(value) =>
                        updateFormState("status", value)
                      }
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select status" />
                      </SelectTrigger>
                      <SelectContent>
                        {(ticket.statusOptions || []).map((option) => (
                          <SelectItem key={option} value={option}>
                            {option}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                </Field>
                {showLevelField && (
                  <Field>
                    <FieldLabel>Level</FieldLabel>
                    <Input
                      value={ticket.levelName || "-"}
                      readOnly
                      className={
                        ticket.ticketEntitlementExpired
                          ? "text-destructive border-destructive"
                          : ""
                      }
                    />
                  </Field>
                )}
                <Field>
                  <FieldLabel>External issue</FieldLabel>
                  {isClosed || !canEditExternalIssue ? (
                    formState.externalIssueLink ? (
                      <div className="h-10 px-3 py-2 border rounded-md bg-muted/50 truncate">
                        <a
                          href={formState.externalIssueLink}
                          target="_blank"
                          rel="noreferrer"
                          className="text-sm font-medium text-primary hover:underline truncate"
                        >
                          {formState.externalIssueLink}
                        </a>
                      </div>
                    ) : (
                      <Input value="-" readOnly />
                    )
                  ) : (
                    <div className="relative flex items-center">
                      <Input
                        value={formState.externalIssueLink}
                        onChange={(event) =>
                          updateFormState(
                            "externalIssueLink",
                            event.target.value,
                          )
                        }
                        className="pr-14"
                      />
                      {formState.externalIssueLink ? (
                        <a
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-sm font-medium text-primary hover:underline bg-background pl-2"
                          href={formState.externalIssueLink}
                          target="_blank"
                          rel="noreferrer"
                        >
                          Open
                        </a>
                      ) : null}
                    </div>
                  )}
                </Field>
                {showLevelField ? (
                  <div className="hidden md:block" aria-hidden="true" />
                ) : null}
                <Field>
                  <FieldLabel>Affects</FieldLabel>
                  {isClosed || !canEditAffectsVersion ? (
                    <Input
                      value={
                        versionLabel(
                          ticket.versions,
                          formState.affectsVersionId,
                        ) || "-"
                      }
                      readOnly
                    />
                  ) : (
                    <Select
                      value={formState.affectsVersionId}
                      onValueChange={(value) =>
                        updateFormState("affectsVersionId", value)
                      }
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Version" />
                      </SelectTrigger>
                      <SelectContent position="popper">
                        <SelectItem value="none">-</SelectItem>
                        {(ticket.versions || []).map((version: VersionInfo) => (
                          <SelectItem
                            key={version.id}
                            value={String(version.id)}
                          >
                            {version.name} ({version.date})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                </Field>
                <Field>
                  <FieldLabel>Resolved</FieldLabel>
                  {isClosed || !canEditResolvedVersion ? (
                    <Input
                      value={
                        versionLabel(
                          ticket.versions,
                          formState.resolvedVersionId,
                        ) || "-"
                      }
                      readOnly
                    />
                  ) : (
                    <Select
                      value={formState.resolvedVersionId}
                      onValueChange={(value) =>
                        updateFormState("resolvedVersionId", value)
                      }
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Version" />
                      </SelectTrigger>
                      <SelectContent position="popper">
                        <SelectItem value="none">-</SelectItem>
                        {(ticket.versions || []).map((version: VersionInfo) => (
                          <SelectItem
                            key={version.id}
                            value={String(version.id)}
                          >
                            {version.name} ({version.date})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                </Field>
                {(() => {
                  return (
                    <>
                      {relatedTickets.length > 0 ? (
                        <Field>
                          <FieldLabel>Related</FieldLabel>
                          <div className="border rounded-md overflow-hidden">
                            <Table>
                              <TableBody>
                                {relatedTickets.map((ref) => (
                                  <TableRow key={ref.ticketId}>
                                    <TableCell className="text-sm border-r">
                                      <a
                                        href={ref.detailPath}
                                        className="text-primary underline"
                                        target="_blank"
                                        rel="noreferrer"
                                      >
                                        #{ref.ticketName}
                                      </a>
                                    </TableCell>
                                    <TableCell className="text-sm text-muted-foreground truncate max-w-0 w-full">
                                      {ref.ticketTitle}
                                    </TableCell>
                                  </TableRow>
                                ))}
                              </TableBody>
                            </Table>
                          </div>
                        </Field>
                      ) : (
                        <Field>
                          <FieldLabel>Related</FieldLabel>
                          <div className="text-sm text-muted-foreground">-</div>
                        </Field>
                      )}
                      <div className="hidden md:block" aria-hidden="true" />
                    </>
                  );
                })()}
                <Field>
                  <FieldLabel>Support</FieldLabel>
                  <UserReferenceInlineList users={ticket.supportUsers} />
                </Field>
                <Field>
                  <FieldLabel>
                    {ticket.secondaryUsersLabel || secondaryUsersLabel}
                  </FieldLabel>
                  <UserReferenceInlineList
                    users={ticket.secondaryUsers || ticket.tamUsers}
                  />
                </Field>
              </div>

              {!isClosed &&
                (canEditStatus ||
                  canEditCategory ||
                  canEditExternalIssue ||
                  canEditAffectsVersion ||
                  canEditResolvedVersion) && (
                  <div className="flex justify-end pt-6">
                    <Button type="submit" disabled={saveState.saving}>
                      {saveState.saving ? "Saving..." : "Save"}
                    </Button>
                  </div>
                )}
            </form>

            <div className="space-y-4">
              <h2
                className="px-1 text-3xl font-bold tracking-tight"
                ref={messagesHeadingRef}
              >
                Messages
              </h2>
              {!ticket.messages || ticket.messages.length === 0 ? (
                <p className="rounded-md border border-border/80 bg-muted/20 p-4 text-muted-foreground">
                  No messages yet.
                </p>
              ) : (
                <div className="space-y-4">
                  {[...(ticket.messages || [])]
                    .sort((a, b) => (b.date ?? "").localeCompare(a.date ?? ""))
                    .map((message) => (
                      <TicketMessageCard
                        key={`msg-${message.id}`}
                        message={message}
                        enableAttachmentPreviews={enableAttachmentPreviews}
                        crossReferences={crossReferences}
                      />
                    ))}
                </div>
              )}
            </div>
            {!isClosed ? (
              <div className="space-y-4">
                <h2 className="px-1 text-3xl font-bold tracking-tight">
                  Reply
                </h2>
                <form className="space-y-6" onSubmit={submitReply}>
                  <LexicalEditor
                    value={replyBody}
                    onChange={setReplyBody}
                    inputRef={replyInputRef}
                    name="body"
                    rows={6}
                    required
                    ticketSuggestApiBase={apiBase}
                    excludeTicketId={ticket.id}
                  />

                  <div className="space-y-3">
                    <label className="text-sm font-semibold leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                      Attachments
                    </label>
                    <div className="overflow-hidden rounded-md border">
                      <Table>
                        <TableHeader>
                          <TableRow className="bg-muted/50 hover:bg-muted/50">
                            <TableHead>Name</TableHead>
                            <TableHead>Type</TableHead>
                            <TableHead>Size</TableHead>
                            <TableHead className="w-[100px]"></TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {files.map((file, index) => (
                            <TableRow
                              key={`${file.name}-${file.size}-${index}`}
                            >
                              <TableCell className="font-medium">
                                {file.name}
                              </TableCell>
                              <TableCell className="text-muted-foreground">
                                {file.type || "application/octet-stream"}
                              </TableCell>
                              <TableCell className="text-muted-foreground">
                                {formatFileSize(file.size)}
                              </TableCell>
                              <TableCell className="text-right">
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="sm"
                                  className="h-8 px-2 text-destructive hover:bg-destructive/10 hover:text-destructive"
                                  onClick={() => removeReplyFile(index)}
                                >
                                  Remove
                                </Button>
                              </TableCell>
                            </TableRow>
                          ))}
                          {files.length === 0 && (
                            <TableRow>
                              <TableCell
                                colSpan={4}
                                className="h-24 text-center text-muted-foreground"
                              >
                                No attachments selected.
                              </TableCell>
                            </TableRow>
                          )}
                        </TableBody>
                      </Table>
                    </div>
                    <div className="pt-2">
                      <Input
                        ref={fileInputRef}
                        type="file"
                        name="attachments"
                        multiple
                        className="max-w-xs cursor-pointer"
                        onChange={addReplyFiles}
                      />
                    </div>
                  </div>

                  {replyState.error && (
                    <p className="text-sm font-medium text-destructive">
                      {replyState.error}
                    </p>
                  )}

                  <div className="mt-8 flex items-center justify-between pt-6">
                    <div>
                      {ticket.exportPath && (
                        <Button variant="outline" asChild>
                          <a href={ticket.exportPath}>Export</a>
                        </Button>
                      )}
                    </div>
                    <Button type="submit" disabled={replyState.saving}>
                      {replyState.saving ? "Adding..." : "Add"}
                    </Button>
                  </div>
                </form>
              </div>
            ) : (
              ticket.exportPath && (
                <div className="flex justify-end pt-4 mt-2">
                  <Button variant="outline" asChild>
                    <a href={ticket.exportPath}>Export History</a>
                  </Button>
                </div>
              )
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}
