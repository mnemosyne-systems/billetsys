/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useState } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import type { SessionPageProps } from "../types/app";
import {
  pickInstallationBranding,
  readCachedInstallationBranding,
} from "../utils/installationBranding";
import { Card, CardContent } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { EyeIcon, EyeOffIcon } from "lucide-react";

export default function LoginPage({ sessionState }: SessionPageProps) {
  const session = sessionState.data;
  const location = useLocation();
  const error = new URLSearchParams(location.search).get("error");
  const [showPassword, setShowPassword] = useState(false);

  const branding = {
    ...readCachedInstallationBranding(),
    ...pickInstallationBranding(session),
  };
  const brandName = branding.installationCompanyName || "billetsys";
  const logoSrc = branding.installationLogoBase64;

  if (session?.authenticated) {
    return <Navigate replace to="/" />;
  }

  return (
    <Card className="w-full max-w-[760px] bg-background/95 backdrop-blur shadow-2xl rounded-2xl mx-auto overflow-hidden border-0 flex flex-col md:flex-row">
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

      <div className="w-full md:w-7/12 p-8 lg:p-10">
        <CardContent className="p-0">
          <form className="space-y-5" method="post" action="/login">
            <div className="space-y-4">
              <Field className="space-y-2">
                <FieldLabel className="text-sm font-medium leading-none text-foreground">
                  Username
                </FieldLabel>
                <Input
                  name="username"
                  autoComplete="username"
                  required
                  className="h-10 focus-visible:ring-1 focus-visible:ring-primary focus-visible:border-primary focus-visible:ring-offset-0"
                />
              </Field>
              <Field className="space-y-2">
                <div className="flex items-center justify-between">
                  <FieldLabel className="text-sm font-medium leading-none text-foreground">
                    Password
                  </FieldLabel>
                  <Link
                    to="/forgot-password"
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    Forgot your password?
                  </Link>
                </div>
                <div className="relative">
                  <Input
                    type={showPassword ? "text" : "password"}
                    name="password"
                    autoComplete="current-password"
                    required
                    placeholder="••••••••"
                    className="h-10 pr-10 focus-visible:ring-1 focus-visible:ring-primary focus-visible:border-primary focus-visible:ring-offset-0"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="absolute right-0 top-0 h-10 w-10 rounded-l-none border-l-0"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? (
                      <EyeOffIcon className="h-4 w-4 text-[var(--color-buttons-text)]" />
                    ) : (
                      <EyeIcon className="h-4 w-4 text-[var(--color-buttons-text)]" />
                    )}
                    <span className="sr-only">
                      {showPassword ? "Hide password" : "Show password"}
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
              >
                Login
              </Button>
            </div>
          </form>
        </CardContent>
      </div>
    </Card>
  );
}
