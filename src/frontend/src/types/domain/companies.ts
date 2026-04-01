/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { CountryOption, NamedEntity, TimezoneOption } from './common';
import type { LevelRecord } from './content';
import type { UserReference, DirectoryUserRecord } from './users';

export interface CompanyAssignment {
  entitlementId?: string | number;
  levelId?: string | number;
  entitlementName?: string;
  levelName?: string;
}

export interface CompanyRecord extends NamedEntity {
  createPath?: string;
  countryName?: string;
  timezoneName?: string;
  phoneNumber?: string;
  address1?: string;
  address2?: string;
  city?: string;
  state?: string;
  zip?: string;
  superuserCount?: number;
  userCount?: number;
  tamCount?: number;
  entitlementAssignments?: CompanyAssignment[];
  selectedSuperusers?: UserReference[];
  selectedUsers?: UserReference[];
  selectedTams?: UserReference[];
}

export interface DirectoryCompanyDetail extends CompanyRecord {
  backPath?: string;
  users?: DirectoryUserRecord[];
  superusers?: DirectoryUserRecord[];
  tamUsers?: DirectoryUserRecord[];
  supportUsers?: DirectoryUserRecord[];
}

export interface CompanyEntitlementOption extends NamedEntity {
  levelName?: string;
}

export interface DurationOption {
  value?: string | number;
  label?: string;
}

export interface CompanyFormBootstrap extends NamedEntity {
  address1?: string;
  address2?: string;
  city?: string;
  state?: string;
  zip?: string;
  phoneNumber?: string;
  countryId?: string | number;
  timezoneId?: string | number;
  defaultCountryId?: string | number;
  defaultTimezoneId?: string | number;
  selectedUserIds?: Array<string | number>;
  selectedTamIds?: Array<string | number>;
  superuserId?: string | number;
  selectedSuperusers?: UserReference[];
  userOptions?: UserReference[];
  tamOptions?: UserReference[];
  countries?: CountryOption[];
  timezones?: TimezoneOption[];
  entitlements?: NamedEntity[];
  levels?: LevelRecord[];
  durations?: DurationOption[];
  todayDate?: string;
  entitlementAssignments?: Array<{
    entitlementId?: string | number;
    levelId?: string | number;
    date?: string;
    duration?: string | number;
  }>;
}
