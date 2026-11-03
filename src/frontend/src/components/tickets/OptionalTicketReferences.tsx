/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { lazy, Suspense } from "react";
import type { Id, Role } from "../../types/app";

const Reference =
  import.meta.env.VITE_TICKET_REFERENCES === "true"
    ? lazy(() => import("./TicketReference"))
    : null;
const Lookup =
  import.meta.env.VITE_TICKET_REFERENCES === "true"
    ? lazy(() => import("./TicketReferenceLookup"))
    : null;

/** Load the optional reference display only when the build enables it. */
export function TicketReference({ ticketId }: { ticketId: Id }) {
  return Reference ? (
    <Suspense fallback={null}>
      <Reference ticketId={ticketId} />
    </Suspense>
  ) : null;
}

/** Load the optional lookup without adding its codec to disabled builds. */
export function TicketReferenceLookup({ role }: { role?: Role }) {
  return Lookup ? (
    <Suspense fallback={null}>
      <Lookup role={role} />
    </Suspense>
  ) : null;
}
