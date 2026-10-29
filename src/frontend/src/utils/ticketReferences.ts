/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { createReferenceCodec } from "refmint/codec";
import type { Id, Role } from "../types/app";

// This public, reversible alias is not an access token. Keep the namespace and
// v1 contract stable once an installation has shared a reference.
const tickets = createReferenceCodec({
  prefix: "btk",
  kind: "uint64",
  version: 1,
});
const MAX_TICKET_ID = (1n << 63n) - 1n;

/** Encode the existing positive Java Long ID without rounding a JSON number. */
export function formatTicketReference(id: Id): string | null {
  if (typeof id === "number" && (!Number.isSafeInteger(id) || id <= 0)) {
    return null;
  }
  const decimal = String(id);
  if (decimal.length > 19 || !/^[1-9][0-9]*$/.test(decimal)) {
    return null;
  }
  const value = BigInt(decimal);
  return value <= MAX_TICKET_ID ? tickets.format(tickets.encode(value)) : null;
}

/** Resolve only checked references, preserving the exact decimal ID in the URL. */
export function ticketReferencePath(
  input: unknown,
  role?: Role,
): string | null {
  const decoded = tickets.decode(input);
  if (!decoded.ok || decoded.value <= 0n || decoded.value > MAX_TICKET_ID) {
    return null;
  }
  const id = decoded.value.toString();
  // These are the existing authenticated routes. Their server-side role,
  // company and participant checks remain the authority for ticket access.
  switch (role) {
    case "support":
      return `/support/tickets/${id}`;
    case "superuser":
      return `/superuser/tickets/${id}`;
    case "tam":
    case "user":
      return `/user/tickets/${id}`;
    default:
      return null;
  }
}
