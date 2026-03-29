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
  primaryContactId: string;
  primaryContactUsername: string;
  primaryContactFullName: string;
  primaryContactEmail: string;
  primaryContactSocial: string;
  primaryContactPhoneNumber: string;
  primaryPhoneNumberExtension: string;
  primaryContactCountry: string;
  primaryContactTimeZone: string;
  primaryContactPassword: string;
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
