/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { useMemo, useState } from "react";
import { eachDayOfInterval, format, formatISO, parseISO } from "date-fns";
import {
  CalendarDaysIcon,
  PencilIcon,
  PlusIcon,
  Trash2Icon,
} from "lucide-react";
import { toast } from "sonner";

import { AvailabilityBadge } from "@/availability/AvailabilityBadge";
import { AvailabilityDialog } from "@/availability/AvailabilityDialog";
import DataState from "@/components/common/DataState";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import useJson from "@/hooks/useJson";
import useSubmissionGuard from "@/hooks/useSubmissionGuard";
import { cn } from "@/lib/utils";
import type { Role } from "@/types/app";
import type { Availability, AvailabilityScope } from "@/types/domain";

interface AvailabilityCalendarProps {
  className?: string;
  role?: Role;
}

interface DialogState {
  open: boolean;
  entry: Availability | null;
  initialDate?: string;
}

function AvailabilityCalendar({ className, role }: AvailabilityCalendarProps) {
  const guard = useSubmissionGuard();

  const canManageCompany = role === "support" || role === "superuser";
  const [scope, setScope] = useState<AvailabilityScope>("PERSONAL");
  const activeScope = canManageCompany ? scope : "PERSONAL";
  const [refreshKey, setRefreshKey] = useState(0);
  const [dialogState, setDialogState] = useState<DialogState>({
    open: false,
    entry: null,
    initialDate: undefined,
  });

  const scopeUrl = activeScope === "COMPANY" ? "/company" : "/me";
  const availabilityState = useJson<Availability[]>(
    `/api/availability${scopeUrl}?refresh=${refreshKey}`,
  );

  const availabilityDays = useMemo(() => {
    const entries = availabilityState.data ?? [];
    return entries.flatMap((entry) =>
      eachDayOfInterval({
        start: parseISO(entry.startDate),
        end: parseISO(entry.endDate),
      }),
    );
  }, [availabilityState.data]);

  const refresh = () => {
    setRefreshKey((current) => current + 1);
  };

  const closeDialog = () => {
    setDialogState({ open: false, entry: null, initialDate: undefined });
  };

  const openCreate = (date?: Date) => {
    setDialogState({
      open: true,
      entry: null,
      initialDate: date
        ? formatISO(date, { representation: "date" })
        : undefined,
    });
  };

  const openEdit = (entry: Availability) => {
    setDialogState({ open: true, entry, initialDate: undefined });
  };

  const confirmDelete = async (entry: Availability) => {
    if (!guard.tryEnter()) {
      return;
    }

    try {
      const response = await fetch(`/api/availability/${entry.id}`, {
        method: "DELETE",
        credentials: "same-origin",
        cache: "no-store",
        headers: {
          "X-Billetsys-Client": "react",
        },
      });

      if (response.status === 401) {
        throw new Error("You need to sign in again.");
      }
      if (response.status === 403) {
        throw new Error("You do not have access to this action.");
      }
      if (!response.ok) {
        throw new Error(
          toErrorMessage(response.statusText, "Unable to delete availability."),
        );
      }

      toast.success("Availability deleted.");
      refresh();
    } catch (error) {
      toast.error(
        error instanceof Error
          ? error.message
          : "Unable to delete availability.",
      );
    } finally {
      guard.exit();
    }
  };

  const entries = availabilityState.data ?? [];

  return (
    <section className={cn("w-full", className)}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        {canManageCompany ? (
          <Tabs
            value={scope}
            onValueChange={(value) => setScope(value as AvailabilityScope)}
          >
            <TabsList>
              <TabsTrigger value="PERSONAL">Personal</TabsTrigger>
              <TabsTrigger value="COMPANY">Company</TabsTrigger>
            </TabsList>
          </Tabs>
        ) : null}
        <Button type="button" onClick={() => openCreate()}>
          <PlusIcon />
          <span>Add availability</span>
        </Button>
      </div>

      <DataState
        state={availabilityState}
        emptyMessage="No availability periods found."
      >
        {entries.length === 0 ? (
          <p className="mt-6 text-muted-foreground">
            No availability periods found.
          </p>
        ) : (
          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            <Card>
              <CardContent className="flex justify-center pt-2">
                <Calendar
                  className="w-full min-w-[300px]"
                  modifiers={{ availability: availabilityDays }}
                  modifiersClassNames={{
                    availability:
                      "bg-primary/15 font-medium text-foreground data-[selected=true]:bg-primary/20 data-[selected=true]:text-foreground",
                  }}
                  onDayClick={(day) => openCreate(day)}
                />
              </CardContent>
            </Card>

            <div className="mt-2 space-y-3">
              {entries.map((entry) => (
                <Card key={entry.id} size="sm">
                  <CardContent className="flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <CalendarDaysIcon className="size-4 shrink-0 text-muted-foreground" />
                        <span className="truncate font-medium">
                          {format(parseISO(entry.startDate), "PPP")}
                          {" \u2013 "}
                          {format(parseISO(entry.endDate), "PPP")}
                        </span>
                      </div>
                      {entry.reason ? (
                        <p className="mt-1 truncate text-sm text-muted-foreground">
                          {entry.reason}
                        </p>
                      ) : null}
                    </div>
                    <div className="flex shrink-0 items-center gap-1">
                      <AvailabilityBadge scope={entry.scope} />
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon-sm"
                        aria-label="Edit availability"
                        onClick={() => openEdit(entry)}
                      >
                        <PencilIcon />
                      </Button>
                      <AlertDialog>
                        <AlertDialogTrigger asChild>
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon-sm"
                            aria-label="Delete availability"
                          >
                            <Trash2Icon className="text-destructive" />
                          </Button>
                        </AlertDialogTrigger>
                        <AlertDialogContent>
                          <AlertDialogHeader>
                            <AlertDialogTitle>
                              Delete availability?
                            </AlertDialogTitle>
                            <AlertDialogDescription>
                              This action cannot be undone. This will
                              permanently delete this availability period.
                            </AlertDialogDescription>
                          </AlertDialogHeader>
                          <AlertDialogFooter>
                            <AlertDialogCancel>Cancel</AlertDialogCancel>
                            <AlertDialogAction
                              variant="destructive"
                              onClick={() => void confirmDelete(entry)}
                            >
                              Delete
                            </AlertDialogAction>
                          </AlertDialogFooter>
                        </AlertDialogContent>
                      </AlertDialog>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        )}
      </DataState>

      <AvailabilityDialog
        open={dialogState.open}
        scope={activeScope}
        entry={dialogState.entry}
        initialDate={dialogState.initialDate}
        canManageCompany={canManageCompany}
        onClose={closeDialog}
        onSaved={refresh}
      />
    </section>
  );
}

function toErrorMessage(text: string, fallback: string): string {
  const trimmed = text.trim();
  if (!trimmed) {
    return fallback;
  }
  if (trimmed.startsWith("<!doctype html") || trimmed.startsWith("<html")) {
    return fallback;
  }
  return trimmed;
}

export { AvailabilityCalendar };
