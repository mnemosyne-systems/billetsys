/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type {
  CollectionResponse,
  CountryOption,
  NamedEntity,
  TimezoneOption,
} from "./common";
import type { Id } from "../app";

export interface UserReference extends NamedEntity {
  username?: string;
  displayName?: string;
  fullName?: string;
  email?: string;
  companyName?: string;
  detailPath?: string;
  profilePath?: string;
  logoBase64?: string;
  countryName?: string;
  timezoneName?: string;
  social?: string;
  phoneNumber?: string;
  phoneExtension?: string;
  type?: string;
  typeLabel?: string;
  companyId?: Id;
  countryId?: Id;
  timezoneId?: Id;
}

export interface OwnerCompany extends NamedEntity {
  phoneNumber?: string;
  countryName?: string;
  timezoneName?: string;
  address1?: string;
  address2?: string;
  city?: string;
  state?: string;
  zip?: string;
  countryId?: Id;
  timezoneId?: Id;
  supportUsers: UserReference[];
  tamUsers: UserReference[];
  supportOptions: UserReference[];
  tamOptions: UserReference[];
  countries: CountryOption[];
  timezones: TimezoneOption[];
}

export interface ProfileRecord extends UserReference {
  countries: CountryOption[];
  timezones: TimezoneOption[];
  companies: NamedEntity[];
  canSelectCompany?: boolean;
  currentCompanyId?: Id;
}

export interface DirectoryUserRecord extends UserReference {
  editPath?: string;
}

export interface DirectoryUsersResponse extends CollectionResponse<DirectoryUserRecord> {
  description?: string;
  showCompanySelector?: boolean;
  selectedCompanyId?: Id;
  companies?: NamedEntity[];
}

export interface DirectoryUserBootstrap {
  title?: string;
  submitPath: string;
  cancelPath?: string;
  companyLocked?: boolean;
  passwordRequired?: boolean;
  selectedCompanyId?: Id;
  user?: {
    id?: Id;
    name?: string;
    fullName?: string;
    email?: string;
    social?: string;
    phoneNumber?: string;
    phoneExtension?: string;
    countryId?: Id;
    timezoneId?: Id;
    type?: string;
    companyId?: Id;
  };
  countries?: CountryOption[];
  timezones?: TimezoneOption[];
  types?: Array<{ value?: string; label?: string }>;
  companies?: NamedEntity[];
  [key: string]: unknown;
}

export interface DirectoryUserDetail extends DirectoryUserRecord {
  backPath?: string;
  companyPath?: string;
  deletePath?: string;
}
