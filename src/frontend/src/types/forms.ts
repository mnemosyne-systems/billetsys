/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

export interface CompanyEntitlementEntry {
  entitlementId: string;
  levelId: string;
  date: string;
  duration: string;
}

export interface CompanyFormState {
  name: string;
  address1: string;
  address2: string;
  city: string;
  state: string;
  zip: string;
  phoneNumber: string;
  countryId: string;
  timezoneId: string;
  selectedUserIds: Array<string | number>;
  selectedTamIds: Array<string | number>;
  entitlements: CompanyEntitlementEntry[];
  superuserId: string;
  superuserUsername: string;
  superuserFullName: string;
  superuserEmail: string;
  superuserSocial: string;
  superuserPhoneNumber: string;
  superuserPhoneExtension: string;
  superuserCountryId: string;
  superuserTimezoneId: string;
  superuserPassword: string;
}

export interface SupportTicketCreateFormState {
  ticketName: string;
  companyId: string;
  companyEntitlementId: string;
  categoryId: string;
  affectsVersionId: string;
  message: string;
}

export interface SupportTicketDetailState {
  status: string;
  categoryId: string;
  externalIssueLink: string;
  affectsVersionId: string;
  resolvedVersionId: string;
}

export interface TicketWorkbenchFormState {
  id: string;
  status: string;
  companyId: string;
  companyEntitlementId: string;
  categoryId: string;
  externalIssueLink: string;
  affectsVersionId: string;
  resolvedVersionId: string;
}
