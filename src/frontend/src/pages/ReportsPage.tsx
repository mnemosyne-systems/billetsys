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
import { toQueryString } from "../utils/formatting";
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
    return <div className="report-no-data">Unable to load diagrams.</div>;
  }
  if (!scriptReady) {
    return <div className="report-no-data">Loading diagrams...</div>;
  }
  if (normalizedItems.length === 0) {
    return <div className="report-no-data">No data available</div>;
  }
  return (
    <div className="report-chart-container">
      <canvas ref={canvasRef} />
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
    <section className="detail-card report-chart-card">
      <div className="report-title-row">
        <h3>{title}</h3>
        {children}
      </div>
      <ReportChartCanvas chartKey={chartKey} {...chartProps} />
    </section>
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
      <section className="panel">
        <h2>Reports</h2>
        <p className="muted-text">
          Reports are available for admin, TAM, and superuser roles.
        </p>
      </section>
    );
  }

  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <h2>Reports</h2>
        </div>
      </div>

      <DataState
        state={reportsState}
        emptyMessage="No report data available."
        signInHref={sessionState.data?.homePath || "/login"}
      >
        {reports && (
          <div className="report-layout">
            {reports.showCompanyFilter ? (
              <div className="filter-row">
                <label>
                  Company
                  <select
                    value={selectedCompanyId}
                    onChange={(event) =>
                      setFilters((current) => ({
                        ...current,
                        companyId: event.target.value,
                      }))
                    }
                  >
                    <option value="">All companies</option>
                    {(reports.companies || []).map((company: NamedEntity) => (
                      <option key={company.id} value={company.id}>
                        {company.name}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            ) : (
              <p className="report-summary">
                <strong>Company:</strong> {reports.companyName}
              </p>
            )}
            <p className="report-summary">
              Total tickets: <strong>{reports.totalTickets}</strong>
            </p>

            <div className="report-grid">
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
                <label className="report-inline-filter">
                  Period
                  <select
                    value={filters.period}
                    onChange={(event) =>
                      setFilters((current) => ({
                        ...current,
                        period: event.target.value,
                      }))
                    }
                  >
                    <option value="all">All</option>
                    <option value="year">Year</option>
                    <option value="month">Month</option>
                  </select>
                </label>
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
              <section className="detail-card report-chart-card">
                <div className="report-title-row">
                  <h3>Resolution Time</h3>
                </div>
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
                <table className="report-histogram-table">
                  <thead>
                    <tr>
                      <th>Duration</th>
                      <th>Count</th>
                      <th>Tickets</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(reports.histogram || []).map(
                      (bucket: ReportHistogramBucket) => (
                        <tr key={bucket.label}>
                          <td>{bucket.label}</td>
                          <td>{bucket.count}</td>
                          <td>
                            {bucket.tickets.length === 0
                              ? "—"
                              : bucket.tickets.map(
                                  (ticket: TicketListItem, index: number) => (
                                    <span key={ticket.id}>
                                      <a href={`/tickets/${ticket.id}`}>
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
              </section>
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

            <div className="button-row button-row-end">
              <button
                type="button"
                className="action-button export-btn"
                onClick={exportReport}
                disabled={!chartScriptState.loaded}
              >
                Export
              </button>
            </div>
          </div>
        )}
      </DataState>
    </section>
  );
}
