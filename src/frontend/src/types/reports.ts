import type { NamedEntity, TicketListItem } from './domain';

export type ReportChartType = 'pie' | 'bar' | 'line';

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
