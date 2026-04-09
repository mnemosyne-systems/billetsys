/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Badge } from "../ui/badge";

interface LevelColorProps {
  color?: string | null;
  display?: string | null;
}

export function LevelColorBadge({ color, display }: LevelColorProps) {
  return (
    <Badge
      variant="outline"
      className="flex items-center gap-1.5 w-fit whitespace-nowrap bg-card"
    >
      <span
        className="w-2.5 h-2.5 rounded-full border border-black/10 inline-block shrink-0"
        style={{ backgroundColor: color || "transparent" }}
        aria-hidden="true"
      />
      <span>{display || "No color"}</span>
    </Badge>
  );
}

export function LevelColorFieldValue({ color, display }: LevelColorProps) {
  return (
    <div className="flex items-center gap-2">
      <span
        className="w-2.5 h-2.5 rounded-full border border-black/10 inline-block shrink-0"
        style={{ backgroundColor: color || "transparent" }}
        aria-hidden="true"
      />
      <span>{display || "—"}</span>
    </div>
  );
}
