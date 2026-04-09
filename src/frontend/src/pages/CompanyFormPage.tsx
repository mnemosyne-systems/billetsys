/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import DataState from "../components/common/DataState";
import { resolvePostRedirectPath } from "../utils/routing";
import { postForm } from "../utils/api";
import { isNetworkRequestError, submitBrowserForm } from "../utils/forms";
import {
  SelectableUserPicker,
  SelectableUserSummary,
} from "../components/users/UserComponents";
import type { FormMode, SessionPageProps } from "../types/app";
import type { CompanyFormBootstrap } from "../types/domain";
import type { CompanyEntitlementEntry, CompanyFormState } from "../types/forms";
import type { BrowserFormEntries } from "../utils/forms";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "../components/ui/alert-dialog";
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

interface CompanyFormPageProps extends SessionPageProps {
  mode: FormMode;
}

const EMPTY_COMPANY_FORM_STATE: CompanyFormState = {
  name: "",
  address1: "",
  address2: "",
  city: "",
  state: "",
  zip: "",
  phoneNumber: "",
  countryId: "",
  timezoneId: "",
  selectedUserIds: [],
  selectedTamIds: [],
  entitlements: [],
  superuserId: "",
  superuserUsername: "",
  superuserFullName: "",
  superuserEmail: "",
  superuserSocial: "",
  superuserPhoneNumber: "",
  superuserPhoneExtension: "",
  superuserCountryId: "",
  superuserTimezoneId: "",
  superuserPassword: "",
};

export default function CompanyFormPage({
  sessionState,
  mode,
}: CompanyFormPageProps) {
  const navigate = useNavigate();

  const { id } = useParams();
  const companyState = useJson<CompanyFormBootstrap>(
    mode === "edit" && id ? `/api/companies/${id}` : "/api/companies/bootstrap",
  );
  const company = companyState.data;
  const [formState, setFormState] = useState<CompanyFormState | null>(null);
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const submissionGuard = useSubmissionGuard();
  const isEdit = mode === "edit";

  useEffect(() => {
    if (!company) {
      return;
    }
    if (isEdit) {
      setFormState({
        ...EMPTY_COMPANY_FORM_STATE,
        name: company.name || "",
        address1: company.address1 || "",
        address2: company.address2 || "",
        city: company.city || "",
        state: company.state || "",
        zip: company.zip || "",
        phoneNumber: company.phoneNumber || "",
        countryId: company.countryId
          ? String(company.countryId)
          : company.defaultCountryId
            ? String(company.defaultCountryId)
            : "",
        timezoneId: company.timezoneId
          ? String(company.timezoneId)
          : company.defaultTimezoneId
            ? String(company.defaultTimezoneId)
            : "",
        selectedUserIds: company.selectedUserIds || [],
        selectedTamIds: company.selectedTamIds || [],
        entitlements:
          company.entitlementAssignments?.map((entry) => ({
            entitlementId: entry.entitlementId
              ? String(entry.entitlementId)
              : "",
            levelId: entry.levelId ? String(entry.levelId) : "",
            date: entry.date || company.todayDate || "",
            duration: entry.duration ? String(entry.duration) : String(2),
          })) || [],
        superuserId: company.superuserId ? String(company.superuserId) : "",
      });
      return;
    }
    setFormState({
      ...EMPTY_COMPANY_FORM_STATE,
      countryId: company.defaultCountryId
        ? String(company.defaultCountryId)
        : "",
      timezoneId: company.defaultTimezoneId
        ? String(company.defaultTimezoneId)
        : "",
      superuserCountryId: company.defaultCountryId
        ? String(company.defaultCountryId)
        : "",
      superuserTimezoneId: company.defaultTimezoneId
        ? String(company.defaultTimezoneId)
        : "",
    });
  }, [company, isEdit]);

  const availableTimezones =
    company?.timezones?.filter(
      (timezone) =>
        !formState?.countryId ||
        String(timezone.countryId) === formState.countryId,
    ) || [];
  const availableSuperuserTimezones =
    company?.timezones?.filter(
      (timezone) =>
        !formState?.superuserCountryId ||
        String(timezone.countryId) === formState.superuserCountryId,
    ) || [];

  const updateFormState = <K extends keyof CompanyFormState>(
    field: K,
    value: CompanyFormState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  const toggleSelection = (
    field: "selectedUserIds" | "selectedTamIds",
    idToToggle: string | number,
  ) => {
    setFormState((current) =>
      current
        ? {
            ...current,
            [field]: current[field].includes(idToToggle)
              ? current[field].filter((existing) => existing !== idToToggle)
              : [...current[field], idToToggle],
          }
        : current,
    );
  };

  const updateEntitlement = (
    index: number,
    field: keyof CompanyEntitlementEntry,
    value: string,
  ) => {
    setFormState((current) =>
      current
        ? {
            ...current,
            entitlements: current.entitlements.map((entry, entryIndex) =>
              entryIndex === index ? { ...entry, [field]: value } : entry,
            ),
          }
        : current,
    );
  };

  const addEntitlement = () => {
    setFormState((current) =>
      current
        ? {
            ...current,
            entitlements: [
              ...current.entitlements,
              {
                entitlementId: "",
                levelId: "",
                date: company?.todayDate || "",
                duration: String(company?.durations?.[1]?.value || 2),
              },
            ],
          }
        : current,
    );
  };

  const removeEntitlement = (index: number) => {
    setFormState((current) =>
      current
        ? {
            ...current,
            entitlements: current.entitlements.filter(
              (_, entryIndex) => entryIndex !== index,
            ),
          }
        : current,
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !submissionGuard.tryEnter()) {
      return;
    }
    const entries: BrowserFormEntries = [
      ["name", formState.name],
      ["address1", formState.address1],
      ["address2", formState.address2],
      ["city", formState.city],
      ["state", formState.state],
      ["zip", formState.zip],
      ["phoneNumber", formState.phoneNumber],
      ["countryId", formState.countryId],
      ["timezoneId", formState.timezoneId],
      ...formState.selectedUserIds.map((userId): [string, string] => [
        "userIds",
        String(userId),
      ]),
      ...formState.selectedTamIds.map((userId): [string, string] => [
        "tamIds",
        String(userId),
      ]),
      ...formState.entitlements.flatMap(
        (entry): BrowserFormEntries => [
          ["entitlementIds", entry.entitlementId],
          ["levelIds", entry.levelId],
          ["entitlementDates", entry.date],
          ["entitlementDurations", entry.duration],
        ],
      ),
    ];
    try {
      setSaveState({ saving: true, error: "" });
      if (isEdit) {
        entries.push(["superuserId", formState.superuserId]);
      } else {
        entries.push(
          ["superuserUsername", formState.superuserUsername],
          ["superuserFullName", formState.superuserFullName],
          ["superuserEmail", formState.superuserEmail],
          ["superuserSocial", formState.superuserSocial],
          ["superuserPhoneNumber", formState.superuserPhoneNumber],
          ["superuserPhoneExtension", formState.superuserPhoneExtension],
          ["superuserCountryId", formState.superuserCountryId],
          ["superuserTimezoneId", formState.superuserTimezoneId],
          ["superuserPassword", formState.superuserPassword],
        );
      }

      const response = await postForm(
        isEdit ? `/companies/${id}` : "/companies",
        entries,
      );
      toast.success(
        isEdit
          ? "Company updated successfully."
          : "Company created successfully.",
      );
      navigate(await resolvePostRedirectPath(response, "/companies"));
    } catch (error: unknown) {
      if (isNetworkRequestError(error)) {
        setSaveState({ saving: false, error: "" });
        submissionGuard.exit();
        submitBrowserForm(isEdit ? `/companies/${id}` : "/companies", entries);
        return;
      }
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to save company.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to save company.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const deleteCompany = async () => {
    if (!id || !submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(`/companies/${id}/delete`, []);
      toast.success("Company deleted.");
      navigate(await resolvePostRedirectPath(response, "/companies"));
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to delete company.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to delete company.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  return (
    <section className="w-full max-w-5xl mx-auto mt-4">
      <div className="flex items-center justify-between pb-6 px-1">
        <h2 className="text-3xl font-bold tracking-tight">
          {isEdit && company
            ? company.name || "Edit company"
            : "Create company"}
        </h2>
      </div>

      <DataState
        state={companyState}
        emptyMessage="Company not found."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {formState && company && (
          <form className="space-y-6 pb-20" onSubmit={submit}>
            {/* COMPANY DETAILS SECTION */}
            <Card>
              <CardHeader>
                <CardTitle>Company Details</CardTitle>
                {!isEdit && (
                  <p className="text-sm text-muted-foreground mt-1">
                    Required fields are marked{" "}
                    <span className="text-destructive">*</span>.
                  </p>
                )}
              </CardHeader>
              <CardContent className="grid gap-6 md:grid-cols-2">
                <Field>
                  <FieldLabel>
                    Name{" "}
                    {!isEdit && <span className="text-destructive">*</span>}
                  </FieldLabel>
                  <Input
                    value={formState.name}
                    onChange={(event) =>
                      updateFormState("name", event.target.value)
                    }
                    required
                  />
                </Field>
                <Field>
                  <FieldLabel>Phone number</FieldLabel>
                  <PhoneInput
                    defaultCountry="US"
                    value={formState.phoneNumber}
                    onChange={(value) =>
                      updateFormState("phoneNumber", value || "")
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel>Address 1</FieldLabel>
                  <Input
                    value={formState.address1}
                    onChange={(event) =>
                      updateFormState("address1", event.target.value)
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel>Address 2</FieldLabel>
                  <Input
                    value={formState.address2}
                    onChange={(event) =>
                      updateFormState("address2", event.target.value)
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel>City</FieldLabel>
                  <Input
                    value={formState.city}
                    onChange={(event) =>
                      updateFormState("city", event.target.value)
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel>State</FieldLabel>
                  <Input
                    value={formState.state}
                    onChange={(event) =>
                      updateFormState("state", event.target.value)
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel>Zip</FieldLabel>
                  <Input
                    value={formState.zip}
                    onChange={(event) =>
                      updateFormState("zip", event.target.value)
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
                          (company.countries || []).find(
                            (cc) => String(cc.id) === formState.countryId,
                          )?.name,
                      )?.alpha2 || ""
                    }
                    onChange={(country) => {
                      const matched = (company.countries || []).find(
                        (c) => c.name === country.name,
                      );
                      const nextCountryId = matched ? String(matched.id) : "";
                      const timezoneStillValid = (company.timezones || []).some(
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
                    onValueChange={(value) =>
                      updateFormState("timezoneId", value)
                    }
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

                {isEdit && (
                  <div className="md:col-span-2 pt-2">
                    <FieldLabel>Superuser</FieldLabel>
                    <div className="mt-2">
                      <SelectableUserSummary
                        users={company.selectedSuperusers}
                      />
                    </div>
                  </div>
                )}

                <div className="md:col-span-2 mt-2 space-y-4 pt-4 border-t">
                  <SelectableUserPicker
                    title="Users"
                    users={company.userOptions || []}
                    selectedIds={formState.selectedUserIds}
                    onToggle={(userId) =>
                      toggleSelection("selectedUserIds", userId)
                    }
                  />
                  <SelectableUserPicker
                    title="TAMs"
                    users={company.tamOptions || []}
                    selectedIds={formState.selectedTamIds}
                    onToggle={(userId) =>
                      toggleSelection("selectedTamIds", userId)
                    }
                  />
                </div>
              </CardContent>
            </Card>

            {/* SUPERUSER SECTION */}
            {!isEdit && (
              <Card>
                <CardHeader>
                  <CardTitle>Superuser Details</CardTitle>
                </CardHeader>
                <CardContent className="grid gap-6 md:grid-cols-2">
                  <Field>
                    <FieldLabel>
                      Username <span className="text-destructive">*</span>
                    </FieldLabel>
                    <Input
                      value={formState.superuserUsername}
                      onChange={(event) =>
                        updateFormState("superuserUsername", event.target.value)
                      }
                      required
                    />
                  </Field>
                  <Field>
                    <FieldLabel>Full name</FieldLabel>
                    <Input
                      value={formState.superuserFullName}
                      onChange={(event) =>
                        updateFormState("superuserFullName", event.target.value)
                      }
                    />
                  </Field>
                  <Field>
                    <FieldLabel>
                      Email <span className="text-destructive">*</span>
                    </FieldLabel>
                    <Input
                      type="email"
                      value={formState.superuserEmail}
                      onChange={(event) =>
                        updateFormState("superuserEmail", event.target.value)
                      }
                      required
                    />
                  </Field>
                  <Field>
                    <FieldLabel>Social</FieldLabel>
                    <Input
                      value={formState.superuserSocial}
                      onChange={(event) =>
                        updateFormState("superuserSocial", event.target.value)
                      }
                    />
                  </Field>
                  <Field>
                    <FieldLabel>Phone number</FieldLabel>
                    <PhoneInput
                      defaultCountry="US"
                      value={formState.superuserPhoneNumber}
                      onChange={(value) =>
                        updateFormState("superuserPhoneNumber", value || "")
                      }
                    />
                  </Field>
                  <Field>
                    <FieldLabel>Phone extension</FieldLabel>
                    <Input
                      value={formState.superuserPhoneExtension}
                      onChange={(event) =>
                        updateFormState(
                          "superuserPhoneExtension",
                          event.target.value,
                        )
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
                            (company.countries || []).find(
                              (cc) =>
                                String(cc.id) === formState.superuserCountryId,
                            )?.name,
                        )?.alpha2 || ""
                      }
                      onChange={(country) => {
                        const matched = (company.countries || []).find(
                          (c) => c.name === country.name,
                        );
                        const nextCountryId = matched ? String(matched.id) : "";
                        const timezoneStillValid = (
                          company.timezones || []
                        ).some(
                          (timezone) =>
                            String(timezone.id) ===
                              formState.superuserTimezoneId &&
                            String(timezone.countryId) === nextCountryId,
                        );
                        setFormState((current) =>
                          current
                            ? {
                                ...current,
                                superuserCountryId: nextCountryId,
                                superuserTimezoneId: timezoneStillValid
                                  ? current.superuserTimezoneId
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
                      value={formState.superuserTimezoneId || undefined}
                      onValueChange={(value) =>
                        updateFormState("superuserTimezoneId", value)
                      }
                    >
                      <SelectTrigger className="w-full">
                        <SelectValue placeholder="Select a time zone" />
                      </SelectTrigger>
                      <SelectContent>
                        {availableSuperuserTimezones.map((timezone) => (
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
                  <Field className="md:col-span-2">
                    <FieldLabel>
                      Password <span className="text-destructive">*</span>
                    </FieldLabel>
                    <Input
                      type="password"
                      value={formState.superuserPassword}
                      onChange={(event) =>
                        updateFormState("superuserPassword", event.target.value)
                      }
                      required
                    />
                  </Field>
                </CardContent>
              </Card>
            )}

            {/* ENTITLEMENTS SECTION */}
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <div>
                  <CardTitle>Entitlements</CardTitle>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={addEntitlement}
                >
                  Add Entitlement
                </Button>
              </CardHeader>
              <CardContent className="space-y-4">
                {formState.entitlements.map((entry, index) => (
                  <div
                    key={`${entry.entitlementId || "new"}-${entry.levelId || "level"}-${index}`}
                    className="p-4 border rounded-lg bg-card text-card-foreground shadow-sm flex flex-col md:flex-row md:items-end gap-4"
                  >
                    <div className="grid gap-4 md:grid-cols-4 flex-grow">
                      <Field>
                        <FieldLabel>Entitlement</FieldLabel>
                        <Select
                          value={entry.entitlementId || undefined}
                          onValueChange={(value) =>
                            updateEntitlement(index, "entitlementId", value)
                          }
                          required
                        >
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Select entitlement" />
                          </SelectTrigger>
                          <SelectContent>
                            {(company.entitlements || []).map((option) => (
                              <SelectItem
                                key={option.id}
                                value={String(option.id)}
                              >
                                {option.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </Field>
                      <Field>
                        <FieldLabel>Level</FieldLabel>
                        <Select
                          value={entry.levelId || undefined}
                          onValueChange={(value) =>
                            updateEntitlement(index, "levelId", value)
                          }
                          required
                        >
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Select level" />
                          </SelectTrigger>
                          <SelectContent>
                            {(company.levels || []).map((option) => (
                              <SelectItem
                                key={option.id}
                                value={String(option.id)}
                              >
                                {option.name} ({option.level})
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </Field>
                      <Field>
                        <FieldLabel>Date</FieldLabel>
                        <Input
                          type="date"
                          value={entry.date}
                          onChange={(event) =>
                            updateEntitlement(index, "date", event.target.value)
                          }
                          required
                        />
                      </Field>
                      <Field>
                        <FieldLabel>Duration</FieldLabel>
                        <Select
                          value={entry.duration || undefined}
                          onValueChange={(value) =>
                            updateEntitlement(index, "duration", value)
                          }
                          required
                        >
                          <SelectTrigger className="w-full">
                            <SelectValue placeholder="Select duration" />
                          </SelectTrigger>
                          <SelectContent>
                            {(company.durations || []).map((option) => (
                              <SelectItem
                                key={option.value}
                                value={String(option.value)}
                              >
                                {option.label}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </Field>
                    </div>
                    <div className="flex-shrink-0 mt-4 md:mt-0">
                      <Button
                        type="button"
                        variant="destructive"
                        onClick={() => removeEntitlement(index)}
                      >
                        Remove
                      </Button>
                    </div>
                  </div>
                ))}

                {formState.entitlements.length === 0 && (
                  <p className="text-sm text-muted-foreground italic text-center py-4">
                    No entitlements added yet.
                  </p>
                )}
              </CardContent>
            </Card>

            <div className="flex items-center justify-end space-x-3 pt-4 border-t">
              {isEdit && (
                <AlertDialog>
                  <AlertDialogTrigger asChild>
                    <Button
                      type="button"
                      variant="destructive"
                      disabled={saveState.saving}
                      className="mr-auto"
                    >
                      Delete company
                    </Button>
                  </AlertDialogTrigger>
                  <AlertDialogContent>
                    <AlertDialogHeader>
                      <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                      <AlertDialogDescription>
                        This action cannot be undone. This will permanently
                        delete this company.
                      </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                      <AlertDialogCancel>Cancel</AlertDialogCancel>
                      <AlertDialogAction
                        onClick={deleteCompany}
                        className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                      >
                        Delete
                      </AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
              )}
              <Button type="submit" disabled={saveState.saving}>
                {saveState.saving
                  ? "Saving..."
                  : isEdit
                    ? "Save company"
                    : "Create company"}
              </Button>
            </div>
          </form>
        )}
      </DataState>
    </section>
  );
}
