/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { Id } from "../app";

export type CustomFieldType = "TEXT" | "NUMBER" | "DATE" | "SELECT";

export interface CustomFieldView {
  id: Id;
  name: string;
  fieldType: CustomFieldType | string;
  required: boolean;
  active?: boolean;
  description?: string | null;
  sortOrder?: number;
  options?: string[];
  value?: string | null;
}

export interface CustomFieldSummary {
  id: Id;
  name: string;
  fieldType: CustomFieldType | string;
  required: boolean;
  active: boolean;
  sortOrder?: number;
  description?: string | null;
  options?: string[];
  editPath?: string;
  deactivatePath?: string;
}

export type CustomFieldDetail = CustomFieldSummary;

export interface CustomFieldListResponse {
  canCreate?: boolean;
  createPath?: string;
  items: CustomFieldSummary[];
}

export interface CustomFieldBootstrap {
  canEdit?: boolean;
  fieldTypes?: string[];
}
