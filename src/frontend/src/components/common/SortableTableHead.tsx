/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { ReactNode } from "react";
import { TableHead } from "../ui/table";
import { cn } from "@/lib/utils";
import { ArrowDownIcon, ArrowUpIcon, ChevronsUpDownIcon } from "lucide-react";

interface SortableTableHeadProps {
  columnKey: string;
  currentSort: string | null;
  currentDir: "asc" | "desc";
  onSort: (column: string) => void;
  children: ReactNode;
  className?: string;
}

export default function SortableTableHead({
  columnKey,
  currentSort,
  currentDir,
  onSort,
  children,
  className,
}: SortableTableHeadProps) {
  const isActive = currentSort === columnKey;
  const Icon = isActive
    ? currentDir === "asc"
      ? ArrowUpIcon
      : ArrowDownIcon
    : ChevronsUpDownIcon;

  return (
    <TableHead
      className={cn("whitespace-nowrap", className)}
      aria-sort={
        isActive ? (currentDir === "asc" ? "ascending" : "descending") : "none"
      }
    >
      <button
        type="button"
        className="inline-flex items-center gap-1 rounded-sm text-left font-medium transition-colors hover:text-[var(--color-header-bg)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        onClick={() => onSort(columnKey)}
      >
        {children}
        <Icon
          aria-hidden="true"
          className={cn(
            "size-3.5 transition-opacity",
            isActive ? "opacity-100" : "opacity-40",
          )}
        />
      </button>
    </TableHead>
  );
}
