/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

export type AvailabilityScope = "PERSONAL" | "COMPANY";

export interface Availability {
  id: number;
  startDate: string;
  endDate: string;
  scope: AvailabilityScope;
  reason: string | null;
}

export interface AvailabilityRequest {
  startDate: string;
  endDate: string;
  reason: string | null;
}
