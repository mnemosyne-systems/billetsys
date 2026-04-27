/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { FormEvent, useState } from "react";
import { toast } from "sonner";
import { FileSpreadsheet } from "lucide-react";
import { useNavigate } from "react-router-dom";
import DataState from "../components/common/DataState";
import PageHeader from "../components/layout/PageHeader";
import { Badge } from "../components/ui/badge";
import { Button } from "../components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "../components/ui/card";
import { Input } from "../components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../components/ui/table";
import useJson from "../hooks/useJson";
import { postMultipart } from "../utils/api";
import { resolvePostRedirectPath, SmartLink } from "../utils/routing";
import { PATHS } from "../routes/paths";

interface TicketImportFormat {
  id: string;
  label: string;
  requiredColumns: string[];
  optionalColumns: string[];
}

interface TicketImportBootstrap {
  formats: TicketImportFormat[];
  submitPath: string;
}

interface TicketImportResult {
  rowNumber: number;
  sourceSystem: string;
  sourceKey: string;
  result: "created" | "skipped" | "failed";
  ticketId?: number | null;
  ticketName?: string | null;
  errorMessage?: string | null;
}

interface TicketImportSummary {
  batchId: number;
  sourceType: string;
  fileName?: string | null;
  status: string;
  rowCount: number;
  createdCount: number;
  skippedCount: number;
  failedCount: number;
  results: TicketImportResult[];
}

export default function TicketImportPage() {
  const navigate = useNavigate();
  const bootstrapState = useJson<TicketImportBootstrap>(
    "/api/ticket-imports/bootstrap",
  );
  const [file, setFile] = useState<File | null>(null);
  const [summary, setSummary] = useState<TicketImportSummary | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const csvFormat = bootstrapState.data?.formats?.find(
    (format) => format.id === "csv",
  );

  const submitImport = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!file || submitting) {
      return;
    }
    setSubmitting(true);
    try {
      const response = await postMultipart(
        bootstrapState.data?.submitPath || "/api/ticket-imports/csv",
        [["file", file]],
      );
      if (
        response.type === "opaqueredirect" ||
        (response.status >= 300 && response.status < 400)
      ) {
        navigate(
          await resolvePostRedirectPath(response, PATHS.supportTicketsOpen),
        );
        return;
      }
      const contentType = response.headers.get("content-type") || "";
      if (!contentType.includes("application/json")) {
        throw new Error("Import did not return a JSON summary.");
      }
      const nextSummary = (await response.json()) as TicketImportSummary;
      setSummary(nextSummary);
      toast.success("Ticket import completed.");
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Unable to import tickets.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="w-full mt-4">
      <PageHeader title="Import tickets" />

      <DataState state={bootstrapState} emptyMessage="Import is unavailable.">
        <form onSubmit={submitImport} className="space-y-6 pb-20">
          <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
            <Card>
              <CardHeader>
                <CardTitle>CSV upload</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-start gap-4 rounded-sm border bg-muted/20 p-4">
                  <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-sm bg-sky-50 text-sky-700 ring-1 ring-sky-100">
                    <FileSpreadsheet className="h-5 w-5" />
                  </span>
                  <div className="space-y-1 text-sm">
                    <p className="font-medium text-foreground">
                      Upload a CSV file to create tickets in billetsys.
                    </p>
                    <p className="leading-6 text-muted-foreground">
                      Each row creates one ticket and one initial message. Rows
                      already imported from the same source are skipped.
                    </p>
                  </div>
                </div>

                <div className="space-y-2">
                  <p className="text-sm font-medium text-foreground">File</p>
                  <Input
                    type="file"
                    accept=".csv,text/csv"
                    onChange={(event) =>
                      setFile(event.target.files?.item(0) || null)
                    }
                  />
                  <p className="text-sm text-muted-foreground">
                    {file ? file.name : "Choose a CSV file to begin."}
                  </p>
                </div>
              </CardContent>
            </Card>

            {csvFormat && (
              <Card>
                <CardHeader>
                  <CardTitle>CSV columns</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4 text-sm">
                  <div>
                    <p className="mb-2 font-medium">Required</p>
                    <div className="flex flex-wrap gap-2">
                      {csvFormat.requiredColumns.map((column) => (
                        <Badge key={column} variant="secondary">
                          {column}
                        </Badge>
                      ))}
                    </div>
                  </div>
                  <div>
                    <p className="mb-2 font-medium">Optional</p>
                    <div className="flex flex-wrap gap-2">
                      {csvFormat.optionalColumns.map((column) => (
                        <Badge key={column} variant="outline">
                          {column}
                        </Badge>
                      ))}
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}
          </div>

          <div className="flex justify-end">
            <Button type="submit" disabled={!file || submitting}>
              {submitting ? "Importing" : "Import"}
            </Button>
          </div>

          {summary && (
            <div className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-4">
                <Metric label="Rows" value={summary.rowCount} />
                <Metric label="Created" value={summary.createdCount} />
                <Metric label="Skipped" value={summary.skippedCount} />
                <Metric label="Failed" value={summary.failedCount} />
              </div>
              <div className="overflow-x-auto rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Row</TableHead>
                      <TableHead>Source</TableHead>
                      <TableHead>Result</TableHead>
                      <TableHead>Ticket</TableHead>
                      <TableHead>Message</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {summary.results.map((result) => (
                      <TableRow key={`${result.rowNumber}-${result.sourceKey}`}>
                        <TableCell>{result.rowNumber}</TableCell>
                        <TableCell>
                          {result.sourceSystem}:{result.sourceKey}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              result.result === "failed"
                                ? "destructive"
                                : "secondary"
                            }
                          >
                            {result.result}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {result.ticketId ? (
                            <SmartLink
                              href={`/support/tickets/${result.ticketId}`}
                            >
                              {result.ticketName || result.ticketId}
                            </SmartLink>
                          ) : (
                            "-"
                          )}
                        </TableCell>
                        <TableCell>{result.errorMessage || "-"}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </div>
          )}
        </form>
      </DataState>
    </section>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-md border p-4">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="text-2xl font-semibold">{value}</p>
    </div>
  );
}
