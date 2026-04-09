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
  MessageReference,
  NamedEntity,
  SupportTicketDetailRecord,
  VersionInfo,
} from "../types/domain";
import type { SupportTicketDetailState } from "../types/forms";
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
import { Alert, AlertDescription, AlertTitle } from "../components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogTrigger,
} from "../components/ui/dialog";
import { ScrollArea } from "../components/ui/scroll-area";
import { CheckCircle2Icon, Maximize2Icon } from "lucide-react";

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
    return (
      <div className="attachment-summary">
        <span className="attachment-name">
          <a href={attachment.downloadPath} target="_blank" rel="noreferrer">
            {attachment.name}
          </a>
        </span>
        <span className="attachment-meta">
          {attachment.mimeType} - {attachment.sizeLabel}
        </span>
      </div>
    );
  }

  return (
    <div
      className="attachment-preview-hover"
      onMouseEnter={() => setPreviewRequested(true)}
      onFocusCapture={() => setPreviewRequested(true)}
    >
      <div className="attachment-summary">
        <span className="attachment-name">
          <a href={attachment.downloadPath} target="_blank" rel="noreferrer">
            {attachment.name}
          </a>
        </span>
        <span className="attachment-meta">
          {attachment.mimeType} - {attachment.sizeLabel}
        </span>
      </div>

      <div className="attachment-preview-popover">
        {previewState.loading ? (
          <div className="attachment-preview-window attachment-preview-status">
            Loading preview...
          </div>
        ) : previewState.error ||
          previewState.unauthorized ||
          previewState.forbidden ||
          !preview ? (
          <div className="attachment-preview-window attachment-preview-status">
            Preview unavailable.
          </div>
        ) : previewKind === "image" && preview.downloadPath ? (
          <div className="attachment-preview-window attachment-preview-image">
            <img src={preview.downloadPath} alt={attachment.name} />
          </div>
        ) : previewKind === "pdf" && preview.downloadPath ? (
          <iframe
            className="attachment-preview-window attachment-preview-pdf"
            src={preview.downloadPath}
            title={`Preview of ${attachment.name || "attachment"}`}
          />
        ) : previewKind === "markdown" ? (
          <div className="attachment-preview-window attachment-preview-markdown markdown-output">
            <MarkdownContent>{attachmentPreviewText(preview)}</MarkdownContent>
          </div>
        ) : (
          <pre className="attachment-preview-window attachment-preview-text">
            {attachmentPreviewText(preview)}
          </pre>
        )}
      </div>
    </div>
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
      <DataState
        state={ticketState}
        emptyMessage="Ticket not found."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {ticket && formState && (
          <>
            <div className="flex flex-col lg:flex-row gap-8 items-start lg:items-stretch">
              <div className="flex-1 w-full min-w-0 space-y-6">
                <Card className="h-full">
                  <CardHeader className="pb-4">
                    <CardTitle className="text-xl font-bold tracking-tight">
                      {ticket.title || ticket.name || titleFallback}
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <form onSubmit={saveTicket} className="space-y-6">
                      <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
                        <Field className="md:col-span-2 xl:col-span-3">
                          <FieldLabel>Title</FieldLabel>
                          {isClosed ? (
                            <Input
                              value={
                                formState.title ||
                                ticket.title ||
                                ticket.name ||
                                ""
                              }
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
                        <Field>
                          <FieldLabel>Company</FieldLabel>
                          {ticket.companyId ? (
                            <div className="relative">
                              <Input
                                value={ticket.companyName || ""}
                                readOnly
                              />
                              <a
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-sm font-medium text-primary hover:underline bg-background pl-2"
                                href={`/support/companies/${ticket.companyId}`}
                              >
                                {ticket.companyName || "-"}
                              </a>
                            </div>
                          ) : (
                            <Input value={ticket.companyName || "-"} readOnly />
                          )}
                        </Field>
                        <Field>
                          <FieldLabel>Category</FieldLabel>
                          {isClosed || !canEditCategory ? (
                            <Input
                              value={ticket.categoryName || "-"}
                              readOnly
                            />
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
                                {(ticket.versions || []).map(
                                  (version: VersionInfo) => (
                                    <SelectItem
                                      key={version.id}
                                      value={String(version.id)}
                                    >
                                      {version.name} ({version.date})
                                    </SelectItem>
                                  ),
                                )}
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
                                {(ticket.versions || []).map(
                                  (version: VersionInfo) => (
                                    <SelectItem
                                      key={version.id}
                                      value={String(version.id)}
                                    >
                                      {version.name} ({version.date})
                                    </SelectItem>
                                  ),
                                )}
                              </SelectContent>
                            </Select>
                          )}
                        </Field>
                        <Field>
                          <FieldLabel>Support</FieldLabel>
                          <UserReferenceInlineList
                            users={ticket.supportUsers}
                          />
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
                          <div className="flex justify-end pt-4 border-t">
                            <Button type="submit" disabled={saveState.saving}>
                              {saveState.saving ? "Saving..." : "Save"}
                            </Button>
                          </div>
                        )}
                    </form>
                  </CardContent>
                </Card>
              </div>

              <div className="w-full lg:w-[420px] xl:w-[480px] shrink-0 lg:relative">
                <div className="flex flex-col space-y-6 lg:absolute lg:inset-0">
                  <h2
                    className="text-2xl font-bold tracking-tight shrink-0"
                    ref={messagesHeadingRef}
                  >
                    Messages
                  </h2>
                  <div className="flex-1 min-h-0">
                    {!ticket.messages || ticket.messages.length === 0 ? (
                      <p className="text-muted-foreground p-4 bg-muted/20 rounded-md border">
                        No messages yet.
                      </p>
                    ) : (
                      <ScrollArea className="h-[400px] lg:h-full pr-4">
                        <div className="space-y-4">
                          {(ticket.messages || []).map(
                            (message: MessageReference) => (
                              <Dialog key={message.id}>
                                <Alert className="relative overflow-hidden bg-muted/10 shadow-sm border-muted-foreground/20">
                                  <CheckCircle2Icon className="h-5 w-5 mt-0.5 text-primary" />
                                  <AlertTitle className="flex justify-between items-center text-sm ml-2 pr-6">
                                    <span className="font-semibold text-foreground truncate max-w-[150px]">
                                      {message.author?.detailPath ? (
                                        <UserHoverLink
                                          user={message.author}
                                          className="hover:underline"
                                        >
                                          {message.author.displayName ||
                                            message.author.username}
                                        </UserHoverLink>
                                      ) : (
                                        message.author?.displayName ||
                                        message.author?.username ||
                                        "Unknown"
                                      )}
                                    </span>
                                    <span className="flex items-center gap-1 text-[11px] text-muted-foreground font-normal whitespace-nowrap">
                                      {message.dateLabel?.split(",")[0] || "-"}
                                    </span>
                                  </AlertTitle>
                                  <AlertDescription className="mt-3 ml-2 text-sm leading-relaxed text-foreground">
                                    <div className="prose prose-sm dark:prose-invert max-w-none [&>p:first-child]:mt-0 [&>p:last-child]:mb-0 line-clamp-4">
                                      <MarkdownContent>
                                        {message.body || ""}
                                      </MarkdownContent>
                                    </div>
                                  </AlertDescription>
                                  <DialogTrigger asChild>
                                    <Button
                                      variant="ghost"
                                      size="icon"
                                      className="absolute top-2 right-2 h-6 w-6 text-muted-foreground hover:bg-muted/50 transition-colors"
                                    >
                                      <Maximize2Icon className="h-3 w-3" />
                                    </Button>
                                  </DialogTrigger>

                                  <DialogContent className="max-w-3xl w-full max-h-[85vh] flex flex-col !p-0 overflow-hidden border-muted-foreground/20 shadow-2xl">
                                    <div className="bg-background border-b px-6 py-4 flex items-center justify-between pr-14 relative">
                                      <div className="flex items-center gap-3.5">
                                        <div className="h-10 w-10 shrink-0 rounded-full bg-primary/10 text-primary flex items-center justify-center font-semibold text-base border border-primary/20">
                                          {(
                                            message.author?.displayName ||
                                            message.author?.username ||
                                            "U"
                                          )
                                            .charAt(0)
                                            .toUpperCase()}
                                        </div>
                                        <div className="flex flex-col">
                                          <DialogTitle className="text-base font-semibold leading-none tracking-tight">
                                            {message.author?.displayName ||
                                              message.author?.username ||
                                              "Unknown"}
                                          </DialogTitle>
                                          <span className="text-xs text-muted-foreground mt-1.5 font-medium">
                                            {message.dateLabel}
                                          </span>
                                        </div>
                                      </div>
                                    </div>

                                    <div className="flex-1 overflow-auto p-6 md:p-8 bg-background">
                                      <div className="prose prose-sm md:prose-base dark:prose-invert max-w-none">
                                        <MarkdownContent>
                                          {message.body || ""}
                                        </MarkdownContent>
                                      </div>
                                      {(message.attachments || []).length >
                                        0 && (
                                        <div className="mt-8 pt-6 border-t border-muted-foreground/10 flex flex-wrap gap-3">
                                          {(message.attachments || []).map(
                                            (
                                              attachment: AttachmentReference,
                                            ) => (
                                              <div
                                                key={attachment.id}
                                                className="text-sm"
                                              >
                                                {enableAttachmentPreviews ? (
                                                  <AttachmentPreview
                                                    attachment={attachment}
                                                  />
                                                ) : (
                                                  <div className="inline-flex items-center gap-2 px-4 py-2 border rounded-md bg-muted/30 hover:bg-muted/50 transition-colors">
                                                    <a
                                                      href={
                                                        attachment.downloadPath
                                                      }
                                                      target="_blank"
                                                      rel="noreferrer"
                                                      className="font-medium hover:underline text-sm"
                                                    >
                                                      {attachment.name}
                                                    </a>
                                                    <span className="text-xs text-muted-foreground border-l pl-2">
                                                      {attachment.sizeLabel}
                                                    </span>
                                                  </div>
                                                )}
                                              </div>
                                            ),
                                          )}
                                        </div>
                                      )}
                                    </div>
                                  </DialogContent>
                                </Alert>
                              </Dialog>
                            ),
                          )}
                        </div>
                      </ScrollArea>
                    )}
                  </div>
                </div>
              </div>
            </div>

            <div className="w-full mt-6">
              {!isClosed ? (
                <Card className="shadow-sm border-muted-foreground/20">
                  <CardContent className="pt-2 px-6 pb-6">
                    <h3 className="text-lg font-bold mb-3">Add Reply</h3>
                    <form className="space-y-6" onSubmit={submitReply}>
                      <LexicalEditor
                        value={replyBody}
                        onChange={setReplyBody}
                        inputRef={replyInputRef}
                        name="body"
                        rows={6}
                        required
                      />

                      <div className="space-y-3">
                        <label className="text-lg font-bold leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                          Attachments
                        </label>
                        <div className="rounded-md border overflow-hidden">
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

                      <div className="flex items-center justify-between pt-6 border-t mt-8">
                        <div>
                          {ticket.exportPath && (
                            <Button variant="outline" asChild>
                              <a href={ticket.exportPath}>Export</a>
                            </Button>
                          )}
                        </div>
                        <div className="flex gap-3">
                          <Button type="submit" disabled={replyState.saving}>
                            {replyState.saving ? "Adding..." : "Add reply"}
                          </Button>
                        </div>
                      </div>
                    </form>
                  </CardContent>
                </Card>
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
          </>
        )}
      </DataState>
    </section>
  );
}
