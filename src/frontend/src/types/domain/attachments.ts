/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { NamedEntity } from "./common";
import type { UserReference } from "./users";
import type { Id } from "../app";

export interface AttachmentReference extends NamedEntity {
  downloadPath?: string;
  mimeType?: string;
  size?: number;
  sizeLabel?: string;
}

export interface AttachmentLine {
  number: number | string;
  content: string;
}

export interface AttachmentDetail extends AttachmentReference {
  backPath?: string;
  ticketName?: string;
  image?: boolean;
  lines?: AttachmentLine[];
  messageBody?: string;
}

export interface MessageReference extends NamedEntity {
  ticketName?: string;
  date?: string;
  dateLabel?: string;
  preview?: string;
  body?: string;
  editPath?: string;
  attachmentCount?: number;
  attachments?: AttachmentReference[];
  author?: UserReference;
  authorName?: string;
}

export interface MessageFormBootstrap {
  title?: string;
  submitPath: string;
  cancelPath?: string;
  edit?: boolean;
  message?: {
    body?: string;
    date?: string;
    ticketId?: Id;
  };
  tickets?: NamedEntity[];
  attachments?: AttachmentReference[];
}
