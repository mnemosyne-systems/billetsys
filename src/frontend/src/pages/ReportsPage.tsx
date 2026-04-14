/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactNode } from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import useJson from "../hooks/useJson";
import useExternalScript from "../hooks/useExternalScript";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import { toQueryString } from "../utils/formatting";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "../components/ui/card";
import { Button } from "../components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import type { NamedEntity, TicketListItem } from "../types/domain";
import type { SessionPageProps } from "../types/app";
import type {
  ReportChartPoint,
  ReportChartType,
  ReportData,
  ReportHistogramBucket,
} from "../types/reports";
import { SUPPORT_TICKET_STATUSES } from "../types/tickets";

const REPORT_STATUS_COLORS: Partial<
  Record<(typeof SUPPORT_TICKET_STATUSES)[number], string>
> = {
  Open: "#4285f4",
  Assigned: "#fbbc04",
  Closed: "#34a853",
  Resolved: "#34a853",
  "In Progress": "#ea4335",
};

const REPORT_DEFAULT_COLORS = [
  "#4285f4",
  "#ea4335",
  "#fbbc04",
  "#34a853",
  "#9334e6",
  "#ff6d01",
  "#46bdc6",
  "#7baaf7",
  "#f07b72",
  "#fdd663",
];

interface ChartInstance {
  destroy: () => void;
  toBase64Image: () => string;
}

interface ChartConstructor {
  new (
    canvas: HTMLCanvasElement,
    config: {
      type: ReportChartType;
      data: {
        labels: string[];
        datasets: Array<{
          label?: string;
          data: number[];
          backgroundColor: string[];
          borderColor?: string;
          borderWidth: number;
          fill?: boolean;
          tension?: number;
          pointRadius?: number;
        }>;
      };
      options: Record<string, unknown>;
    },
  ): ChartInstance;
}

interface ChartWindow extends Window {
  Chart?: ChartConstructor;
}

interface ReportChartCanvasProps {
  chartKey: string;
  type: ReportChartType;
  items?: ReportChartPoint[];
  scriptReady: boolean;
  scriptError: string;
  onChartReady?: (name: string, instance: ChartInstance | null) => void;
  colorMap?: Record<string, string>;
  integerScale?: boolean;
  fill?: boolean;
}

interface ReportChartCardProps extends ReportChartCanvasProps {
  title: string;
  children?: ReactNode;
}

function reportColorsForLabels(
  labels: string[],
  colorMap?: Record<string, string>,
) {
  return labels.map(
    (label, index) =>
      (colorMap && colorMap[label]) ||
      REPORT_DEFAULT_COLORS[index % REPORT_DEFAULT_COLORS.length],
  );
}

function ReportChartCanvas({
  chartKey,
  type,
  items,
  scriptReady,
  scriptError,
  onChartReady,
  colorMap,
  integerScale = false,
  fill = false,
}: ReportChartCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const chartRef = useRef<ChartInstance | null>(null);
  const normalizedItems = useMemo(
    () => (Array.isArray(items) ? items : []),
    [items],
  );
  const ChartCtor = (window as ChartWindow).Chart;

  useEffect(() => {
    if (
      !scriptReady ||
      !canvasRef.current ||
      normalizedItems.length === 0 ||
      !ChartCtor
    ) {
      if (chartRef.current) {
        chartRef.current.destroy();
        chartRef.current = null;
      }
      onChartReady?.(chartKey, null);
      return undefined;
    }

    if (chartRef.current) {
      chartRef.current.destroy();
    }

    const labels = normalizedItems.map((item) => item.label);
    const values = normalizedItems.map((item) => item.value);
    const options: Record<string, unknown> = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: type === "pie", position: "bottom" },
      },
    };
    if (type !== "pie") {
      options.scales = {
        y: {
          beginAtZero: true,
          ticks: integerScale ? { stepSize: 1 } : undefined,
        },
      };
    }

    chartRef.current = new ChartCtor(canvasRef.current, {
      type,
      data: {
        labels,
        datasets: [
          {
            label:
              type === "line"
                ? "Tickets created"
                : type === "pie"
                  ? undefined
                  : "Tickets",
            data: values,
            backgroundColor: reportColorsForLabels(labels, colorMap),
            borderColor: type === "line" ? "#b00020" : undefined,
            borderWidth: type === "line" ? 2 : 0,
            fill,
            tension: type === "line" ? 0.3 : undefined,
            pointRadius: type === "line" ? 4 : undefined,
          },
        ],
      },
      options,
    });
    onChartReady?.(chartKey, chartRef.current);

    return () => {
      if (chartRef.current) {
        chartRef.current.destroy();
        chartRef.current = null;
      }
      onChartReady?.(chartKey, null);
    };
  }, [
    ChartCtor,
    chartKey,
    colorMap,
    fill,
    integerScale,
    normalizedItems,
    onChartReady,
    scriptReady,
    type,
  ]);

  if (scriptError) {
    return (
      <div className="flex h-[300px] w-full items-center justify-center text-sm text-destructive">
        Unable to load diagrams.
      </div>
    );
  }
  if (!scriptReady) {
    return (
      <div className="flex h-[300px] w-full items-center justify-center text-sm text-muted-foreground animate-pulse">
        Loading diagrams...
      </div>
    );
  }
  if (normalizedItems.length === 0) {
    return (
      <div className="flex h-[300px] w-full items-center justify-center text-sm text-muted-foreground italic">
        No data available
      </div>
    );
  }
  return (
    <div className="relative w-full h-full min-h-[300px]">
      <canvas ref={canvasRef} className="absolute inset-0 h-full w-full" />
    </div>
  );
}

function ReportChartCard({
  chartKey,
  title,
  children,
  ...chartProps
}: ReportChartCardProps) {
  return (
    <Card className="flex flex-col overflow-hidden">
      <CardHeader className="flex flex-row items-center justify-between bg-muted/30 pb-4 border-b space-y-0">
        <CardTitle className="text-base font-medium">{title}</CardTitle>
        {children && (
          <div className="flex items-center space-x-2">{children}</div>
        )}
      </CardHeader>
      <CardContent className="flex-1 p-4 bg-background">
        <ReportChartCanvas chartKey={chartKey} {...chartProps} />
      </CardContent>
    </Card>
  );
}

export default function ReportsPage({ sessionState }: SessionPageProps) {
  const session = sessionState.data;
  const role = session?.role;
  const supportsReports = ["admin", "tam", "superuser"].includes(role || "");
  const [filters, setFilters] = useState({ companyId: "", period: "all" });
  const chartScriptState = useExternalScript(
    "/webjars/chart.js/4.5.1/dist/chart.umd.js",
  );
  const chartInstancesRef = useRef<Record<string, ChartInstance>>({});
  const reportUrl = supportsReports
    ? `/api/reports${toQueryString({ companyId: filters.companyId || undefined, period: filters.period || undefined })}`
    : null;
  const reportsState = useJson<ReportData>(reportUrl);
  const reports = reportsState.data;
  const selectedCompanyId =
    filters.companyId ||
    (reports?.selectedCompanyId ? String(reports.selectedCompanyId) : "");

  const onChartReady = (name: string, instance: ChartInstance | null) => {
    if (instance) {
      chartInstancesRef.current[name] = instance;
      return;
    }
    delete chartInstancesRef.current[name];
  };

  const exportReport = () => {
    if (!reports?.exportPath) {
      return;
    }
    const form = document.createElement("form");
    form.method = "POST";
    form.action = `${reports.exportPath}${toQueryString({ companyId: filters.companyId || undefined, period: filters.period || undefined })}`;

    const imageFields = {
      statusChart: chartInstancesRef.current.statusChart,
      categoryChart: chartInstancesRef.current.categoryChart,
      companyChart: chartInstancesRef.current.companyChart,
      timeChart: chartInstancesRef.current.timeChart,
      responseTimeChart: chartInstancesRef.current.responseTimeChart,
      resolutionTimeChart: chartInstancesRef.current.resolutionTimeChart,
      histogramChart: chartInstancesRef.current.histogramChart,
    };

    Object.entries(imageFields).forEach(([name, chart]) => {
      const input = document.createElement("input");
      input.type = "hidden";
      input.name = name;
      input.value = chart ? chart.toBase64Image() : "";
      form.appendChild(input);
    });

    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
  };

  if (!supportsReports) {
    return (
      <section className="w-full mt-4">
        <PageHeader
          title="Reports"
          subtitle="Reports are available for admin, TAM, and superuser roles."
        />
      </section>
    );
  }

  return (
    <section className="w-full mt-4">
      <PageHeader title="Reports" />

      <DataState state={reportsState} emptyMessage="No report data available.">
        {reports && (
          <div className="space-y-8 pb-20">
            <Card>
              <CardContent className="p-6">
                <div className="flex flex-col md:flex-row md:items-center justify-between space-y-4 md:space-y-0">
                  {reports.showCompanyFilter ? (
                    <div className="flex items-center space-x-3">
                      <span className="text-sm font-medium">Company</span>
                      <Select
                        value={selectedCompanyId || "all"}
                        onValueChange={(value) =>
                          setFilters((current) => ({
                            ...current,
                            companyId: value === "all" ? "" : value,
                          }))
                        }
                      >
                        <SelectTrigger className="w-fit min-w-[200px]">
                          <SelectValue placeholder="All companies" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="all">All companies</SelectItem>
                          {(reports.companies || []).map(
                            (company: NamedEntity) => (
                              <SelectItem
                                key={company.id}
                                value={String(company.id)}
                              >
                                {company.name}
                              </SelectItem>
                            ),
                          )}
                        </SelectContent>
                      </Select>
                    </div>
                  ) : (
                    <p className="text-sm">
                      <strong className="font-medium">Company:</strong>{" "}
                      {reports.companyName}
                    </p>
                  )}
                  <p className="text-sm">
                    Total tickets:{" "}
                    <strong className="font-medium text-lg">
                      {reports.totalTickets}
                    </strong>
                  </p>
                </div>
              </CardContent>
            </Card>

            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-2">
              <ReportChartCard
                chartKey="statusChart"
                title="Tickets by Status"
                type="pie"
                items={reports.status}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                colorMap={REPORT_STATUS_COLORS}
                onChartReady={onChartReady}
              />
              <ReportChartCard
                chartKey="categoryChart"
                title="Tickets by Category"
                type="bar"
                items={reports.category}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                integerScale
                onChartReady={onChartReady}
              />
              {reports.showCompanyChart && (
                <ReportChartCard
                  chartKey="companyChart"
                  title="Tickets by Company"
                  type="bar"
                  items={reports.company}
                  scriptReady={chartScriptState.loaded}
                  scriptError={chartScriptState.error}
                  integerScale
                  onChartReady={onChartReady}
                />
              )}
              <ReportChartCard
                chartKey="timeChart"
                title="Ticket Volume Over Time"
                type="line"
                items={reports.timeline}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                integerScale
                fill
                onChartReady={onChartReady}
              >
                <div className="flex items-center space-x-2 text-sm font-normal">
                  <span className="text-muted-foreground">Period</span>
                  <Select
                    value={filters.period || "all"}
                    onValueChange={(value) =>
                      setFilters((current) => ({
                        ...current,
                        period: value,
                      }))
                    }
                  >
                    <SelectTrigger className="h-8 min-w-[100px]">
                      <SelectValue placeholder="Period" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All</SelectItem>
                      <SelectItem value="year">Year</SelectItem>
                      <SelectItem value="month">Month</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </ReportChartCard>
              <ReportChartCard
                chartKey="responseTimeChart"
                title="Avg. First Response Time (hours)"
                type="bar"
                items={reports.firstResponse}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                onChartReady={onChartReady}
              />
              <ReportChartCard
                chartKey="resolutionTimeChart"
                title="Avg. Resolution Time (hours)"
                type="bar"
                items={reports.resolutionTime}
                scriptReady={chartScriptState.loaded}
                scriptError={chartScriptState.error}
                onChartReady={onChartReady}
              />
            </div>

            <Card className="overflow-hidden">
              <CardHeader className="bg-muted/30 pb-4 border-b">
                <CardTitle className="text-base font-medium">
                  Resolution Time
                </CardTitle>
              </CardHeader>
              <CardContent className="p-4 grid gap-8 lg:grid-cols-2">
                <div>
                  <ReportChartCanvas
                    chartKey="histogramChart"
                    type="bar"
                    items={(reports.histogram || []).map(
                      (bucket: ReportHistogramBucket) => ({
                        label: bucket.label,
                        value: bucket.count,
                      }),
                    )}
                    scriptReady={chartScriptState.loaded}
                    scriptError={chartScriptState.error}
                    integerScale
                    onChartReady={onChartReady}
                  />
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm text-left border-collapse">
                    <thead className="bg-muted/50 text-muted-foreground uppercase text-xs">
                      <tr>
                        <th className="px-4 py-3 font-medium border-b">
                          Duration
                        </th>
                        <th className="px-4 py-3 font-medium border-b text-right">
                          Count
                        </th>
                        <th className="px-4 py-3 font-medium border-b">
                          Tickets
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {(reports.histogram || []).map(
                        (bucket: ReportHistogramBucket) => (
                          <tr
                            key={bucket.label}
                            className="border-b last:border-0 hover:bg-muted/20"
                          >
                            <td className="px-4 py-3 whitespace-nowrap">
                              {bucket.label}
                            </td>
                            <td className="px-4 py-3 text-right">
                              {bucket.count}
                            </td>
                            <td className="px-4 py-3">
                              {bucket.tickets.length === 0
                                ? "—"
                                : bucket.tickets.map(
                                    (ticket: TicketListItem, index: number) => (
                                      <span key={ticket.id}>
                                        <a
                                          className="text-primary hover:underline hover:text-primary/80"
                                          href={`/tickets/${ticket.id}`}
                                        >
                                          {ticket.name}
                                        </a>
                                        {index < bucket.tickets.length - 1
                                          ? ", "
                                          : ""}
                                      </span>
                                    ),
                                  )}
                            </td>
                          </tr>
                        ),
                      )}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>

            <div className="flex items-center justify-end space-x-3 pt-4 border-t">
              <Button
                variant="outline"
                onClick={exportReport}
                disabled={!chartScriptState.loaded}
              >
                Export PDF
              </Button>
            </div>
          </div>
        )}
      </DataState>
    </section>
  );
}
