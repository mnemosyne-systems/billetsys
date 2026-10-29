/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useId, useState } from "react";
import type { Id } from "../../types/app";
import { formatTicketReference } from "../../utils/ticketReferences";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

/** An opt-in display alias; existing ticket names, links and stored IDs stay primary. */
export default function TicketReference({ ticketId }: { ticketId: Id }) {
  const inputId = useId();
  const [message, setMessage] = useState("");
  if (import.meta.env.VITE_TICKET_REFERENCES !== "true") return null;
  const reference = formatTicketReference(ticketId);
  if (!reference) return null;

  async function copy(value: string) {
    try {
      await navigator.clipboard.writeText(value);
      setMessage("Ticket reference copied.");
    } catch {
      setMessage("Select the reference above and copy it manually.");
    }
  }

  return (
    <div className="max-w-md space-y-1">
      <label htmlFor={inputId} className="text-sm font-medium">
        Ticket reference
      </label>
      <div className="flex gap-2">
        <Input id={inputId} value={reference} readOnly className="font-mono" />
        <Button
          type="button"
          variant="outline"
          onClick={() => void copy(reference)}
        >
          Copy
        </Button>
      </div>
      <p role="status" className="text-sm text-muted-foreground">
        {message}
      </p>
    </div>
  );
}
