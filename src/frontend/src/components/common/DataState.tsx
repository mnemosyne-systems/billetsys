/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import type { AsyncState } from "../../types/app";
import { Spinner } from "../ui/spinner";

interface DataStateProps<T> {
  state: AsyncState<T>;
  emptyMessage: string;
  children?: ReactNode;
}

export default function DataState<T>({
  state,
  emptyMessage,
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
    return <Navigate replace to="/login" />;
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
