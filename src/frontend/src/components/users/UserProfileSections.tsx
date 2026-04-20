/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactNode } from "react";
import { formatPhone, profileInitial } from "../../utils/formatting";
import { SmartLink } from "../../utils/routing";
import { Field, FieldLabel } from "../ui/field";
import { Input } from "../ui/input";

interface UserLogoPreviewProps {
  logoBase64?: string;
  fullName?: string;
  username?: string;
  email?: string;
}

interface UserDetailCardUser {
  username?: string;
  fullName?: string;
  email?: string;
  social?: string;
  phoneNumber?: string;
  phoneExtension?: string;
  type?: string;
  typeLabel?: string;
  countryName?: string;
  timezoneName?: string;
  companyName?: string;
  logoBase64?: string;
}

interface UserDetailCardProps {
  user: UserDetailCardUser;
  companyHref?: string;
  companyLabel?: string;
  actions?: ReactNode;
}

export function UserLogoPreview({
  logoBase64,
  fullName,
  username,
  email,
}: UserLogoPreviewProps) {
  const initial = profileInitial(fullName, username, email);

  return (
    <div className="flex h-20 w-20 shrink-0 overflow-hidden rounded-full border border-border bg-muted items-center justify-center">
      {logoBase64 ? (
        <img
          className="aspect-square h-full w-full object-cover"
          src={logoBase64}
          alt="Profile logo"
        />
      ) : (
        <span
          className="text-xl font-medium text-muted-foreground"
          aria-label="Profile initial"
        >
          {initial}
        </span>
      )}
    </div>
  );
}

export function UserDetailCard({
  user,
  companyHref,
  companyLabel = "Company",
  actions,
}: UserDetailCardProps) {
  const hasCompany = user.companyName || companyHref;

  return (
    <div className="w-full space-y-6">
      <div className="grid gap-6 md:grid-cols-2">
        <Field>
          <FieldLabel>Username</FieldLabel>
          <Input value={user.username || "—"} readOnly />
        </Field>
        <Field>
          <FieldLabel>Full name</FieldLabel>
          <Input value={user.fullName || "—"} readOnly />
        </Field>
        <Field>
          <FieldLabel>Type</FieldLabel>
          <Input value={user.typeLabel || user.type || "User"} readOnly />
        </Field>
        <Field>
          <FieldLabel>Email</FieldLabel>
          <Input value={user.email || "—"} readOnly />
        </Field>
        <Field>
          <FieldLabel>Social</FieldLabel>
          <Input value={user.social || "—"} readOnly />
        </Field>
        <Field>
          <FieldLabel>Phone</FieldLabel>
          <Input
            value={formatPhone(user.phoneNumber, user.phoneExtension)}
            readOnly
          />
        </Field>
        <Field>
          <FieldLabel>Country</FieldLabel>
          <Input value={user.countryName || "—"} readOnly />
        </Field>
        <Field>
          <FieldLabel>Time zone</FieldLabel>
          <Input value={user.timezoneName || "—"} readOnly />
        </Field>

        <div className="md:col-span-2 pt-4 border-t mt-2 grid gap-6 md:grid-cols-2">
          <div>
            <FieldLabel className="mb-2 block">{companyLabel}</FieldLabel>
            {companyHref ? (
              <SmartLink
                className="text-primary hover:underline font-medium flex items-center h-10"
                href={companyHref}
              >
                {user.companyName || "Open company"}
              </SmartLink>
            ) : (
              <div className="flex h-10 items-center">
                <span className="text-sm font-medium">
                  {hasCompany ? user.companyName : "—"}
                </span>
              </div>
            )}
          </div>

          <div>
            <FieldLabel className="mb-2 block">Logo</FieldLabel>
            <UserLogoPreview
              logoBase64={user.logoBase64}
              fullName={user.fullName}
              username={user.username}
              email={user.email}
            />
          </div>
        </div>
      </div>

      {actions ? <div className="flex items-center pt-4">{actions}</div> : null}
    </div>
  );
}
