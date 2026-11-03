/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useState } from "react";
import { toast } from "sonner";
import { useNavigate } from "react-router-dom";
import type { Role } from "../../types/app";
import { ticketReferencePath } from "../../utils/ticketReferences";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

/** Check a manually entered alias before navigating to an existing protected route. */
export default function TicketReferenceLookup({ role }: { role?: Role }) {
  const [value, setValue] = useState("");
  const [invalid, setInvalid] = useState(false);
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
          setInvalid(true);
          toast.error(
            "Invalid ticket reference. Check the code and try again.",
          );
          return;
        }
        setInvalid(false);
        setValue("");
        navigate(path);
      }}
    >
      <div className="flex gap-2">
        <Input
          aria-label="Ticket reference"
          aria-invalid={invalid}
          autoComplete="off"
          spellCheck={false}
          maxLength={128}
          placeholder="Ticket reference"
          value={value}
          onChange={(event) => {
            setValue(event.target.value);
            setInvalid(false);
          }}
          className="w-44 font-mono text-foreground"
        />
        <Button type="submit" variant="outline">
          Open
        </Button>
      </div>
    </form>
  );
}
