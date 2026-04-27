/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

import type { Id } from "../types/app";
import type { UserReference, VersionInfo } from "../types/domain";

interface QueryValueMap {
  [key: string]: string | number | boolean | null | undefined;
}

interface AssignmentSummary {
  entitlementName?: string;
  levelName?: string;
}

export function formatFileSize(size: number | null | undefined): string {
  if (!size) {
    return "0 B";
  }
  const units = ["B", "KB", "MB", "GB"];
  let value = size;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value >= 10 || unitIndex === 0 ? Math.round(value) : value.toFixed(1)} ${units[unitIndex]}`;
}

export function formatPhone(
  phoneNumber?: string | null,
  extension?: string | null,
): string {
  if (!phoneNumber && !extension) {
    return "—";
  }
  if (!extension) {
    return phoneNumber || "—";
  }
  return `${phoneNumber || ""} ext. ${extension}`.trim();
}

export function durationLabel(value: string | number): string {
  return String(value) === "1" ? "Monthly" : "Yearly";
}

export function versionLabel(
  versions: VersionInfo[] | null | undefined,
  selectedId: Id | null | undefined,
): string {
  if (!selectedId) {
    return "";
  }
  const version = (versions || []).find(
    (option) => String(option.id) === String(selectedId),
  );
  return version ? `${version.name} (${version.date})` : "";
}

export function shouldUseLightTextOnColor(color?: string | null): boolean {
  const rgb = parseColorToRgb(color);
  if (!rgb) {
    return false;
  }
  const [red, green, blue] = rgb.map((value) => {
    const normalized = value / 255;
    return normalized <= 0.03928
      ? normalized / 12.92
      : Math.pow((normalized + 0.055) / 1.055, 2.4);
  });
  const luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
  return luminance < 0.45;
}

function parseColorToRgb(
  color?: string | null,
): [number, number, number] | null {
  if (!color) {
    return null;
  }
  const normalized = color.trim().toLowerCase();
  const named: Record<string, [number, number, number]> = {
    black: [0, 0, 0],
    blue: [0, 0, 255],
    green: [0, 128, 0],
    red: [255, 0, 0],
    white: [255, 255, 255],
    yellow: [255, 255, 0],
  };
  if (named[normalized]) {
    return named[normalized];
  }
  const hex = normalized.match(/^#([0-9a-f]{3}|[0-9a-f]{6})$/i);
  if (hex) {
    const value = hex[1];
    const full =
      value.length === 3
        ? value
            .split("")
            .map((char) => char + char)
            .join("")
        : value;
    return [
      Number.parseInt(full.slice(0, 2), 16),
      Number.parseInt(full.slice(2, 4), 16),
      Number.parseInt(full.slice(4, 6), 16),
    ];
  }
  const rgb = normalized.match(
    /^rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})(?:\s*,\s*[^)]+)?\s*\)$/,
  );
  if (!rgb) {
    return null;
  }
  return [
    clampRgb(Number.parseInt(rgb[1], 10)),
    clampRgb(Number.parseInt(rgb[2], 10)),
    clampRgb(Number.parseInt(rgb[3], 10)),
  ];
}

function clampRgb(value: number): number {
  if (Number.isNaN(value)) {
    return 0;
  }
  return Math.max(0, Math.min(255, value));
}

export function toQueryString(params: QueryValueMap): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

export function profileInitial(
  fullName?: string | null,
  username?: string | null,
  email?: string | null,
): string {
  const firstName = (fullName || "").trim().split(/\s+/).find(Boolean);
  const source = firstName || username || email || "?";
  return source.charAt(0).toUpperCase();
}

export function levelColorMarker(color?: string | null): string {
  switch ((color || "").toLowerCase()) {
    case "black":
      return "⬛";
    case "silver":
    case "white":
      return "⬜";
    case "gray":
      return "◻️";
    case "maroon":
    case "red":
      return "🟥";
    case "purple":
    case "fuchsia":
      return "🟪";
    case "green":
    case "lime":
      return "🟩";
    case "olive":
    case "yellow":
      return "🟨";
    case "navy":
    case "blue":
    case "teal":
    case "aqua":
      return "🟦";
    default:
      return "◻️";
  }
}

export function sortUsersByName(users: UserReference[]): UserReference[] {
  return [...users].sort((left, right) =>
    (left.displayName || left.username || "").localeCompare(
      right.displayName || right.username || "",
      undefined,
      {
        sensitivity: "base",
      },
    ),
  );
}

export function sortEntitlementAssignments(
  assignments: AssignmentSummary[],
): AssignmentSummary[] {
  return [...assignments].sort((left, right) => {
    const entitlementComparison = (left.entitlementName || "").localeCompare(
      right.entitlementName || "",
      undefined,
      {
        sensitivity: "base",
      },
    );
    if (entitlementComparison !== 0) {
      return entitlementComparison;
    }
    return (left.levelName || "").localeCompare(
      right.levelName || "",
      undefined,
      { sensitivity: "base" },
    );
  });
}
