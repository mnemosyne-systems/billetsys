import type { Id } from '../app';

export interface NamedEntity {
  id: Id;
  name?: string;
  [key: string]: unknown;
}

export interface CountryOption extends NamedEntity {}

export interface TimezoneOption extends NamedEntity {
  countryId?: Id;
}

export interface SelectOption extends NamedEntity {
  value?: string;
  label?: string;
}

export interface VersionInfo extends NamedEntity {
  date?: string;
}

export interface NavigationLink {
  href: string;
  label: string;
  [key: string]: unknown;
}

export interface CollectionResponse<T> {
  title?: string;
  createPath?: string;
  items: T[];
  canCreate?: boolean;
  [key: string]: unknown;
}

export interface BootstrapResponse {
  [key: string]: unknown;
}
