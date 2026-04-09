/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Spinner } from "../ui/spinner";

interface LoadingSpinnerProps {
  label?: string;
}

export default function LoadingSpinner({
  label = "Loading...",
}: LoadingSpinnerProps) {
  return (
    <div
      className="flex items-center gap-2 text-muted-foreground"
      role="status"
      aria-live="polite"
    >
      <Spinner className="size-4" />
      {label ? <span className="text-sm font-medium">{label}</span> : null}
    </div>
  );
}
