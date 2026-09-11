/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ChangeEvent } from "react";
import type { AttachmentReference } from "../../types/domain";
import { Input } from "../ui/input";
import { Field, FieldLabel } from "../ui/field";

function fileMimeType(file: File): string {
  const ext = file.name.toLowerCase();
  if (ext.endsWith(".cast")) return "application/x-asciinema";
  return file.type || "application/octet-stream";
}

interface AttachmentPickerProps {
  files: File[];
  onFilesChange: (files: File[]) => void;
  existingAttachments?: AttachmentReference[];
}

export default function AttachmentPicker({
  files,
  onFilesChange,
  existingAttachments,
}: AttachmentPickerProps) {
  return (
    <Field className="w-full">
      <FieldLabel className="text-muted-foreground">Attachments</FieldLabel>
      <div className="grid gap-4 mt-2">
        <Input
          type="file"
          multiple
          className="bg-background shadow-sm"
          onChange={(event: ChangeEvent<HTMLInputElement>) =>
            onFilesChange(Array.from(event.target.files || []))
          }
        />

        {files.length > 0 && (
          <div className="grid gap-2">
            {files.map((file) => (
              <div
                key={`${file.name}-${file.size}`}
                className="flex items-center justify-between text-sm p-3 rounded-lg bg-muted/30 border border-border/60 shadow-[0_1px_2px_rgba(0,0,0,0.02)]"
              >
                <strong className="font-medium truncate text-foreground/90">
                  {file.name}
                </strong>
                <span className="text-muted-foreground/80 shrink-0 ml-4 uppercase tracking-wider text-xs font-medium">
                  {fileMimeType(file).split("/").pop() || "FILE"}
                </span>
              </div>
            ))}
          </div>
        )}
        {!!existingAttachments?.length && (
          <div className="mt-4">
            <h4 className="font-medium text-muted-foreground mb-3 text-sm">
              Existing Attachments
            </h4>
            <div className="flex flex-wrap gap-3">
              {existingAttachments.map((attachment) => (
                <a
                  key={attachment.id}
                  href={attachment.downloadPath}
                  target="_blank"
                  rel="noreferrer"
                  className="group relative flex flex-col justify-center gap-1.5 p-3.5 pr-8 rounded-xl border border-border/60 bg-muted/20 hover:bg-muted/50 transition-all w-72 shadow-[0_1px_2px_rgba(0,0,0,0.02)] hover:shadow-sm"
                >
                  <div className="font-medium text-sm text-foreground truncate group-hover:text-primary transition-colors">
                    {attachment.name}
                  </div>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground/80 font-medium">
                    <span className="uppercase tracking-wider">
                      {attachment.mimeType?.split("/").pop() || "FILE"}
                    </span>
                    <span>•</span>
                    <span>{attachment.sizeLabel}</span>
                  </div>
                  <div className="absolute right-3 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      className="text-primary"
                    >
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                      <polyline points="7 10 12 15 17 10" />
                      <line x1="12" x2="12" y1="15" y2="3" />
                    </svg>
                  </div>
                </a>
              ))}
            </div>
          </div>
        )}
      </div>
    </Field>
  );
}
