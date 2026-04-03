/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { NamedEntity, TicketListItem } from "./domain";

export type ReportChartType = "pie" | "bar" | "line";

export interface ReportChartPoint {
  label: string;
  value: number;
}

export interface ReportHistogramBucket {
  label: string;
  count: number;
  tickets: TicketListItem[];
}

export interface ReportData {
  exportPath?: string;
  selectedCompanyId?: string | number;
  showCompanyFilter?: boolean;
  showCompanyChart?: boolean;
  companyName?: string;
  totalTickets?: number;
  companies?: NamedEntity[];
  status?: ReportChartPoint[];
  category?: ReportChartPoint[];
  company?: ReportChartPoint[];
  timeline?: ReportChartPoint[];
  firstResponse?: ReportChartPoint[];
  resolutionTime?: ReportChartPoint[];
  histogram?: ReportHistogramBucket[];
}
