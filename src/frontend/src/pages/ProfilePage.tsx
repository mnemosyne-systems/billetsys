/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ChangeEvent, FormEvent } from "react";
import { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { toast } from "sonner";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import { UserLogoPreview } from "../components/users/UserProfileSections";
import useJson from "../hooks/useJson";
import type { SessionPageProps } from "../types/app";
import type { ProfileRecord } from "../types/domain";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { PhoneInput } from "../components/ui/aevr/phone-input";
import { CountryDropdown } from "../components/ui/aevr/country-dropdown";
import { Field, FieldLabel } from "../components/ui/field";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { countries } from "country-data-list";

interface ProfileFormState {
  name: string;
  email: string;
  fullName: string;
  social: string;
  phoneNumber: string;
  phoneExtension: string;
  countryId: string;
  timezoneId: string;
  companyId: string;
  logoBase64: string;
  emailFormat: string;
}

export default function ProfilePage(props: SessionPageProps) {
  void props;
  const location = useLocation();

  const profileState = useJson<ProfileRecord>("/api/profile");
  const profile = profileState.data;
  const [formState, setFormState] = useState<ProfileFormState | null>(null);
  const [saveState, setSaveState] = useState({
    saving: false,
    error: "",
    saved: false,
  });
  const routeError = new URLSearchParams(location.search).get("error") || "";
  const logoInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (profile) {
      setFormState({
        name: profile.username || "",
        email: profile.email || "",
        fullName: profile.fullName || "",
        social: profile.social || "",
        phoneNumber: profile.phoneNumber || "",
        phoneExtension: profile.phoneExtension || "",
        countryId: profile.countryId ? String(profile.countryId) : "",
        timezoneId: profile.timezoneId ? String(profile.timezoneId) : "",
        companyId: profile.currentCompanyId
          ? String(profile.currentCompanyId)
          : "",
        logoBase64: profile.logoBase64 || "",
        emailFormat: profile.emailFormat || "",
      });
    }
  }, [profile]);

  useEffect(() => {
    if (routeError) {
      toast.error(routeError);
    }
  }, [routeError]);

  const availableTimezones =
    profile?.timezones?.filter(
      (timezone) =>
        !formState?.countryId ||
        String(timezone.countryId) === formState.countryId,
    ) || [];

  const updateField = (field: keyof ProfileFormState, value: string) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
    setSaveState((current) => ({ ...current, saved: false }));
  };

  const openLogoPicker = () => {
    logoInputRef.current?.click();
  };

  const uploadLogo = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) {
      return;
    }
    if (!file.type.startsWith("image/")) {
      setSaveState({
        saving: false,
        error: "Logo must be an image file.",
        saved: false,
      });
      return;
    }
    const reader = new FileReader();
    reader.onload = (loadEvent) => {
      const result = loadEvent.target?.result;
      if (typeof result !== "string") {
        setSaveState({
          saving: false,
          error: "Unable to read logo file.",
          saved: false,
        });
        return;
      }
      updateField("logoBase64", result);
    };
    reader.onerror = () => {
      setSaveState({
        saving: false,
        error: "Unable to read logo file.",
        saved: false,
      });
    };
    reader.readAsDataURL(file);
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState) {
      return;
    }
    setSaveState({ saving: true, error: "", saved: false });
    try {
      const response = await fetch("/api/profile", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          ...formState,
          name: formState.name,
          countryId: formState.countryId ? Number(formState.countryId) : null,
          timezoneId: formState.timezoneId
            ? Number(formState.timezoneId)
            : null,
          companyId: formState.companyId ? Number(formState.companyId) : null,
          emailFormat: formState.emailFormat || null,
        }),
      });
      if (!response.ok) {
        throw new Error((await response.text()) || "Unable to save profile.");
      }
      const updated = (await response.json()) as ProfileRecord;
      setFormState((current) =>
        current
          ? {
              ...current,
              name: updated.username || "",
              email: updated.email || "",
              fullName: updated.fullName || "",
              social: updated.social || "",
              phoneNumber: updated.phoneNumber || "",
              phoneExtension: updated.phoneExtension || "",
              countryId: updated.countryId ? String(updated.countryId) : "",
              timezoneId: updated.timezoneId ? String(updated.timezoneId) : "",
              companyId: updated.currentCompanyId
                ? String(updated.currentCompanyId)
                : "",
              logoBase64: updated.logoBase64 || "",
              emailFormat: updated.emailFormat || "",
            }
          : current,
      );
      setSaveState({ saving: false, error: "", saved: true });
      toast.success("Profile saved successfully.");
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to save profile.",
        saved: false,
      });
    }
  };

  return (
    <section className="w-full mt-4">
      <PageHeader title="Profile" />

      <DataState state={profileState} emptyMessage="Profile unavailable.">
        {formState && profile && (
          <form className="space-y-6 pb-20" onSubmit={submit}>
            <div className="grid gap-6 md:grid-cols-2">
              <Field>
                <FieldLabel>
                  Username <span className="text-destructive">*</span>
                </FieldLabel>
                <Input
                  value={formState.name}
                  onChange={(event) => updateField("name", event.target.value)}
                  readOnly={Boolean(profile.username)}
                />
              </Field>
              <Field>
                <FieldLabel>Full name</FieldLabel>
                <Input
                  value={formState.fullName}
                  onChange={(event) =>
                    updateField("fullName", event.target.value)
                  }
                />
              </Field>
              <Field>
                <FieldLabel>Email</FieldLabel>
                <Input
                  type="email"
                  value={formState.email}
                  onChange={(event) => updateField("email", event.target.value)}
                />
              </Field>
              <Field>
                <FieldLabel>Social</FieldLabel>
                <Input
                  value={formState.social}
                  onChange={(event) =>
                    updateField("social", event.target.value)
                  }
                />
              </Field>
              <Field>
                <FieldLabel>Phone number</FieldLabel>
                <PhoneInput
                  defaultCountry="US"
                  value={formState.phoneNumber}
                  onChange={(value) => updateField("phoneNumber", value || "")}
                />
              </Field>
              <Field>
                <FieldLabel>Phone extension</FieldLabel>
                <Input
                  value={formState.phoneExtension}
                  onChange={(event) =>
                    updateField("phoneExtension", event.target.value)
                  }
                />
              </Field>
              <Field>
                <FieldLabel>Country</FieldLabel>
                <CountryDropdown
                  defaultValue={
                    countries.all.find(
                      (c) =>
                        c.name ===
                        profile.countries.find(
                          (pc) => String(pc.id) === formState.countryId,
                        )?.name,
                    )?.alpha2 || ""
                  }
                  onChange={(country) => {
                    const matched = profile.countries.find(
                      (c) => c.name === country.name,
                    );
                    const nextCountryId = matched ? String(matched.id) : "";
                    const timezoneStillValid = profile.timezones.some(
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
                    setSaveState((current) => ({
                      ...current,
                      saved: false,
                    }));
                  }}
                />
              </Field>
              <Field>
                <FieldLabel>Time zone</FieldLabel>
                <Select
                  value={formState.timezoneId}
                  onValueChange={(value) =>
                    updateField("timezoneId", value === "none" ? "" : value)
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select a time zone" />
                  </SelectTrigger>
                  <SelectContent position="popper">
                    <SelectItem value="none">Select a time zone</SelectItem>
                    {availableTimezones.map((timezone) => (
                      <SelectItem key={timezone.id} value={String(timezone.id)}>
                        {timezone.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </Field>
              <Field>
                <FieldLabel>Email format</FieldLabel>
                <Select
                  value={formState.emailFormat || "both"}
                  onValueChange={(value) =>
                    updateField("emailFormat", value === "both" ? "" : value)
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select email format" />
                  </SelectTrigger>
                  <SelectContent position="popper">
                    <SelectItem value="both">HTML &amp; Plain text</SelectItem>
                    <SelectItem value="html">HTML only</SelectItem>
                    <SelectItem value="text">Plain text only</SelectItem>
                  </SelectContent>
                </Select>
              </Field>
              {profile.canSelectCompany && (
                <Field>
                  <FieldLabel>Company</FieldLabel>
                  <Select
                    value={formState.companyId}
                    onValueChange={(value) =>
                      updateField("companyId", value === "none" ? "" : value)
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select a company" />
                    </SelectTrigger>
                    <SelectContent position="popper">
                      <SelectItem value="none">Select a company</SelectItem>
                      {profile.companies.map((company) => (
                        <SelectItem key={company.id} value={String(company.id)}>
                          {company.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
              )}

              <div className="md:col-span-2 pt-4">
                <FieldLabel className="mb-2 block">Profile Logo</FieldLabel>
                <div className="flex items-center space-x-6">
                  <UserLogoPreview
                    logoBase64={formState.logoBase64}
                    fullName={formState.fullName}
                    username={formState.name}
                    email={formState.email}
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
              </div>
            </div>

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
