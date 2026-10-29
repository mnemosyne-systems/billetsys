/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useId, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { Role } from "../../types/app";
import { ticketReferencePath } from "../../utils/ticketReferences";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

/** Check a manually entered alias before navigating to an existing protected route. */
export default function TicketReferenceLookup({ role }: { role?: Role }) {
  const [value, setValue] = useState("");
  const [error, setError] = useState("");
  const errorId = useId();
  const navigate = useNavigate();
  if (import.meta.env.VITE_TICKET_REFERENCES !== "true") return null;
  if (!["support", "superuser", "tam", "user"].includes(role || ""))
    return null;

  return (
    <form
      aria-label="Find ticket by reference"
      className="max-w-sm"
      onSubmit={(event) => {
        event.preventDefault();
        const path = ticketReferencePath(value, role);
        if (!path) {
          setError("Invalid ticket reference. Check the code and try again.");
          return;
        }
        setError("");
        setValue("");
        navigate(path);
      }}
    >
      <div className="flex gap-2">
        <Input
          aria-label="Ticket reference"
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          autoComplete="off"
          spellCheck={false}
          maxLength={128}
          placeholder="Ticket reference"
          value={value}
          onChange={(event) => {
            setValue(event.target.value);
            setError("");
          }}
          className="w-44 font-mono"
        />
        <Button type="submit" variant="outline">
          Open
        </Button>
      </div>
      {error && (
        <p id={errorId} role="alert" className="text-sm">
          {error}
        </p>
      )}
    </form>
  );
}
