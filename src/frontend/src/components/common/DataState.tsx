/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactNode } from "react";
import type { AsyncState } from "../../types/app";

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
    return <p>Loading...</p>;
  }

  if (state.unauthorized) {
    return (
      <div className="empty-state">
        <p>You need to sign in to view this area.</p>
        <a className="primary-button" href={signInHref}>
          Sign in
        </a>
      </div>
    );
  }

  if (state.forbidden) {
    return <p className="error-text">You do not have access to this area.</p>;
  }

  if (state.error) {
    return <p className="error-text">{state.error}</p>;
  }

  if (state.empty) {
    return <p className="muted-text">{emptyMessage}</p>;
  }

  return children;
}
