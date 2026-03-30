/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { FormEvent } from "react";
import { useEffect, useRef, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import type { SessionPageProps } from "../types/app";
import {
  pickInstallationBranding,
  readCachedInstallationBranding,
} from "../utils/installationBranding";
import { Card, CardContent } from "../components/ui/card";
import { CardHeader, CardTitle } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { ArrowLeftIcon, CheckCircleIcon } from "lucide-react";
import "@cap.js/widget";

interface CapWidgetElement extends HTMLElement {
  reset?: () => void;
}

function toErrorMessage(text: string, fallback: string): string {
  const trimmed = text.trim();
  if (!trimmed) {
    return fallback;
  }
  const normalized = trimmed.toLowerCase();
  if (
    normalized.startsWith("<!doctype html") ||
    normalized.startsWith("<html")
  ) {
    return fallback;
  }
  return trimmed;
}

export default function ForgotPasswordPage({ sessionState }: SessionPageProps) {
  const session = sessionState.data;
  const [email, setEmail] = useState("");
  const [capToken, setCapToken] = useState("");
  const [capEndpoint, setCapEndpoint] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const widgetRef = useRef<CapWidgetElement>(null);

  const branding = {
    ...readCachedInstallationBranding(),
    ...pickInstallationBranding(session),
  };
  const brandName = branding.installationCompanyName || "billetsys";
  const logoSrc = branding.installationLogoBase64;

  useEffect(() => {
    fetch("/api/cap/endpoint")
      .then((res) => res.json())
      .then((data) => setCapEndpoint(data.endpoint || ""))
      .catch(() => {});
  }, []);

  useEffect(() => {
    const el = widgetRef.current;
    if (!el) return;

    const handleSolve = (e: Event) => {
      const detail = (e as CustomEvent).detail;
      if (detail?.token) setCapToken(detail.token);
    };
    const handleError = () => setError("CAPTCHA error. Please try again.");
    const handleReset = () => setCapToken("");

    el.addEventListener("solve", handleSolve);
    el.addEventListener("error", handleError);
    el.addEventListener("reset", handleReset);
    return () => {
      el.removeEventListener("solve", handleSolve);
      el.removeEventListener("error", handleError);
      el.removeEventListener("reset", handleReset);
    };
  }, [capEndpoint]);

  if (session?.authenticated) {
    return <Navigate replace to="/" />;
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    setSuccess("");
    try {
      const response = await fetch("/api/password-reset/request", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, capToken }),
      });
      if (!response.ok) {
        throw new Error(
          toErrorMessage(await response.text(), "Request failed."),
        );
      }
      const data = await response.json();
      setSuccess(
        data.message ||
          "Password reset instructions have been sent to your email.",
      );
      setEmail("");
      setCapToken("");
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Something went wrong.");
    } finally {
      if (capEndpoint) {
        setCapToken("");
        widgetRef.current?.reset?.();
      }
      setSubmitting(false);
    }
  };

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
        {success ? (
          <div className="flex flex-col items-center justify-center text-center space-y-4 py-4">
            <CheckCircleIcon className="h-12 w-12 text-green-500" />
            <h2 className="text-2xl font-bold tracking-tight text-foreground">
              Check your email
            </h2>
            <p className="text-sm text-muted-foreground">{success}</p>
            <Link
              to="/login"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline pt-2"
            >
              <ArrowLeftIcon className="h-3.5 w-3.5" />
              Back to Login
            </Link>
          </div>
        ) : (
          <>
            <CardHeader className="p-0 mb-6 text-left">
              <CardTitle className="text-2xl font-bold tracking-tight">
                Forgot Credentials
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <form className="space-y-5" onSubmit={submit}>
                <div className="space-y-4">
                  <Field className="space-y-2">
                    <FieldLabel className="text-sm font-medium leading-none text-foreground">
                      Email address
                    </FieldLabel>
                    <Input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      autoComplete="email"
                      required
                      className="h-10 focus-visible:ring-1 focus-visible:ring-primary focus-visible:border-primary focus-visible:ring-offset-0"
                    />
                  </Field>
                </div>

                {capEndpoint && (
                  <div className="flex justify-center">
                    <cap-widget
                      ref={widgetRef}
                      data-cap-api-endpoint={capEndpoint}
                    />
                  </div>
                )}

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
                    disabled={submitting || (!!capEndpoint && !capToken)}
                  >
                    {submitting ? "Sending..." : "Send Reset Email"}
                  </Button>
                </div>

                <div className="text-center">
                  <Link
                    to="/login"
                    className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline"
                  >
                    <ArrowLeftIcon className="h-3.5 w-3.5" />
                    Back to Login
                  </Link>
                </div>
              </form>
            </CardContent>
          </>
        )}
      </div>
    </Card>
  );
}
