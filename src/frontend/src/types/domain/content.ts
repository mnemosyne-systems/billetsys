import type { AttachmentReference } from './attachments';
import type { VersionInfo, NamedEntity } from './common';

export interface ArticleRecord extends NamedEntity {
  title?: string;
  tags?: string;
  body?: string;
  attachments: AttachmentReference[];
  canCreate?: boolean;
  canDelete?: boolean;
  canEdit?: boolean;
  createPath?: string;
  editPath?: string;
}

export interface CategoryRecord extends NamedEntity {
  description?: string;
  descriptionPreview?: string;
  isDefault?: boolean;
  attachments: AttachmentReference[];
  canCreate?: boolean;
  createPath?: string;
  editPath?: string;
}

export interface LevelRecord extends NamedEntity {
  level?: string | number;
  color?: string;
  colorDisplay?: string;
  description?: string;
  descriptionPreview?: string;
  countryName?: string;
  timezoneName?: string;
  fromLabel?: string;
  toLabel?: string;
  editPath?: string;
}

export interface EntitlementRecord extends NamedEntity {
  description?: string;
  descriptionPreview?: string;
  versions?: VersionInfo[];
  supportLevels?: LevelRecord[];
  editPath?: string;
}
