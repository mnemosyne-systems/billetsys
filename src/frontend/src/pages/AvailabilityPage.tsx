/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THE ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THIS SOFTWARE IS GOVERNED BY THE TERMS OF THE AGREEMENT.
 */

import PageHeader from "../components/layout/PageHeader";
import { AvailabilityCalendar } from "../availability/AvailabilityCalendar";
import type { SessionPageProps } from "../types/app";

export default function AvailabilityPage({ sessionState }: SessionPageProps) {
  const role = sessionState.data?.role;

  return (
    <div className="space-y-6">
      <PageHeader title="Availability" />
      <AvailabilityCalendar role={role} />
    </div>
  );
}
