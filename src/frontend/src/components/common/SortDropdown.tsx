/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import { ArrowDownIcon, ArrowUpIcon } from "lucide-react";
import { Button } from "../ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";

export interface SortOption {
  key: string;
  label: string;
}

interface SortDropdownProps {
  options: SortOption[];
  currentSort: string | null;
  currentDir: "asc" | "desc";
  onSort: (column: string) => void;
}

export default function SortDropdown({
  options,
  currentSort,
  currentDir,
  onSort,
}: SortDropdownProps) {
  if (options.length === 0) {
    return null;
  }

  const currentOption = options.find((option) => option.key === currentSort);
  const DirectionIcon = currentDir === "asc" ? ArrowUpIcon : ArrowDownIcon;
  const toggleSortKey = currentOption?.key || options[0].key;

  return (
    <div className="flex items-center gap-2">
      <span className="text-sm text-muted-foreground whitespace-nowrap">
        Sort by:
      </span>
      <Select
        value={currentSort || undefined}
        onValueChange={(value) => onSort(value)}
      >
        <SelectTrigger className="h-8 w-[160px]" id="sort-dropdown">
          <SelectValue placeholder="Sort by...">
            {currentOption?.label || "Sort by..."}
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          {options.map((option) => (
            <SelectItem key={option.key} value={option.key}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Button
        type="button"
        variant="outline"
        size="icon"
        className="h-8 w-8"
        onClick={() => onSort(toggleSortKey)}
        aria-label={`Sort ${currentDir === "asc" ? "descending" : "ascending"}`}
      >
        <DirectionIcon className="size-4" aria-hidden="true" />
      </Button>
    </div>
  );
}
