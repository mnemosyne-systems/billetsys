/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ChangeEvent } from "react";
import type { AttachmentReference } from "../../types/domain";
import { Card, CardHeader, CardTitle, CardContent } from "../ui/card";
import { Input } from "../ui/input";

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
    <Card>
      <CardHeader>
        <CardTitle>Attachments</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-4">
        <Input
          type="file"
          multiple
          onChange={(event: ChangeEvent<HTMLInputElement>) =>
            onFilesChange(Array.from(event.target.files || []))
          }
        />
        <div className="grid gap-2">
          {files.map((file) => (
            <div
              key={`${file.name}-${file.size}`}
              className="flex items-center justify-between text-sm p-2 rounded-md bg-muted/50 border border-border"
            >
              <strong className="font-medium truncate">{file.name}</strong>
              <span className="text-muted-foreground shrink-0 ml-4">
                {file.type || "application/octet-stream"}
              </span>
            </div>
          ))}
          {files.length === 0 && (
            <p className="text-muted-foreground text-sm">
              No new attachments selected.
            </p>
          )}
        </div>
        {!!existingAttachments?.length && (
          <div className="mt-2">
            <h4 className="font-semibold mb-3 text-sm">Existing attachments</h4>
            <div className="w-full text-sm rounded-md border border-border overflow-hidden">
              <div className="grid grid-cols-[minmax(0,1fr)_120px_80px] gap-4 p-2.5 bg-muted font-semibold text-muted-foreground">
                <span>Name</span>
                <span>Type</span>
                <span>Size</span>
              </div>
              {existingAttachments.map((attachment) => (
                <div
                  key={attachment.id}
                  className="grid grid-cols-[minmax(0,1fr)_120px_80px] gap-4 p-2.5 border-t border-border hover:bg-muted/50 transition-colors"
                >
                  <a
                    href={attachment.downloadPath}
                    target="_blank"
                    rel="noreferrer"
                    className="text-primary hover:underline truncate"
                  >
                    {attachment.name}
                  </a>
                  <span className="text-muted-foreground truncate">
                    {attachment.mimeType}
                  </span>
                  <span className="text-muted-foreground tabular-nums whitespace-nowrap">
                    {attachment.sizeLabel}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
