/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import AttachmentPicker from "../components/common/AttachmentPicker";
import DataState from "../components/common/DataState";
import { toast } from "sonner";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import { postForm, postMultipart } from "../utils/api";
import { toQueryString } from "../utils/formatting";
import { PATHS } from "../routes/paths";
import { resolvePostRedirectPath } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { MessageFormBootstrap } from "../types/domain";
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
import { Textarea } from "../components/ui/textarea";

interface MessageFormState {
  body: string;
  date: string;
  ticketId: string;
  files: File[];
}

export default function MessageFormPage(props: SessionPageProps) {
  void props;
  const navigate = useNavigate();

  const { id } = useParams();
  const bootstrapState = useJson<MessageFormBootstrap>(
    `/api/messages/bootstrap${toQueryString({ messageId: id })}`,
  );
  const bootstrap = bootstrapState.data;
  const [formState, setFormState] = useState<MessageFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const submissionGuard = useSubmissionGuard();

  const updateFormState = <K extends keyof MessageFormState>(
    field: K,
    value: MessageFormState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState({
      body: bootstrap.message?.body || "",
      date: bootstrap.message?.date || "",
      ticketId: bootstrap.message?.ticketId
        ? String(bootstrap.message.ticketId)
        : "",
      files: [],
    });
  }, [bootstrap]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!bootstrap || !formState || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postMultipart(bootstrap.submitPath, [
        ["body", formState.body],
        ["date", formState.date],
        ["ticketId", formState.ticketId],
        ["attachments", formState.files],
      ]);
      toast.success(
        bootstrap.edit
          ? "Message updated successfully."
          : "Message created successfully.",
      );
      navigate(await resolvePostRedirectPath(response, PATHS.messages));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to save message.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to save message.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const deleteMessage = async () => {
    if (!id || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(`/messages/${id}/delete`, []);
      toast.success("Message deleted.");
      navigate(await resolvePostRedirectPath(response, PATHS.messages));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to delete message.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to delete message.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="w-full mt-4">
      <div className="flex items-center justify-between pb-6 px-1">
        <h2 className="text-3xl font-bold tracking-tight">
          {bootstrap && bootstrap.edit ? "Edit" : "Create"}
        </h2>
      </div>

      <DataState
        state={bootstrapState}
        emptyMessage="Unable to load the message form."
      >
        {bootstrap && formState && (
          <form className="space-y-6 pb-20" onSubmit={submit}>
            <Card>
              <CardHeader>
                <CardTitle>Message Details</CardTitle>
                <p className="text-sm text-muted-foreground mt-1">
                  Required fields are marked{" "}
                  <span className="text-destructive">*</span>.
                </p>
              </CardHeader>
              <CardContent className="grid gap-6 md:grid-cols-2 pt-4">
                <Field className="md:col-span-2">
                  <FieldLabel>
                    Body <span className="text-destructive">*</span>
                  </FieldLabel>
                  <Textarea
                    rows={8}
                    value={formState.body}
                    onChange={(event) =>
                      updateFormState("body", event.target.value)
                    }
                    required
                  />
                </Field>
                <Field>
                  <FieldLabel>
                    Date <span className="text-destructive">*</span>
                  </FieldLabel>
                  <Input
                    type="datetime-local"
                    value={formState.date}
                    onChange={(event) =>
                      updateFormState("date", event.target.value)
                    }
                    required
                  />
                </Field>
                <Field>
                  <FieldLabel>
                    Ticket <span className="text-destructive">*</span>
                  </FieldLabel>
                  <Select
                    value={formState.ticketId || undefined}
                    onValueChange={(value) =>
                      updateFormState("ticketId", value)
                    }
                    required
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Select ticket" />
                    </SelectTrigger>
                    <SelectContent>
                      {(bootstrap.tickets || []).map((ticket) => (
                        <SelectItem key={ticket.id} value={String(ticket.id)}>
                          {ticket.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Attachments</CardTitle>
              </CardHeader>
              <CardContent>
                <AttachmentPicker
                  files={formState.files}
                  onFilesChange={(files) => updateFormState("files", files)}
                  existingAttachments={bootstrap.attachments || []}
                />
              </CardContent>
            </Card>

            <div className="flex items-center justify-end space-x-3 pt-4 border-t">
              {id && (
                <AlertDialog>
                  <AlertDialogTrigger asChild>
                    <Button
                      type="button"
                      variant="destructive"
                      disabled={saveState.saving}
                      className="mr-auto"
                    >
                      Delete message
                    </Button>
                  </AlertDialogTrigger>
                  <AlertDialogContent>
                    <AlertDialogHeader>
                      <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                      <AlertDialogDescription>
                        This action cannot be undone. This will permanently
                        delete this message.
                      </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                      <AlertDialogCancel>Cancel</AlertDialogCancel>
                      <AlertDialogAction
                        onClick={deleteMessage}
                        className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                      >
                        Delete
                      </AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
              )}
              <Button type="submit" disabled={saveState.saving}>
                {saveState.saving
                  ? "Saving..."
                  : bootstrap.edit
                    ? "Save"
                    : "Create"}
              </Button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}
