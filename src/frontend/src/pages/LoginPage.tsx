/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import type { SessionPageProps } from "../types/app";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardDescription,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Field, FieldLabel } from "../components/ui/field";
import { Input } from "../components/ui/input";
import { EyeIcon, EyeOffIcon } from "lucide-react";

export default function LoginPage({ sessionState }: SessionPageProps) {
  const session = sessionState.data;
  const location = useLocation();
  const error = new URLSearchParams(location.search).get("error");
  const [showPassword, setShowPassword] = useState(false);

  if (session?.authenticated) {
    return <Navigate replace to="/" />;
  }

  return (
    <Card className="w-full max-w-[420px] bg-background shadow-lg rounded-xl mx-auto overflow-hidden border">
      <CardHeader className="space-y-1 pb-6 pt-6 bg-transparent text-left">
        <div className="flex items-center justify-between">
          <CardTitle className="text-2xl font-bold tracking-tight">
            Login to your account
          </CardTitle>
          <a
            href="/signup"
            className="text-sm font-medium text-primary hover:underline"
          >
            Sign Up
          </a>
        </div>
        <CardDescription className="text-[15px] font-normal text-muted-foreground pt-1">
          Enter your email below to login to your account
        </CardDescription>
      </CardHeader>
      <CardContent className="pt-0 pb-6 px-6">
        <form className="space-y-4" method="post" action="/login">
          <div className="space-y-4">
            <Field className="space-y-2">
              <FieldLabel className="text-sm font-medium leading-none text-foreground">
                Email
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
                <a
                  href="/login/forgot"
                  className="text-sm font-medium text-primary hover:underline"
                >
                  Forgot your password?
                </a>
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
                  className="absolute right-0 top-0 h-10 w-10 text-muted-foreground hover:text-foreground hover:bg-transparent"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? (
                    <EyeOffIcon className="h-4 w-4" />
                  ) : (
                    <EyeIcon className="h-4 w-4" />
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
              className="w-full h-10 font-medium rounded-md"
            >
              Login
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
