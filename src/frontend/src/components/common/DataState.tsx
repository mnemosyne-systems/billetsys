/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactNode } from "react";
import type { AsyncState } from "../../types/app";
import { Button } from "../ui/button";
import { Spinner } from "../ui/spinner";

interface DataStateProps<T> {
  state: AsyncState<T>;
  emptyMessage: string;
  signInHref: string;
  children?: ReactNode;
}

export default function DataState<T>({
  state,
  emptyMessage,
  signInHref,
  children,
}: DataStateProps<T>) {
  if (state.loading) {
    return (
      <div className="flex items-center gap-2 text-muted-foreground p-4">
        <Spinner className="size-4" />
        <span className="text-sm font-medium">Loading...</span>
      </div>
    );
  }

  if (state.unauthorized) {
    return (
      <div className="grid gap-3.5 justify-items-start pt-4">
        <p>You need to sign in to view this area.</p>
        <Button asChild>
          <a href={signInHref}>Sign in</a>
        </Button>
      </div>
    );
  }

  if (state.forbidden) {
    return (
      <p className="text-destructive font-semibold">
        You do not have access to this area.
      </p>
    );
  }

  if (state.error) {
    return <p className="text-destructive font-semibold">{state.error}</p>;
  }

  if (state.empty) {
    return <p className="text-muted-foreground">{emptyMessage}</p>;
  }

  return children;
}
