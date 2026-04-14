/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import useJson from "../hooks/useJson";
import DataState from "../components/common/DataState";
import { SmartLink } from "../utils/routing";
import {
  OwnerUserList,
  OwnerSelector,
} from "../components/users/UserComponents";
import type { SessionPageProps } from "../types/app";
import type { OwnerCompany } from "../types/domain";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "../components/ui/card";
import PageHeader from "../components/layout/PageHeader";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
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
  supportIds: Array<string | number>;
  tamIds: Array<string | number>;
}

export function OwnerPage(props: SessionPageProps) {
  void props;
  const ownerState = useJson<OwnerCompany>("/api/owner");
  const owner = ownerState.data;

  return (
    <section className="w-full mt-4">
      <PageHeader title="Owner" />
      <DataState state={ownerState} emptyMessage="Owner company not found.">
        {owner && (
          <div className="space-y-6 pb-20">
            <Card>
              <CardContent className="grid gap-6 md:grid-cols-2">
                <Field>
                  <FieldLabel>Name</FieldLabel>
                  <Input value={owner.name || "—"} readOnly />
                </Field>
                <Field>
                  <FieldLabel>Phone</FieldLabel>
                  <Input value={owner.phoneNumber || "—"} readOnly />
                </Field>
                <Field>
                  <FieldLabel>Country</FieldLabel>
                  <Input value={owner.countryName || "—"} readOnly />
                </Field>
                <Field>
                  <FieldLabel>Time zone</FieldLabel>
                  <Input value={owner.timezoneName || "—"} readOnly />
                </Field>
                <Field>
                  <FieldLabel>Address1</FieldLabel>
                  <Input value={owner.address1 || "—"} readOnly />
                </Field>
                <Field>
                  <FieldLabel>Address2</FieldLabel>
                  <Input value={owner.address2 || "—"} readOnly />
                </Field>
                <Field>
                  <FieldLabel>City</FieldLabel>
                  <Input value={owner.city || "—"} readOnly />
                </Field>
                <Field>
                  <FieldLabel>State</FieldLabel>
                  <Input value={owner.state || "—"} readOnly />
                </Field>
                <Field className="md:col-span-2">
                  <FieldLabel>Zip</FieldLabel>
                  <Input value={owner.zip || "—"} readOnly />
                </Field>

                <div className="md:col-span-2 grid gap-6 md:grid-cols-2 pt-6 border-t mt-2">
                  <div className="space-y-3">
                    <FieldLabel className="text-base">Support</FieldLabel>
                    <OwnerUserList users={owner.supportUsers} />
                  </div>
                  <div className="space-y-3">
                    <FieldLabel className="text-base">TAMs</FieldLabel>
                    <OwnerUserList users={owner.tamUsers} />
                  </div>
                </div>
              </CardContent>
            </Card>

            <div className="flex items-center justify-end space-x-3 pt-4 border-t">
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
  const navigate = useNavigate();
  const ownerState = useJson<OwnerCompany>("/api/owner");
  const owner = ownerState.data;
  const [formState, setFormState] = useState<OwnerFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });

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
        supportIds: owner.supportUsers.map((user) => user.id),
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

      navigate("/owner");
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

    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="w-full mt-4">
      <DataState state={ownerState} emptyMessage="Owner company not found.">
        {formState && owner && (
          <form className="space-y-6 pb-20" onSubmit={submit}>
            <PageHeader title="Edit Owner" />
            <Card>
              <CardHeader>
                <CardTitle>Edit Owner Details</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-6 md:grid-cols-2">
                <Field>
                  <FieldLabel>Name</FieldLabel>
                  <Input
                    value={formState.name}
                    onChange={(event) =>
                      updateField("name", event.target.value)
                    }
                    required
                  />
                </Field>
                <Field>
                  <FieldLabel>Phone</FieldLabel>
                  <PhoneInput
                    defaultCountry="US"
                    value={formState.phoneNumber}
                    onChange={(value) =>
                      updateField("phoneNumber", value || "")
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
                  <FieldLabel>Time zone</FieldLabel>
                  <Select
                    value={formState.timezoneId || undefined}
                    onValueChange={(value) => updateField("timezoneId", value)}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Select a time zone" />
                    </SelectTrigger>
                    <SelectContent>
                      {availableTimezones.map((timezone) => (
                        <SelectItem
                          key={timezone.id}
                          value={String(timezone.id)}
                        >
                          {timezone.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field>
                  <FieldLabel>Address 1</FieldLabel>
                  <Input
                    value={formState.address1}
                    onChange={(event) =>
                      updateField("address1", event.target.value)
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel>Address 2</FieldLabel>
                  <Input
                    value={formState.address2}
                    onChange={(event) =>
                      updateField("address2", event.target.value)
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel>City</FieldLabel>
                  <Input
                    value={formState.city}
                    onChange={(event) =>
                      updateField("city", event.target.value)
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel>State</FieldLabel>
                  <Input
                    value={formState.state}
                    onChange={(event) =>
                      updateField("state", event.target.value)
                    }
                  />
                </Field>
                <Field className="md:col-span-2">
                  <FieldLabel>Zip</FieldLabel>
                  <Input
                    value={formState.zip}
                    onChange={(event) => updateField("zip", event.target.value)}
                  />
                </Field>

                <div className="md:col-span-2 grid gap-6 md:grid-cols-2 pt-6 border-t mt-2">
                  <div className="space-y-4">
                    <FieldLabel className="text-base text-foreground mb-4">
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
                    <FieldLabel className="text-base text-foreground mb-4">
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
              </CardContent>
            </Card>

            {saveState.error && (
              <p className="text-sm font-medium text-destructive">
                {saveState.error}
              </p>
            )}

            <div className="flex items-center justify-end space-x-3 pt-4 border-t">
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
