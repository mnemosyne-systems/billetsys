/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ComponentProps, FormEvent } from "react";
import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getCountryCallingCode } from "react-phone-number-input";
import { toast } from "sonner";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import useJson from "../hooks/useJson";
import useSubmissionGuard from "../hooks/useSubmissionGuard";
import { postForm } from "../utils/api";
import { createDirectoryUserFormState } from "../utils/forms";
import { toQueryString } from "../utils/formatting";
import {
  resolveClientPath,
  resolvePostRedirectPath,
  SmartLink,
} from "../utils/routing";
import type { SessionPageProps } from "../types/app";
import type {
  CountryOption,
  DirectoryUserBootstrap,
  NamedEntity,
  TimezoneOption,
} from "../types/domain";
import type { DirectoryUserFormState } from "../utils/forms";
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
import { Field, FieldLabel, FieldDescription } from "../components/ui/field";
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

interface DirectoryUserFormPageProps extends SessionPageProps {
  bootstrapBase: string;
  navigateFallback: string;
}

interface UserTypeOption {
  value?: string;
  label?: string;
}

const UNASSIGNED_COMPANY_VALUE = "__unassigned__";

export default function DirectoryUserFormPage({
  bootstrapBase,
  navigateFallback,
}: DirectoryUserFormPageProps) {
  const navigate = useNavigate();

  const { id } = useParams();
  const location = useLocation();
  const query = new URLSearchParams(location.search);
  const requestedCompanyId = query.get("companyId") || "";
  const isEdit = Boolean(id);
  const isAdminCreate =
    !isEdit && bootstrapBase === "/api/admin/users/bootstrap";
  const [formState, setFormState] = useState<DirectoryUserFormState | null>(
    null,
  );
  const [saveState, setSaveState] = useState({ saving: false, error: "" });
  const submissionGuard = useSubmissionGuard();
  const selectedCountryId = formState?.countryId || "";
  const bootstrapState = useJson<DirectoryUserBootstrap>(
    `${bootstrapBase}${toQueryString({
      userId: isEdit ? id : undefined,
      companyId:
        formState?.companyId || (isAdminCreate ? "" : requestedCompanyId),
      countryId: selectedCountryId,
    })}`,
  );
  const bootstrap = bootstrapState.data;
  const selectedPhoneCountry: ComponentProps<
    typeof PhoneInput
  >["defaultCountry"] =
    (countries.all.find(
      (country) =>
        country.name ===
        (bootstrap?.countries || []).find(
          (option: CountryOption) => String(option.id) === selectedCountryId,
        )?.name,
    )?.alpha2 as ComponentProps<typeof PhoneInput>["defaultCountry"]) || "US";

  useEffect(() => {
    if (!bootstrap) {
      return;
    }
    setFormState((current) => {
      if (
        !current ||
        String(current.id || "") !== String(bootstrap.user?.id || "")
      ) {
        return {
          ...createDirectoryUserFormState(bootstrap),
          companyId: isAdminCreate
            ? ""
            : createDirectoryUserFormState(bootstrap).companyId,
        };
      }
      const timezones = bootstrap.timezones || [];
      const hasTimezone = timezones.some(
        (timezone) => String(timezone.id) === String(current.timezoneId || ""),
      );
      return {
        ...current,
        companyId:
          current.companyId ||
          (isAdminCreate
            ? ""
            : bootstrap.selectedCompanyId
              ? String(bootstrap.selectedCompanyId)
              : ""),
        timezoneId: hasTimezone
          ? current.timezoneId
          : timezones[0]?.id
            ? String(timezones[0].id)
            : "",
        type:
          current.type ||
          bootstrap.user?.type ||
          bootstrap.types?.[0]?.value ||
          "",
      };
    });
  }, [bootstrap, isAdminCreate]);

  const updateFormState = <K extends keyof DirectoryUserFormState>(
    field: K,
    value: DirectoryUserFormState[K],
  ) => {
    setFormState((current) =>
      current ? { ...current, [field]: value } : current,
    );
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState || !bootstrap) {
      return;
    }
    if (
      bootstrap.passwordRequired &&
      formState.password !== formState.verifyPassword
    ) {
      setSaveState({
        saving: false,
        error: "Password and verify password must match.",
      });
      toast.error("Password and verify password must match.");
      return;
    }
    if (!submissionGuard.tryEnter()) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(bootstrap.submitPath, [
        ["name", formState.name],
        ["fullName", formState.fullName],
        ["email", formState.email],
        ["social", formState.social],
        ["phoneNumber", formState.phoneNumber],
        ["phoneExtension", formState.phoneExtension],
        ["countryId", formState.countryId],
        ["timezoneId", formState.timezoneId],
        ["type", formState.type],
        ["companyId", formState.companyId],
        ["password", formState.password],
      ]);
      toast.success(
        isEdit ? "User updated successfully." : "User created successfully.",
      );
      navigate(
        await resolvePostRedirectPath(
          response,
          resolveClientPath(bootstrap.cancelPath, navigateFallback),
        ),
      );
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error: error instanceof Error ? error.message : "Unable to save user.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to save user.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const deleteUser = async () => {
    if (
      !id ||
      !bootstrap?.submitPath?.startsWith("/user/") ||
      !submissionGuard.tryEnter()
    ) {
      return;
    }
    try {
      setSaveState({ saving: true, error: "" });
      const response = await postForm(`/user/${id}/delete`, []);
      toast.success("User deleted.");
      navigate(
        await resolvePostRedirectPath(
          response,
          resolveClientPath(bootstrap.cancelPath, navigateFallback),
        ),
      );
    } catch (error: unknown) {
      setSaveState({
        saving: false,
        error:
          error instanceof Error ? error.message : "Unable to delete user.",
      });
      toast.error(
        error instanceof Error ? error.message : "Unable to delete user.",
      );
      return;
    } finally {
      submissionGuard.exit();
    }
    setSaveState({ saving: false, error: "" });
  };

  const pageTitle = isEdit
    ? formState?.name?.trim() ||
      formState?.fullName?.trim() ||
      bootstrap?.user?.name ||
      bootstrap?.user?.fullName ||
      "User"
    : isAdminCreate
      ? "New user"
      : bootstrap?.title || (isEdit ? "Edit user" : "New user");

  return (
    <section className="w-full mt-4">
      <PageHeader
        title={pageTitle}
        actions={
          !isAdminCreate &&
          !isEdit &&
          bootstrap?.submitPath?.startsWith("/user/") ? (
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button
                  type="button"
                  variant="destructive"
                  disabled={saveState.saving}
                >
                  Delete user
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                  <AlertDialogDescription>
                    This action cannot be undone. This will permanently delete
                    this user.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={deleteUser}
                    className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                  >
                    Delete
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          ) : null
        }
      />

      <DataState
        state={bootstrapState}
        emptyMessage="Unable to load the user form."
      >
        {formState && bootstrap && (
          <div>
            <form onSubmit={submit}>
              <div
                className={
                  isEdit || isAdminCreate
                    ? "grid gap-6 sm:grid-cols-2"
                    : "grid gap-6 sm:grid-cols-2 rounded-xl border bg-card px-6 py-6 shadow-sm"
                }
              >
                <Field>
                  <FieldLabel className="text-[var(--color-header-bg)]">
                    Username <span className="text-destructive">*</span>
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
                  <FieldLabel className="text-[var(--color-header-bg)]">
                    Full name
                  </FieldLabel>
                  <Input
                    value={formState.fullName}
                    onChange={(event) =>
                      updateFormState("fullName", event.target.value)
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel className="text-[var(--color-header-bg)]">
                    Email <span className="text-destructive">*</span>
                  </FieldLabel>
                  <Input
                    type="email"
                    value={formState.email}
                    onChange={(event) =>
                      updateFormState("email", event.target.value)
                    }
                    required
                  />
                </Field>
                <Field>
                  <FieldLabel className="text-[var(--color-header-bg)]">
                    Social
                  </FieldLabel>
                  <Input
                    value={formState.social}
                    onChange={(event) =>
                      updateFormState("social", event.target.value)
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel className="text-[var(--color-header-bg)]">
                    Phone number
                  </FieldLabel>
                  <PhoneInput
                    key={selectedPhoneCountry || "US"}
                    defaultCountry={selectedPhoneCountry}
                    value={formState.phoneNumber}
                    onChange={(value) =>
                      updateFormState("phoneNumber", value || "")
                    }
                  />
                </Field>
                <Field>
                  <FieldLabel className="text-[var(--color-header-bg)]">
                    Extension
                  </FieldLabel>
                  <Input
                    value={formState.phoneExtension}
                    onChange={(event) =>
                      updateFormState("phoneExtension", event.target.value)
                    }
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
                          (bootstrap.countries || []).find(
                            (bc: CountryOption) =>
                              String(bc.id) === formState.countryId,
                          )?.name,
                      )?.alpha2 || ""
                    }
                    onChange={(country) => {
                      const matched = (bootstrap.countries || []).find(
                        (c: CountryOption) => c.name === country.name,
                      );
                      const nextCountryId = matched ? String(matched.id) : "";
                      const nextCountryAlpha2 = countries.all.find(
                        (entry) => entry.name === country.name,
                      )?.alpha2;
                      setFormState((current) =>
                        current
                          ? {
                              ...current,
                              countryId: nextCountryId,
                              timezoneId: "",
                              phoneNumber: nextCountryAlpha2
                                ? (`+${getCountryCallingCode(
                                    nextCountryAlpha2 as Parameters<
                                      typeof getCountryCallingCode
                                    >[0],
                                  )}` as DirectoryUserFormState["phoneNumber"])
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
                    onValueChange={(value) =>
                      updateFormState("timezoneId", value)
                    }
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Select time zone" />
                    </SelectTrigger>
                    <SelectContent>
                      {(bootstrap.timezones || []).map(
                        (timezone: TimezoneOption) => (
                          <SelectItem
                            key={timezone.id}
                            value={String(timezone.id)}
                          >
                            {timezone.name}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>
                </Field>
                <Field>
                  <FieldLabel className="text-[var(--color-header-bg)]">
                    Type <span className="text-destructive">*</span>
                  </FieldLabel>
                  <Select
                    value={formState.type || undefined}
                    onValueChange={(value) => updateFormState("type", value)}
                    required
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Select a type" />
                    </SelectTrigger>
                    <SelectContent>
                      {(bootstrap.types || []).map((type: UserTypeOption) => (
                        <SelectItem key={type.value} value={type.value || ""}>
                          {type.label || type.value}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field>
                  <FieldLabel className="text-[var(--color-header-bg)]">
                    Company
                  </FieldLabel>
                  <Select
                    value={
                      formState.companyId
                        ? formState.companyId
                        : bootstrap.companyLocked
                          ? undefined
                          : UNASSIGNED_COMPANY_VALUE
                    }
                    disabled={bootstrap.companyLocked}
                    onValueChange={(value) =>
                      updateFormState(
                        "companyId",
                        value === UNASSIGNED_COMPANY_VALUE ? "" : value,
                      )
                    }
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue placeholder="Unassigned" />
                    </SelectTrigger>
                    <SelectContent>
                      {!bootstrap.companyLocked && (
                        <SelectItem value={UNASSIGNED_COMPANY_VALUE}>
                          Unassigned
                        </SelectItem>
                      )}
                      {(bootstrap.companies || []).map(
                        (company: NamedEntity) => (
                          <SelectItem
                            key={company.id}
                            value={String(company.id)}
                          >
                            {company.name}
                          </SelectItem>
                        ),
                      )}
                    </SelectContent>
                  </Select>
                </Field>
                <Field
                  className={
                    bootstrap.passwordRequired ? undefined : "sm:col-span-2"
                  }
                >
                  <FieldLabel className="text-[var(--color-header-bg)]">
                    Password{" "}
                    {bootstrap.passwordRequired && (
                      <span className="text-destructive">*</span>
                    )}
                  </FieldLabel>
                  <Input
                    type="password"
                    value={formState.password}
                    onChange={(event) =>
                      updateFormState("password", event.target.value)
                    }
                    required={bootstrap.passwordRequired}
                  />
                  {!bootstrap.passwordRequired && (
                    <FieldDescription>
                      Leave blank to keep current password.
                    </FieldDescription>
                  )}
                </Field>
                {bootstrap.passwordRequired && (
                  <Field>
                    <FieldLabel className="text-[var(--color-header-bg)]">
                      Verify password{" "}
                      <span className="text-destructive">*</span>
                    </FieldLabel>
                    <Input
                      type="password"
                      value={formState.verifyPassword}
                      onChange={(event) =>
                        updateFormState("verifyPassword", event.target.value)
                      }
                      required
                    />
                  </Field>
                )}
              </div>

              <div
                className={
                  isEdit
                    ? "flex items-center gap-3 pt-4"
                    : isAdminCreate
                      ? "flex items-center justify-end gap-3 pt-4"
                      : "flex items-center space-x-3 justify-end border-t bg-muted/20 px-6 py-4"
                }
              >
                {isEdit && bootstrap.submitPath?.startsWith("/user/") && (
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <Button
                        type="button"
                        variant="destructive"
                        disabled={saveState.saving}
                        className="mr-auto"
                      >
                        Delete
                      </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Are you sure?</AlertDialogTitle>
                        <AlertDialogDescription>
                          This action cannot be undone. This will permanently
                          delete this user.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                          onClick={deleteUser}
                          className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                          Delete
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                )}
                {!isAdminCreate && !isEdit && (
                  <Button variant="outline" asChild>
                    <SmartLink href={bootstrap?.cancelPath || navigateFallback}>
                      Cancel
                    </SmartLink>
                  </Button>
                )}
                <Button type="submit" disabled={saveState.saving}>
                  {saveState.saving
                    ? "Saving..."
                    : isAdminCreate
                      ? "Create"
                      : isEdit
                        ? "Save"
                        : "Create"}
                </Button>
              </div>
              {saveState.error && (
                <p className="mt-3 text-sm font-medium text-destructive">
                  {saveState.error}
                </p>
              )}
            </form>
          </div>
        )}
      </DataState>
    </section>
  );
}
