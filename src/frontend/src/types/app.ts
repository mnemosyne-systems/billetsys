/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactElement, ReactNode } from "react";

export type Id = number | string;

export type Role =
  | "admin"
  | "support"
  | "superuser"
  | "tam"
  | "user"
  | (string & {});

export interface SessionNavigationLink {
  href: string;
  label: string;
  alarm?: boolean;
  [key: string]: unknown;
}

export interface Session {
  authenticated?: boolean;
  role?: Role;
  homePath?: string;
  installationCompanyName?: string;
  displayName?: string;
  username?: string;
  logoBase64?: string;
  navigation?: SessionNavigationLink[];
  [key: string]: unknown;
}

export interface AsyncState<T> {
  loading: boolean;
  error: string;
  unauthorized: boolean;
  forbidden: boolean;
  empty: boolean;
  data: T | null;
}

export type SessionState = AsyncState<Session>;

export interface AppRoute {
  path: string;
  element: ReactElement;
  requiresAuth?: boolean;
  allowedRoles?: Role[];
}

export interface SessionPageProps {
  sessionState: SessionState;
}

export interface ChildrenProps {
  children?: ReactNode;
}

export interface StatusPageProps extends SessionPageProps {
  title: string;
  message: string;
}

export type FormMode = "create" | "edit";
