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
import { useToast } from "../components/common/ToastProvider";
import DataState from "../components/common/DataState";
import { UserLogoPreview } from "../components/users/UserProfileSections";
import useJson from "../hooks/useJson";
import type { SessionPageProps } from "../types/app";
import type { ProfileRecord } from "../types/domain";

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
}

export default function ProfilePage({ sessionState }: SessionPageProps) {
  const location = useLocation();
  const { showToast } = useToast();
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
      });
    }
  }, [profile]);

  useEffect(() => {
    if (routeError) {
      showToast({ variant: "error", message: routeError });
    }
  }, [routeError, showToast]);

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
            }
          : current,
      );
      setSaveState({ saving: false, error: "", saved: true });
      showToast({ variant: "success", message: "Profile saved successfully." });
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
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Profile</h2>
        </div>
      </div>

      <DataState
        state={profileState}
        emptyMessage="Profile unavailable."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {formState && profile && (
          <div className="article-detail">
            <div className="form-card ticket-detail-card">
              <form className="owner-form" onSubmit={submit}>
                <div className="owner-form-grid ticket-detail-grid">
                  <label>
                    Username
                    <input
                      value={formState.name}
                      onChange={(event) =>
                        updateField("name", event.target.value)
                      }
                      required
                    />
                  </label>
                  <label>
                    Full name
                    <input
                      value={formState.fullName}
                      onChange={(event) =>
                        updateField("fullName", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    Email
                    <input
                      type="email"
                      value={formState.email}
                      onChange={(event) =>
                        updateField("email", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    Social
                    <input
                      value={formState.social}
                      onChange={(event) =>
                        updateField("social", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    Phone number
                    <input
                      value={formState.phoneNumber}
                      onChange={(event) =>
                        updateField("phoneNumber", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    Phone extension
                    <input
                      value={formState.phoneExtension}
                      onChange={(event) =>
                        updateField("phoneExtension", event.target.value)
                      }
                    />
                  </label>
                  <label>
                    Country
                    <select
                      value={formState.countryId}
                      onChange={(event) => {
                        const nextCountryId = event.target.value;
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
                    >
                      <option value="">Select a country</option>
                      {profile.countries.map((country) => (
                        <option key={country.id} value={country.id}>
                          {country.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Time zone
                    <select
                      value={formState.timezoneId}
                      onChange={(event) =>
                        updateField("timezoneId", event.target.value)
                      }
                    >
                      <option value="">Select a time zone</option>
                      {availableTimezones.map((timezone) => (
                        <option key={timezone.id} value={timezone.id}>
                          {timezone.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  {profile.canSelectCompany ? (
                    <label>
                      Company
                      <select
                        value={formState.companyId}
                        onChange={(event) =>
                          updateField("companyId", event.target.value)
                        }
                      >
                        <option value="">Select a company</option>
                        {profile.companies.map((company) => (
                          <option key={company.id} value={company.id}>
                            {company.name}
                          </option>
                        ))}
                      </select>
                    </label>
                  ) : (
                    <label className="ticket-detail-spacer" aria-hidden="true">
                      <input value="-" readOnly />
                    </label>
                  )}
                  <div className="owner-detail-panel">
                    <div className="owner-detail-panel-label">Logo</div>
                    <div className="owner-detail-panel-body profile-logo-panel">
                      <div className="profile-logo-panel-content">
                        <UserLogoPreview
                          logoBase64={formState.logoBase64}
                          fullName={formState.fullName}
                          username={formState.name}
                          email={formState.email}
                        />
                        <input
                          ref={logoInputRef}
                          type="file"
                          accept="image/*"
                          className="hidden-file-input"
                          onChange={uploadLogo}
                        />
                        <button
                          type="button"
                          className="primary-button profile-logo-upload-button"
                          onClick={openLogoPicker}
                        >
                          Upload
                        </button>
                      </div>
                    </div>
                  </div>
                  <div className="detail-card-spacer" aria-hidden="true" />
                </div>

                <div className="button-row button-row-end">
                  <button
                    type="submit"
                    className="primary-button"
                    disabled={saveState.saving}
                  >
                    {saveState.saving ? "Saving..." : "Save"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </DataState>
    </section>
  );
}
