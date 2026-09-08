/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { Building2Icon, UserRoundIcon } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import type { AvailabilityScope } from "@/types/domain";

const SCOPE_LABELS: Record<AvailabilityScope, string> = {
  PERSONAL: "Personal",
  COMPANY: "Company",
};

function AvailabilityBadge({ scope }: { scope: AvailabilityScope }) {
  const isCompany = scope === "COMPANY";

  return (
    <Badge
      variant={isCompany ? "default" : "secondary"}
      className="gap-1 whitespace-nowrap font-normal"
      data-icon="inline-start"
    >
      {isCompany ? (
        <Building2Icon data-icon="inline-start" />
      ) : (
        <UserRoundIcon data-icon="inline-start" />
      )}
      <span>{SCOPE_LABELS[scope]}</span>
    </Badge>
  );
}

export { AvailabilityBadge, SCOPE_LABELS };
