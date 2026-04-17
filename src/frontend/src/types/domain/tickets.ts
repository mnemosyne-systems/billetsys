/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { MessageReference } from "./attachments";
import type { CompanyEntitlementOption } from "./companies";
import type { NamedEntity, VersionInfo } from "./common";
import type { UserReference } from "./users";
import type { Id } from "../app";

export interface TicketReference extends NamedEntity {
  title?: string;
  status?: string;
  displayStatus?: string;
  categoryId?: Id;
  categoryName?: string;
  companyId?: Id;
  companyName?: string;
  companyEntitlementId?: Id;
  entitlementName?: string;
  levelName?: string;
  externalIssueLink?: string;
  affectsVersionId?: Id;
  resolvedVersionId?: Id;
  editableStatus?: boolean;
  editableCategory?: boolean;
  editableExternalIssue?: boolean;
  editableAffectsVersion?: boolean;
  editableResolvedVersion?: boolean;
  secondaryUsersLabel?: string;
  supportUsers?: UserReference[];
  secondaryUsers?: UserReference[];
  tamUsers?: UserReference[];
  categories?: NamedEntity[];
  statusOptions?: string[];
  versions?: VersionInfo[];
  messages?: MessageReference[];
  actionPath?: string;
  exportPath?: string;
  messageActionPath?: string;
  ticketEntitlementExpired?: boolean;
}

export interface TicketListItem extends TicketReference {
  detailPath?: string;
  messageDirectionArrow?: string;
  messageDateLabel?: string;
  status?: string;
  supportUser?: UserReference;
  companyPath?: string;
  affectsVersionName?: string;
  resolvedVersionName?: string;
  slaColor?: string;
}

export interface TicketWorkbenchListItem extends TicketListItem {
  requesterName?: string;
  lastMessageLabel?: string;
  editPath?: string;
}

export interface SupportTicketCreateBootstrap {
  submitPath?: string;
  selectedCompanyId?: Id;
  selectedCompanyEntitlementId?: Id;
  defaultCategoryId?: Id;
  defaultAffectsVersion?: VersionInfo;
  ticketName?: string;
  title?: string;
  companies?: NamedEntity[];
  companyEntitlements?: CompanyEntitlementOption[];
  categories?: NamedEntity[];
  versions?: VersionInfo[];
  [key: string]: unknown;
}

export interface TicketWorkbenchBootstrap {
  title?: string;
  submitPath: string;
  edit?: boolean;
  ticket?: TicketReference;
  companies?: NamedEntity[];
  entitlements?: CompanyEntitlementOption[];
  categories?: NamedEntity[];
  versions?: VersionInfo[];
  messages?: MessageReference[];
  [key: string]: unknown;
}

export interface EventEntry {
  id: number;
  action: string;
  dateLabel?: string;
  user?: UserReference;
}

export interface SupportTicketDetailRecord extends TicketReference {
  categories?: NamedEntity[];
  versions?: VersionInfo[];
  messages?: MessageReference[];
  events?: EventEntry[];
}
