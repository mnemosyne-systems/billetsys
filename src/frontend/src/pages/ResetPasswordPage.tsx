/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useState } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import type { SessionPageProps } from "../types/app";
import {
  pickInstallationBranding,
  readCachedInstallationBranding,
} from "../utils/installationBranding";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  CheckCircleIcon,
  EyeIcon,
  EyeOffIcon,
  AlertTriangleIcon,
} from "lucide-react";

export default function ResetPasswordPage({ sessionState }: SessionPageProps) {
  const session = sessionState.data;
  const location = useLocation();
  const token = new URLSearchParams(location.search).get("token") || "";
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");

  const branding = {
    ...readCachedInstallationBranding(),
    ...pickInstallationBranding(session),
  };
  const brandName = branding.installationCompanyName || "billetsys";
  const logoSrc = branding.installationLogoBase64;

  if (session?.authenticated) {
    return <Navigate replace to="/" />;
  }

  const logoSection = (
    <div className="w-full md:w-5/12 p-8 flex flex-col items-center justify-center text-center border-b md:border-b-0 md:border-r border-border/50">
      <div className="flex items-center justify-center mb-6">
        {logoSrc ? (
          <img
            src={logoSrc}
            alt={`${brandName} logo`}
            className="h-20 w-auto max-w-[160px] object-contain drop-shadow-md"
          />
        ) : (
          <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
            className="w-20 h-20 fill-none stroke-primary stroke-[1.5]"
          >
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <path d="M7 8h10M7 12h10M7 16h6" />
            <path d="M6 8l1 1 2-2" />
          </svg>
        )}
      </div>
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          {brandName}
        </h1>
      </div>
    </div>
  );

  if (!token) {
    return (
      <Card className="w-full max-w-[760px] bg-background/95 backdrop-blur shadow-2xl rounded-2xl mx-auto overflow-hidden border-0 flex flex-col md:flex-row">
        {logoSection}
        <div className="w-full md:w-7/12 p-8 lg:p-10">
          <div className="flex flex-col items-center justify-center text-center space-y-4 py-4">
            <AlertTriangleIcon className="h-12 w-12 text-destructive" />
            <h2 className="text-2xl font-bold tracking-tight text-foreground">
              Invalid Link
            </h2>
            <p className="text-sm text-muted-foreground">
              This password reset link is invalid or has expired.
            </p>
            <Link
              to="/forgot-password"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline pt-2"
            >
              Request a new link
              <ArrowRightIcon className="h-3.5 w-3.5" />
            </Link>
          </div>
        </div>
      </Card>
    );
  }

  if (success) {
    return (
      <Card className="w-full max-w-[760px] bg-background/95 backdrop-blur shadow-2xl rounded-2xl mx-auto overflow-hidden border-0 flex flex-col md:flex-row">
        {logoSection}
        <div className="w-full md:w-7/12 p-8 lg:p-10">
          <div className="flex flex-col items-center justify-center text-center space-y-4 py-4">
            <CheckCircleIcon className="h-12 w-12 text-green-500" />
            <h2 className="text-2xl font-bold tracking-tight text-foreground">
              Password Updated
            </h2>
            <p className="text-sm text-muted-foreground">
              Your password has been successfully reset.
            </p>
            <Link
              to="/login"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline pt-2"
            >
              Go to Login
              <ArrowRightIcon className="h-3.5 w-3.5" />
            </Link>
          </div>
        </div>
      </Card>
    );
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const response = await fetch("/api/password-reset/reset", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, newPassword, confirmPassword }),
      });
      if (!response.ok) {
        throw new Error((await response.text()) || "Unable to reset password.");
      }
      setSuccess(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Something went wrong.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card className="w-full max-w-[760px] bg-background/95 backdrop-blur shadow-2xl rounded-2xl mx-auto overflow-hidden border-0 flex flex-col md:flex-row">
      {logoSection}

      <div className="w-full md:w-7/12 p-8 lg:p-10">
        <CardHeader className="p-0 mb-6 text-left">
          <CardTitle className="text-2xl font-bold tracking-tight">
            Reset Password
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <form className="space-y-5" onSubmit={submit}>
            <div className="space-y-4">
              <Field className="space-y-2">
                <FieldLabel className="text-sm font-medium leading-none text-foreground">
                  New password
                </FieldLabel>
                <div className="relative">
                  <Input
                    type={showNewPassword ? "text" : "password"}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    autoComplete="new-password"
                    required
                    placeholder="••••••••"
                    className="h-10 pr-10 focus-visible:ring-1 focus-visible:ring-primary focus-visible:border-primary focus-visible:ring-offset-0"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="absolute right-0 top-0 h-10 w-10 rounded-l-none border-l-0"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                  >
                    {showNewPassword ? (
                      <EyeOffIcon className="h-4 w-4 text-[var(--color-buttons-text)]" />
                    ) : (
                      <EyeIcon className="h-4 w-4 text-[var(--color-buttons-text)]" />
                    )}
                    <span className="sr-only">
                      {showNewPassword ? "Hide password" : "Show password"}
                    </span>
                  </Button>
                </div>
              </Field>
              <Field className="space-y-2">
                <FieldLabel className="text-sm font-medium leading-none text-foreground">
                  Confirm password
                </FieldLabel>
                <div className="relative">
                  <Input
                    type={showConfirmPassword ? "text" : "password"}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    autoComplete="new-password"
                    required
                    placeholder="••••••••"
                    className="h-10 pr-10 focus-visible:ring-1 focus-visible:ring-primary focus-visible:border-primary focus-visible:ring-offset-0"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="absolute right-0 top-0 h-10 w-10 rounded-l-none border-l-0"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  >
                    {showConfirmPassword ? (
                      <EyeOffIcon className="h-4 w-4 text-[var(--color-buttons-text)]" />
                    ) : (
                      <EyeIcon className="h-4 w-4 text-[var(--color-buttons-text)]" />
                    )}
                    <span className="sr-only">
                      {showConfirmPassword ? "Hide password" : "Show password"}
                    </span>
                  </Button>
                </div>
              </Field>
            </div>

            {error && (
              <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-md">
                <p className="text-destructive font-medium text-sm text-center">
                  {error}
                </p>
              </div>
            )}

            <div className="pt-2">
              <Button
                type="submit"
                className="w-full h-10 font-medium rounded-md shadow-sm"
                disabled={submitting}
              >
                {submitting ? "Resetting..." : "Reset Password"}
              </Button>
            </div>

            <div className="text-center">
              <Link
                to="/login"
                className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline"
              >
              </Link>
            </div>
          </form>
        </CardContent>
      </div>
    </Card>
  );
}
