/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useParams } from "react-router-dom";
import DataState from "../components/common/DataState";
import useJson from "../hooks/useJson";
import { SmartLink } from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type { AttachmentDetail } from "../types/domain";
import { Card, CardContent } from "../components/ui/card";
import { Button } from "../components/ui/button";
import PageHeader from "../components/layout/PageHeader";

export default function AttachmentPage(props: SessionPageProps) {
  void props;
  const { id } = useParams();
  const attachmentState = useJson<AttachmentDetail>(
    id ? `/api/attachments/${id}` : null,
  );
  const attachment = attachmentState.data;
  const attachmentTitle = attachment?.ticketName
    ? `${attachment.ticketName}: ${attachment.name || "Attachment"}`
    : attachment?.name || "Attachment";
  return (
    <section className="w-full mt-4">
      <PageHeader title={attachmentTitle} titleClassName="text-3xl font-bold" />

      <DataState state={attachmentState} emptyMessage="Attachment not found.">
        {attachment && (
          <div className="space-y-6">
            {attachment.image ? (
              <Card>
                <CardContent className="p-0 overflow-hidden rounded-lg flex items-center justify-center bg-muted/10 border-none shadow-none">
                  <img
                    src={attachment.downloadPath}
                    alt={attachment.name}
                    className="max-w-full h-auto object-contain max-h-[80vh] rounded-lg"
                  />
                </CardContent>
              </Card>
            ) : (
              <Card>
                <CardContent className="p-0">
                  <pre className="p-4 overflow-x-auto text-sm font-mono bg-muted/10 rounded-lg whitespace-pre-wrap break-words">
                    {(attachment.lines || [])
                      .map((line) => line.content)
                      .join("\n") ||
                      attachment.messageBody ||
                      ""}
                  </pre>
                </CardContent>
              </Card>
            )}

            <div className="text-sm text-muted-foreground flex items-center space-x-2">
              <span className="font-medium bg-muted px-2 py-0.5 rounded-md text-xs border">
                {attachment.mimeType || "Unknown Type"}
              </span>
              {attachment.sizeLabel && <span>{attachment.sizeLabel}</span>}
            </div>

            {(attachment.backPath || attachment.downloadPath) && (
              <div className="flex items-center justify-between pt-4 border-t">
                {attachment.backPath ? (
                  <Button asChild>
                    <SmartLink href={attachment.backPath}>
                      Back to Ticket
                    </SmartLink>
                  </Button>
                ) : (
                  <span aria-hidden="true" />
                )}
                {attachment.downloadPath && (
                  <Button asChild>
                    <a
                      href={attachment.downloadPath}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Download File
                    </a>
                  </Button>
                )}
              </div>
            )}
          </div>
        )}
      </DataState>
    </section>
  );
}
