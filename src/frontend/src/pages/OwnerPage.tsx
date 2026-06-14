/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ChangeEvent, FormEvent } from "react";
import { useEffect, useRef, useState } from "react";
import useJson from "../hooks/useJson";
import DataState from "../components/common/DataState";
import { SmartLink } from "../utils/routing";
import {
  OwnerUserList,
  OwnerSelector,
  UserRoleBadge,
} from "../components/users/UserComponents";
import { UserLogoPreview } from "../components/users/UserProfileSections";
import type { Id, SessionPageProps } from "../types/app";
import type { OwnerCompany } from "../types/domain";
import PageHeader from "../components/layout/PageHeader";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { Switch } from "../components/ui/switch";
import { PhoneInput } from "../components/ui/aevr/phone-input";
import { CountryDropdown } from "../components/ui/aevr/country-dropdown";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { countries } from "country-data-list";
import {
  ownerInstallationBranding,
  writeCachedInstallationBranding,
} from "../utils/installationBranding";

const DEFAULT_INSTALLATION_COLOR = "#b00020";
const BRANDING_COLOR_COLUMNS = [
  { key: "headerFooterColor", label: "Header/Footer" },
  { key: "headersColor", label: "Headers" },
  { key: "buttonsColor", label: "Buttons" },
] as const;

type BrandingColorKey = (typeof BRANDING_COLOR_COLUMNS)[number]["key"];

interface OwnerFormState {
  name: string;
  address1: string;
  address2: string;
  city: string;
  state: string;
  zip: string;
  phoneNumber: string;
  countryId: string;
  timezoneId: string;
  logoBase64: string;
  backgroundBase64: string;
  headerFooterColor: string;
  headersColor: string;
  buttonsColor: string;
  use24HourClock: boolean;
  ticketAutoCloseDays: number;
  adminRoleIcon: string;
  supportRoleIcon: string;
  superuserRoleIcon: string;
  tamRoleIcon: string;
  userRoleIcon: string;
  externalRoleIcon: string;
  adminRoleColor: string;
  supportRoleColor: string;
  superuserRoleColor: string;
  tamRoleColor: string;
  userRoleColor: string;
  externalRoleColor: string;
  supportIds: Array<string | number>;
  tamIds: Array<string | number>;
}

function ClockFormatField({
  checked,
  onCheckedChange,
  disabled = false,
}: {
  checked: boolean;
  onCheckedChange?: (checked: boolean) => void;
  disabled?: boolean;
}) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-xl border border-border px-4 py-3">
      <div className="space-y-1">
        <div className="text-sm font-semibold text-[var(--color-section-header)]">
          24-hour clock
        </div>
        <p className="text-sm text-muted-foreground">
          Off shows <span className="font-medium text-foreground">2:43pm</span>.
          On shows <span className="font-medium text-foreground">14:43</span>.
        </p>
      </div>
      <Switch
        checked={checked}
        onCheckedChange={onCheckedChange}
        disabled={disabled}
        aria-label="Use 24-hour clock"
      />
    </div>
  );
}

function AutoCloseDaysField({
  value,
  onChange,
  disabled = false,
}: {
  value: number;
  onChange?: (value: number) => void;
  disabled?: boolean;
}) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-xl border border-border px-4 py-3">
      <div className="space-y-1">
        <div className="text-sm font-semibold text-[var(--color-section-header)]">
          Auto-close unrated tickets
        </div>
        <p className="text-sm text-muted-foreground">
          {value === 0 ? (
            "Disabled — resolved tickets will not be auto-closed."
          ) : (
            <>
              Resolved tickets without a rating are closed after{" "}
              <span className="font-medium text-foreground">
                {value} {value === 1 ? "day" : "days"}
              </span>
              .
            </>
          )}
        </p>
      </div>
      {!disabled && onChange && (
        <Input
          type="number"
          min={0}
          max={365}
          value={value}
          onChange={(e) => {
            const parsed = parseInt(e.target.value, 10);
            if (!isNaN(parsed)) {
              onChange(Math.max(0, Math.min(365, parsed)));
            }
          }}
          className="w-20 text-center"
          aria-label="Auto-close days"
        />
      )}
      {disabled && (
        <span className="text-sm font-medium tabular-nums">
          {value === 0 ? "Off" : `${value} days`}
        </span>
      )}
    </div>
  );
}

function BrandingColorTable({
  colors,
}: {
  colors: Record<BrandingColorKey, string | undefined>;
}) {
  return (
    <div className="overflow-hidden rounded-xl border border-border">
      <table className="w-full table-fixed">
        <thead className="bg-muted/40">
          <tr>
            {BRANDING_COLOR_COLUMNS.map((column) => (
              <th
                key={column.key}
                className="px-4 py-3 text-left text-sm font-semibold text-[var(--color-section-header)]"
              >
                {column.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          <tr>
            {BRANDING_COLOR_COLUMNS.map((column) => (
              <td key={column.key} className="px-4 py-4">
                <div className="flex justify-start">
                  <div
                    aria-label={column.label}
                    className="h-11 w-24 rounded-md border border-border"
                    style={{
                      backgroundColor:
                        colors[column.key] || DEFAULT_INSTALLATION_COLOR,
                    }}
                  />
                </div>
              </td>
            ))}
          </tr>
        </tbody>
      </table>
    </div>
  );
}

function BrandingColorPickerTable({
  colors,
  onChange,
}: {
  colors: Record<BrandingColorKey, string>;
  onChange: (field: BrandingColorKey, value: string) => void;
}) {
  return (
    <div className="overflow-hidden rounded-xl border border-border">
      <table className="w-full table-fixed">
        <thead className="bg-muted/40">
          <tr>
            {BRANDING_COLOR_COLUMNS.map((column) => (
              <th
                key={column.key}
                className="px-4 py-3 text-left text-sm font-semibold text-[var(--color-section-header)]"
              >
                {column.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          <tr>
            {BRANDING_COLOR_COLUMNS.map((column) => (
              <td key={column.key} className="px-4 py-4">
                <div className="flex justify-start">
                  <Input
                    type="color"
                    value={colors[column.key] || DEFAULT_INSTALLATION_COLOR}
                    onChange={(event) =>
                      onChange(column.key, event.target.value)
                    }
                    className="h-11 w-24 cursor-pointer rounded-md p-1"
                    aria-label={column.label}
                  />
                </div>
              </td>
            ))}
          </tr>
        </tbody>
      </table>
    </div>
  );
}

function RoleIconsDisplay({
  icons,
  colors,
}: {
  icons: Record<string, string>;
  colors: Record<string, string>;
}) {
  const keys = ["admin", "support", "tam", "superuser", "user", "external"];
  const labels: Record<string, string> = {
    admin: "Admin",
    support: "Support",
    tam: "TAM",
    superuser: "Superuser",
    user: "User",
    external: "External",
  };
  return (
    <div className="grid grid-cols-2 md:grid-cols-6 gap-4">
      {keys.map((key) => (
        <div
          key={key}
          className="flex items-center gap-2 p-3 border rounded-xl bg-muted/10"
        >
          <i
            className={`ti ti-${icons[key]} text-2xl`}
            style={{ color: colors[key] || "var(--color-primary)" }}
          />
          <div className="flex flex-col min-w-0">
            <span className="text-xs text-muted-foreground">{labels[key]}</span>
            <span className="text-sm font-medium truncate" title={icons[key]}>
              {icons[key]}
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}

const COMMON_TABLER_ICONS = [
  "shield-check",
  "headset",
  "briefcase",
  "crown",
  "user",
  "user-star",
  "star",
  "users",
  "user-check",
  "user-cog",
  "user-exclamation",
  "user-plus",
  "building",
  "id-badge",
  "tie",
  "tool",
  "settings",
  "lifebuoy",
  "help",
  "rocket",
  "alien",
  "robot",
  "ghost",
  "spy",
  "code",
  "brand-github",
];

function RoleIconsPicker({
  icons,
  colors,
  onChange,
}: {
  icons: Record<string, string>;
  colors: Record<string, string>;
  onChange: (field: keyof OwnerFormState, value: string) => void;
}) {
  const keys = ["admin", "support", "tam", "superuser", "user", "external"];
  const labels: Record<string, string> = {
    admin: "Admin",
    support: "Support",
    tam: "TAM",
    superuser: "Superuser",
    user: "User",
    external: "External",
  };
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {keys.map((key) => (
        <div key={key} className="flex items-center gap-3">
          <div className="h-10 w-10 shrink-0 border rounded-md flex items-center justify-center bg-muted/20">
            <i
              className={`ti ti-${icons[key]} text-xl`}
              style={{ color: colors[key] || "var(--color-primary)" }}
            />
          </div>
          <div className="flex-1 space-y-1 min-w-0">
            <span className="text-xs text-muted-foreground">{labels[key]}</span>
            <div className="flex items-center gap-2">
              <Select
                value={icons[key]}
                onValueChange={(val) =>
                  onChange(`${key}RoleIcon` as keyof OwnerFormState, val)
                }
              >
                <SelectTrigger className="flex-1 min-w-0">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent position="popper">
                  {COMMON_TABLER_ICONS.map((i) => (
                    <SelectItem key={i} value={i}>
                      <div className="flex items-center gap-2">
                        <i className={`ti ti-${i}`} />
                        <span>{i}</span>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Input
                type="color"
                value={colors[key] || "#000000"}
                onChange={(e) =>
                  onChange(
                    `${key}RoleColor` as keyof OwnerFormState,
                    e.target.value,
                  )
                }
                className="h-10 w-12 cursor-pointer rounded-md p-1 shrink-0"
                title={`${labels[key]} Color`}
              />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export function OwnerPage(props: SessionPageProps) {
  void props;
  const ownerState = useJson<OwnerCompany>("/api/owner");
  const owner = ownerState.data;

  return (
    <section className="w-full mt-4">
      <DataState state={ownerState} emptyMessage="Owner company not found.">
        {owner && (
          <div className="space-y-6 pb-20">
            <PageHeader title={owner.name || "Owner"} />
            <div className="grid gap-6 md:grid-cols-2">
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Name
                </FieldLabel>
                <Input value={owner.name || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Phone
                </FieldLabel>
                <Input value={owner.phoneNumber || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Country
                </FieldLabel>
                <Input value={owner.countryName || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Time zone
                </FieldLabel>
                <Input value={owner.timezoneName || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Address 1
                </FieldLabel>
                <Input value={owner.address1 || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Address 2
                </FieldLabel>
                <Input value={owner.address2 || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  City
                </FieldLabel>
                <Input value={owner.city || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  State
                </FieldLabel>
                <Input value={owner.state || "—"} readOnly />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Zip
                </FieldLabel>
                <Input value={owner.zip || "—"} readOnly />
              </Field>

              <div className="md:col-span-2 grid gap-6 md:grid-cols-2 pt-6 border-t mt-2">
                <div className="space-y-4">
                  <FieldLabel className="mb-4 text-base text-[var(--color-header-bg)]">
                    Support
                  </FieldLabel>
                  <OwnerUserList users={owner.supportUsers} />
                </div>
                <div className="space-y-4">
                  <FieldLabel className="mb-4 text-base text-[var(--color-header-bg)]">
                    TAMs
                  </FieldLabel>
                  <OwnerUserList users={owner.tamUsers} />
                </div>
              </div>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Logo
                </FieldLabel>
                <div className="flex items-center gap-4">
                  {owner.logoBase64 ? (
                    <img
                      src={owner.logoBase64}
                      alt={`${owner.name || "Installation"} logo`}
                      className="h-12 w-12 rounded-md object-contain"
                    />
                  ) : (
                    <span className="text-sm text-muted-foreground">None</span>
                  )}
                </div>
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Background
                </FieldLabel>
                {owner.backgroundBase64 ? (
                  <div
                    className="h-40 w-full rounded-xl border border-border bg-center bg-cover bg-no-repeat"
                    style={{
                      backgroundImage: `url(${owner.backgroundBase64})`,
                    }}
                  />
                ) : (
                  <span className="text-sm text-muted-foreground">None</span>
                )}
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Clock
                </FieldLabel>
                <ClockFormatField
                  checked={Boolean(owner.use24HourClock)}
                  disabled
                />
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Ticket auto-close
                </FieldLabel>
                <AutoCloseDaysField
                  value={owner.ticketAutoCloseDays ?? 7}
                  disabled
                />
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel>Colors</FieldLabel>
                <BrandingColorTable
                  colors={{
                    headerFooterColor: owner.headerFooterColor,
                    headersColor: owner.headersColor,
                    buttonsColor: owner.buttonsColor,
                  }}
                />
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Role Badges (Tabler Icons)
                </FieldLabel>
                <RoleIconsDisplay
                  icons={{
                    admin: owner.adminRoleIcon || "shield-check",
                    support: owner.supportRoleIcon || "headset",
                    superuser: owner.superuserRoleIcon || "crown",
                    tam: owner.tamRoleIcon || "briefcase",
                    user: owner.userRoleIcon || "user",
                    external: owner.externalRoleIcon || "user-star",
                  }}
                  colors={{
                    admin: owner.adminRoleColor || "",
                    support: owner.supportRoleColor || "",
                    superuser: owner.superuserRoleColor || "",
                    tam: owner.tamRoleColor || "",
                    user: owner.userRoleColor || "",
                    external: owner.externalRoleColor || "",
                  }}
                />
              </Field>
            </div>

            <div className="flex items-center justify-end space-x-3 pt-4">
              <Button asChild>
                <SmartLink href="/owner/edit">Edit</SmartLink>
              </Button>
            </div>
          </div>
        )}
      </DataState>
    </section>
  );
}

export function OwnerEditPage(props: SessionPageProps) {
  void props;
  const ownerState = useJson<OwnerCompany>("/api/owner");
  const owner = ownerState.data;
  const [formState, setFormState] = useState<OwnerFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const logoInputRef = useRef<HTMLInputElement | null>(null);
  const backgroundInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (owner) {
      setFormState({
        name: owner.name || "",
        address1: owner.address1 || "",
        address2: owner.address2 || "",
        city: owner.city || "",
        state: owner.state || "",
        zip: owner.zip || "",
        phoneNumber: owner.phoneNumber || "",
        countryId: owner.countryId ? String(owner.countryId) : "",
        timezoneId: owner.timezoneId ? String(owner.timezoneId) : "",
        logoBase64: owner.logoBase64 || "",
        backgroundBase64: owner.backgroundBase64 || "",
        headerFooterColor:
          owner.headerFooterColor || DEFAULT_INSTALLATION_COLOR,
        headersColor: owner.headersColor || DEFAULT_INSTALLATION_COLOR,
        buttonsColor: owner.buttonsColor || DEFAULT_INSTALLATION_COLOR,
        use24HourClock: Boolean(owner.use24HourClock),
        ticketAutoCloseDays: owner.ticketAutoCloseDays ?? 7,
        adminRoleIcon: owner.adminRoleIcon || "shield-check",
        supportRoleIcon: owner.supportRoleIcon || "headset",
        superuserRoleIcon: owner.superuserRoleIcon || "crown",
        tamRoleIcon: owner.tamRoleIcon || "briefcase",
        userRoleIcon: owner.userRoleIcon || "user",
        externalRoleIcon: owner.externalRoleIcon || "user-star",
        adminRoleColor: owner.adminRoleColor || "",
        supportRoleColor: owner.supportRoleColor || "",
        superuserRoleColor: owner.superuserRoleColor || "",
        tamRoleColor: owner.tamRoleColor || "",
        userRoleColor: owner.userRoleColor || "",
        externalRoleColor: owner.externalRoleColor || "",
        supportIds: owner.supportUsers.map((u) => u.id).filter(Boolean) as Id[],
        tamIds: owner.tamUsers.map((user) => user.id),
      });
    }
  }, [owner]);

  const selectedCountryId = formState?.countryId || "";
  const availableTimezones =
    owner?.timezones?.filter(
      (timezone) =>
        selectedCountryId && String(timezone.countryId) === selectedCountryId,
    ) || [];

  const updateField = (field: keyof OwnerFormState, value: string) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  const updateToggleField = (field: "use24HourClock", value: boolean) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  const updateNumericField = (field: "ticketAutoCloseDays", value: number) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  const openLogoPicker = () => {
    logoInputRef.current?.click();
  };

  const openBackgroundPicker = () => {
    backgroundInputRef.current?.click();
  };

  const uploadImage =
    (field: "logoBase64" | "backgroundBase64", label: string) =>
    (event: ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];
      event.target.value = "";
      if (!file) {
        return;
      }
      if (!file.type.startsWith("image/")) {
        setSaveState({
          saving: false,
          error: `${label} must be an image file.`,
        });
        return;
      }
      const reader = new FileReader();
      reader.onload = (loadEvent) => {
        const result = loadEvent.target?.result;
        if (typeof result !== "string") {
          setSaveState({
            saving: false,
            error: `Unable to read ${label.toLowerCase()} file.`,
          });
          return;
        }
        updateField(field, result);
      };
      reader.onerror = () => {
        setSaveState({
          saving: false,
          error: `Unable to read ${label.toLowerCase()} file.`,
        });
      };
      reader.readAsDataURL(file);
    };

  const uploadLogo = uploadImage("logoBase64", "Logo");

  const uploadBackground = uploadImage("backgroundBase64", "Background");

  const clearBackground = () => {
    updateField("backgroundBase64", "");
  };

  const toggleSelectedUser = (
    field: "supportIds" | "tamIds",
    userId: string | number,
  ) => {
    setFormState((current) =>
      current
        ? {
            ...current,
            [field]: current[field].includes(userId)
              ? current[field].filter((existing) => existing !== userId)
              : [...current[field], userId],
          }
        : current,
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState) {
      return;
    }

    setSaveState({ saving: true, error: "" });
    try {
      const response = await fetch("/api/owner", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          ...formState,
          use24HourClock: formState.use24HourClock,
          ticketAutoCloseDays: formState.ticketAutoCloseDays,
          adminRoleIcon: formState.adminRoleIcon,
          supportRoleIcon: formState.supportRoleIcon,
          superuserRoleIcon: formState.superuserRoleIcon,
          tamRoleIcon: formState.tamRoleIcon,
          userRoleIcon: formState.userRoleIcon,
          externalRoleIcon: formState.externalRoleIcon,
          adminRoleColor: formState.adminRoleColor,
          supportRoleColor: formState.supportRoleColor,
          superuserRoleColor: formState.superuserRoleColor,
          tamRoleColor: formState.tamRoleColor,
          userRoleColor: formState.userRoleColor,
          externalRoleColor: formState.externalRoleColor,
          countryId: formState.countryId ? Number(formState.countryId) : null,
          timezoneId: formState.timezoneId
            ? Number(formState.timezoneId)
            : null,
        }),
      });

      if (response.status === 401) {
        throw new Error("You need to sign in again.");
      }

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Unable to save owner details.");
      }

      const updatedOwner = (await response.json()) as OwnerCompany;
      writeCachedInstallationBranding(ownerInstallationBranding(updatedOwner));
      window.location.assign("/owner");
      return;
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error
            ? error.message
            : "Unable to save owner details.",
      });
      return;
    }
  };

  return (
    <section className="w-full mt-4">
      <DataState state={ownerState} emptyMessage="Owner company not found.">
        {formState && owner && (
          <form className="space-y-6 pb-20" onSubmit={submit}>
            <PageHeader title={formState.name || owner.name || "Owner"} />
            <div className="grid gap-6 md:grid-cols-2">
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Name
                </FieldLabel>
                <Input
                  value={formState.name}
                  onChange={(event) => updateField("name", event.target.value)}
                  required
                />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Phone
                </FieldLabel>
                <PhoneInput
                  defaultCountry="US"
                  value={formState.phoneNumber}
                  onChange={(value) => updateField("phoneNumber", value || "")}
                />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Country
                </FieldLabel>
                <CountryDropdown
                  defaultValue={
                    countries.all.find(
                      (c) =>
                        c.name ===
                        owner.countries.find(
                          (oc) => String(oc.id) === formState.countryId,
                        )?.name,
                    )?.alpha2 || ""
                  }
                  onChange={(country) => {
                    const matched = owner.countries.find(
                      (c) => c.name === country.name,
                    );
                    const nextCountryId = matched ? String(matched.id) : "";
                    const timezoneStillValid = owner.timezones.some(
                      (timezone) =>
                        String(timezone.id) === formState.timezoneId &&
                        String(timezone.countryId) === nextCountryId,
                    );
                    setFormState((current) =>
                      current
                        ? {
                            ...current,
                            countryId: nextCountryId,
                            timezoneId: timezoneStillValid
                              ? current.timezoneId
                              : "",
                          }
                        : current,
                    );
                  }}
                />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Time zone
                </FieldLabel>
                <Select
                  value={formState.timezoneId || undefined}
                  onValueChange={(value) => updateField("timezoneId", value)}
                >
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Select a time zone" />
                  </SelectTrigger>
                  <SelectContent>
                    {availableTimezones.map((timezone) => (
                      <SelectItem key={timezone.id} value={String(timezone.id)}>
                        {timezone.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Address 1
                </FieldLabel>
                <Input
                  value={formState.address1}
                  onChange={(event) =>
                    updateField("address1", event.target.value)
                  }
                />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Address 2
                </FieldLabel>
                <Input
                  value={formState.address2}
                  onChange={(event) =>
                    updateField("address2", event.target.value)
                  }
                />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  City
                </FieldLabel>
                <Input
                  value={formState.city}
                  onChange={(event) => updateField("city", event.target.value)}
                />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  State
                </FieldLabel>
                <Input
                  value={formState.state}
                  onChange={(event) => updateField("state", event.target.value)}
                />
              </Field>
              <Field>
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Zip
                </FieldLabel>
                <Input
                  value={formState.zip}
                  onChange={(event) => updateField("zip", event.target.value)}
                />
              </Field>

              <div className="md:col-span-2 grid gap-6 md:grid-cols-2 pt-6 border-t mt-2">
                <div className="space-y-4">
                  <FieldLabel className="mb-4 text-base text-[var(--color-header-bg)] flex items-center gap-2">
                    <UserRoleBadge type="support" className="text-xl" />
                    Support
                  </FieldLabel>
                  <div className="mt-2">
                    <OwnerSelector
                      title=""
                      users={owner.supportOptions}
                      selectedIds={formState.supportIds}
                      onToggle={(userId) =>
                        toggleSelectedUser("supportIds", userId)
                      }
                    />
                  </div>
                </div>
                <div className="space-y-4">
                  <FieldLabel className="mb-4 text-base text-[var(--color-header-bg)] flex items-center gap-2">
                    <UserRoleBadge type="tam" className="text-xl" />
                    TAMs
                  </FieldLabel>
                  <div className="mt-2">
                    <OwnerSelector
                      title=""
                      users={owner.tamOptions}
                      selectedIds={formState.tamIds}
                      onToggle={(userId) =>
                        toggleSelectedUser("tamIds", userId)
                      }
                    />
                  </div>
                </div>
              </div>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Logo
                </FieldLabel>
                <div className="flex items-center gap-6">
                  <UserLogoPreview
                    logoBase64={formState.logoBase64}
                    fullName={formState.name}
                    username={formState.name}
                    email={formState.name}
                  />
                  <div>
                    <input
                      ref={logoInputRef}
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={uploadLogo}
                    />
                    <Button
                      type="button"
                      variant="outline"
                      onClick={openLogoPicker}
                    >
                      Upload
                    </Button>
                  </div>
                </div>
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Background
                </FieldLabel>
                <div className="space-y-4">
                  {formState.backgroundBase64 ? (
                    <div
                      className="h-48 w-full rounded-xl border border-border bg-center bg-cover bg-no-repeat"
                      style={{
                        backgroundImage: `url(${formState.backgroundBase64})`,
                      }}
                    />
                  ) : (
                    <div className="flex h-24 items-center justify-center rounded-xl border border-dashed border-border text-sm text-muted-foreground">
                      None
                    </div>
                  )}
                  <div className="flex items-center gap-3">
                    <input
                      ref={backgroundInputRef}
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={uploadBackground}
                    />
                    <Button
                      type="button"
                      variant="outline"
                      onClick={openBackgroundPicker}
                    >
                      Upload
                    </Button>
                    {formState.backgroundBase64 ? (
                      <Button
                        type="button"
                        variant="outline"
                        onClick={clearBackground}
                      >
                        Remove
                      </Button>
                    ) : null}
                  </div>
                </div>
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Clock
                </FieldLabel>
                <ClockFormatField
                  checked={formState.use24HourClock}
                  onCheckedChange={(checked) =>
                    updateToggleField("use24HourClock", checked)
                  }
                />
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Ticket auto-close
                </FieldLabel>
                <AutoCloseDaysField
                  value={formState.ticketAutoCloseDays}
                  onChange={(value) =>
                    updateNumericField("ticketAutoCloseDays", value)
                  }
                />
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel>Colors</FieldLabel>
                <BrandingColorPickerTable
                  colors={{
                    headerFooterColor: formState.headerFooterColor,
                    headersColor: formState.headersColor,
                    buttonsColor: formState.buttonsColor,
                  }}
                  onChange={updateField}
                />
              </Field>
              <Field className="md:col-span-2">
                <FieldLabel className="text-[var(--color-header-bg)]">
                  Role Badges (Tabler Icons)
                </FieldLabel>
                <RoleIconsPicker
                  icons={{
                    admin: formState.adminRoleIcon,
                    support: formState.supportRoleIcon,
                    tam: formState.tamRoleIcon,
                    superuser: formState.superuserRoleIcon,
                    user: formState.userRoleIcon,
                    external: formState.externalRoleIcon,
                  }}
                  colors={{
                    admin: formState.adminRoleColor,
                    support: formState.supportRoleColor,
                    tam: formState.tamRoleColor,
                    superuser: formState.superuserRoleColor,
                    user: formState.userRoleColor,
                    external: formState.externalRoleColor,
                  }}
                  onChange={updateField}
                />
              </Field>
            </div>

            {saveState.error && (
              <p className="text-sm font-medium text-destructive">
                {saveState.error}
              </p>
            )}

            <div className="flex items-center justify-end space-x-3 pt-4">
              <Button type="submit" disabled={saveState.saving}>
                {saveState.saving ? "Saving..." : "Save"}
              </Button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}

export default OwnerPage;
